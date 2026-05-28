package org.sspd.servicemgmt.purchaseoptions.purchasereturnoptions.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sspd.servicemgmt.accountingoptions.paymentmethodoptions.model.PaymentMethod;
import org.sspd.servicemgmt.accountingoptions.paymentmethodoptions.repository.PaymentMethodRepository;
import org.sspd.servicemgmt.accountingoptions.paymenttransactionoptions.model.PaymentTransaction;
import org.sspd.servicemgmt.accountingoptions.paymenttransactionoptions.model.ReferenceType;
import org.sspd.servicemgmt.accountingoptions.paymenttransactionoptions.repository.PaymentTransactionRepository;
import org.sspd.servicemgmt.exceptionhandler.ResourceNotFoundException;
import org.sspd.servicemgmt.journaloption.detail.dto.JournalDetailDTO;
import org.sspd.servicemgmt.journaloption.entry.dto.JournalEntryDTO;
import org.sspd.servicemgmt.journaloption.entry.service.JournalWriter;
import org.sspd.servicemgmt.accountingoptions.coaoptions.AccountResolver;
import org.sspd.servicemgmt.purchaseoptions.model.Purchase;
import org.sspd.servicemgmt.purchaseoptions.model.PaymentStatus;
import org.sspd.servicemgmt.purchaseoptions.purchasereturnoptions.dto.PurchaseReturnDTO;
import org.sspd.servicemgmt.purchaseoptions.purchasereturnoptions.mapper.PurchaseReturnMapper;
import org.sspd.servicemgmt.purchaseoptions.purchasereturnoptions.model.PurchaseReturn;
import org.sspd.servicemgmt.purchaseoptions.purchasereturnoptions.repository.PurchaseReturnRepository;
import org.sspd.servicemgmt.purchaseoptions.purchasereturndetails.dto.PurchaseReturnDetailDTO;
import org.sspd.servicemgmt.purchaseoptions.purchasereturndetails.model.PurchaseReturnDetail;
import org.sspd.servicemgmt.purchaseoptions.repository.PurchaseRepository;
import org.sspd.servicemgmt.stockoptions.productserialoptions.enums.SerialStatus;
import org.sspd.servicemgmt.stockoptions.productserialoptions.model.ProductSerial;
import org.sspd.servicemgmt.stockoptions.productserialoptions.repository.ProductSerialRepository;
import org.sspd.servicemgmt.stockoptions.stockmovementoptions.model.MovementType;
import org.sspd.servicemgmt.stockoptions.stockmovementoptions.model.StockMovement;
import org.sspd.servicemgmt.stockoptions.stockmovementoptions.service.StockMovementService;
import org.sspd.servicemgmt.stockoptions.productoptions.model.Product;
import org.sspd.servicemgmt.stockoptions.productoptions.repository.ProductRepository;
import org.sspd.servicemgmt.supplieroptions.model.Supplier;
import org.sspd.servicemgmt.supplieroptions.repository.SupplierRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.sspd.servicemgmt.api.PageResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseReturnService {

    private final PurchaseReturnRepository purchaseReturnRepository;
    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final JournalWriter journalWriter;
    private final AccountResolver accountResolver;
    private final PurchaseReturnMapper mapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final ProductSerialRepository productSerialRepository;

    private static final String PURCHASE_RETURN_TOPIC = "/topic/purchase-return";

    @PreAuthorize("hasAuthority('CAN_ACCESS_PURCHASE_RETURN_CREATE')")
    @Transactional
    public PurchaseReturnDTO save(PurchaseReturnDTO dto) {
        if (dto.getDetails() == null || dto.getDetails().isEmpty()) {
            throw new RuntimeException("Purchase return details are required");
        }

        if (dto.getPurchaseId() == null) {
            throw new RuntimeException("Purchase reference is required for purchase return");
        }

        Purchase purchase = purchaseRepository.findById(dto.getPurchaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found"));
        Supplier supplier = purchase.getSupplier();

        BigDecimal oldTotal = purchase.getTotalAmount() != null ? purchase.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal oldDue = purchase.getDueAmount() != null ? purchase.getDueAmount() : BigDecimal.ZERO;
        BigDecimal paidAmount = purchase.getPaidAmount() != null ? purchase.getPaidAmount() : BigDecimal.ZERO;

        PurchaseReturn entity = mapper.toEntity(dto);
        entity.setReturnNo(generateReturnNo());

        entity.setPurchase(purchase);

        if (entity.getReturnDate() == null) {
            entity.setReturnDate(LocalDateTime.now());
        }

        BigDecimal total = BigDecimal.ZERO;
        List<PurchaseReturnDetail> detailEntities = new ArrayList<>();

        for (PurchaseReturnDetailDTO dDto : dto.getDetails()) {
            Product product = productRepository.findById(dDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            if (dDto.getSerialNumbers() == null || dDto.getSerialNumbers().size() != dDto.getQty()) {
                throw new RuntimeException("Serial count must match qty for product: " + product.getName());
            }

            BigDecimal subtotal = dDto.getUnitPrice().multiply(BigDecimal.valueOf(dDto.getQty()));
            total = total.add(subtotal);

            PurchaseReturnDetail detail = PurchaseReturnDetail.builder()
                    .purchaseReturn(entity)
                    .product(product)
                    .qty(dDto.getQty())
                    .unitPrice(dDto.getUnitPrice())
                    .subtotal(subtotal)
                    .serialNumber(joinSerials(dDto.getSerialNumbers()))
                    .build();
            detailEntities.add(detail);

            // Remove serials from inventory (returning to supplier)
            for (String sn : dDto.getSerialNumbers()) {
                ProductSerial serial = productSerialRepository.findBySerialNumber(sn)
                        .orElseThrow(() -> new RuntimeException("Serial number '" + sn + "' not found"));
                if (!serial.getProduct().getId().equals(product.getId())) {
                    throw new RuntimeException("Serial number '" + sn + "' does not belong to product: " + product.getName());
                }
                if (serial.getStatus() != SerialStatus.Available) {
                    throw new RuntimeException("Serial number '" + sn + "' is not available for return");
                }
                productSerialRepository.delete(serial);
            }
        }

        entity.setDetails(detailEntities);
        entity.setTotalReturnAmount(total);

        PurchaseReturn savedEntity = purchaseReturnRepository.save(entity);

        // Reduce purchase outstanding balance by the returned amount.
        // Remaining formula: Remaining = TotalPurchase - Returns - Payments.
        BigDecimal newTotal = oldTotal.subtract(total);
        if (newTotal.compareTo(BigDecimal.ZERO) < 0) {
            newTotal = BigDecimal.ZERO;
        }

        // Use uncapped remaining for supplier balance so over-returns reduce payable as well.
        BigDecimal remaining = newTotal.subtract(paidAmount);
        BigDecimal cappedDue = remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;

        purchase.setTotalAmount(newTotal);
        purchase.setDueAmount(cappedDue);
        if (cappedDue.compareTo(BigDecimal.ZERO) <= 0) {
            purchase.setPaymentStatus(PaymentStatus.Paid);
        } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            purchase.setPaymentStatus(PaymentStatus.Partial);
        } else {
            purchase.setPaymentStatus(PaymentStatus.Pending);
        }
        purchaseRepository.save(purchase);

        // Stock decreases when returning to supplier
        for (PurchaseReturnDetail detail : detailEntities) {
            stockMovementService.recordMovement(StockMovement.builder()
                    .product(detail.getProduct())
                    .movementType(MovementType.OUT)
                    .qty(detail.getQty())
                    .referenceId(savedEntity.getId())
                    .referenceType("PurchaseReturn")
                    .build());
        }

        BigDecimal refundAmount = dto.getRefundAmount() != null ? dto.getRefundAmount() : total;

        if (supplier != null) {
            syncSupplierBalance(supplier);
        }

        // Record refund payment transaction and accounting journal
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (dto.getPaymentMethodId() == null) {
                throw new RuntimeException("Payment Method is required for refund amount");
            }
            PaymentMethod method = paymentMethodRepository.findById(dto.getPaymentMethodId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment Method not found"));

            PaymentTransaction paymentTx = new PaymentTransaction();
            paymentTx.setReferenceId(savedEntity.getId());
            paymentTx.setReferenceType(ReferenceType.Purchase_Return);
            paymentTx.setPaymentMethod(method);
            paymentTx.setAmount(refundAmount);
            paymentTx.setPaymentDate(LocalDateTime.now());

            String txnNo = (dto.getTransactionNo() == null || dto.getTransactionNo().isEmpty())
                    ? generateTransactionNo()
                    : dto.getTransactionNo();
            paymentTx.setTransactionNo(txnNo);

            paymentTransactionRepository.save(paymentTx);

            Integer staffId = purchase.getStaff() != null ? purchase.getStaff().getId() : null;
            createReturnJournal(savedEntity, method, refundAmount, staffId, supplier != null ? supplier.getName() : "");
        }

        messagingTemplate.convertAndSend(PURCHASE_RETURN_TOPIC, "PURCHASE_RETURN_CREATED");
        return mapper.toDto(savedEntity);
    }

    @PreAuthorize("hasAuthority('CAN_ACCESS_PURCHASE_RETURN_READ')")
    @Transactional(readOnly = true)
    public PageResponse<PurchaseReturnDTO> findAll(String search, int page, int size) {
        return PageResponse.of(
                purchaseReturnRepository.findBySearch(search, PageRequest.of(page, size, Sort.by("id").descending()))
                        .map(mapper::toDto)
        );
    }

    @PreAuthorize("hasAuthority('CAN_ACCESS_PURCHASE_RETURN_READ')")
    @Transactional(readOnly = true)
    public List<PurchaseReturnDTO> findByPurchaseId(Integer purchaseId) {
        return purchaseReturnRepository.findByPurchaseId(purchaseId).stream()
                .map(mapper::toDto).toList();
    }

    @PreAuthorize("hasAuthority('CAN_ACCESS_PURCHASE_RETURN_READ')")
    @Transactional(readOnly = true)
    public PurchaseReturnDTO findById(Integer id) {
        PurchaseReturn entity = purchaseReturnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase return not found with id: " + id));
        return mapper.toDto(entity);
    }



    @PreAuthorize("hasAuthority('CAN_ACCESS_PURCHASE_RETURN_UPDATE')")
    @Transactional
    public PurchaseReturnDTO update(Integer id, PurchaseReturnDTO dto) {
        PurchaseReturn existing = purchaseReturnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase return not found with id: " + id));

        if (dto.getPurchaseId() != null) {
            Purchase purchase = purchaseRepository.findById(dto.getPurchaseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Purchase not found"));
            existing.setPurchase(purchase);
        } else {
            existing.setPurchase(null);
        }

        if (dto.getReturnDate() != null) {
            existing.setReturnDate(dto.getReturnDate());
        }
        existing.setReason(dto.getReason());

        if (dto.getDetails() != null) {
            existing.getDetails().clear();

            BigDecimal total = BigDecimal.ZERO;
            List<PurchaseReturnDetail> detailEntities = new ArrayList<>();

            for (PurchaseReturnDetailDTO dDto : dto.getDetails()) {
                Product product = productRepository.findById(dDto.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

                if (dDto.getSerialNumbers() == null || dDto.getSerialNumbers().size() != dDto.getQty()) {
                    throw new RuntimeException("Serial count must match qty for product: " + product.getName());
                }

                BigDecimal subtotal = dDto.getUnitPrice().multiply(BigDecimal.valueOf(dDto.getQty()));
                total = total.add(subtotal);

                PurchaseReturnDetail detail = PurchaseReturnDetail.builder()
                        .purchaseReturn(existing)
                        .product(product)
                        .qty(dDto.getQty())
                        .unitPrice(dDto.getUnitPrice())
                        .subtotal(subtotal)
                        .serialNumber(joinSerials(dDto.getSerialNumbers()))
                        .build();
                detailEntities.add(detail);
            }

            existing.getDetails().addAll(detailEntities);
            existing.setTotalReturnAmount(total);
        }

        PurchaseReturn savedEntity = purchaseReturnRepository.save(existing);
        messagingTemplate.convertAndSend(PURCHASE_RETURN_TOPIC, "PURCHASE_RETURN_UPDATED");
        return mapper.toDto(savedEntity);
    }

    @PreAuthorize("hasAuthority('CAN_ACCESS_PURCHASE_RETURN_DELETE')")
    @Transactional
    public void delete(Integer id) {
        PurchaseReturn existing = purchaseReturnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase return not found with id: " + id));
        purchaseReturnRepository.delete(existing);
        messagingTemplate.convertAndSend(PURCHASE_RETURN_TOPIC, "PURCHASE_RETURN_DELETED");
    }

    private void syncSupplierBalance(Supplier supplier) {
        BigDecimal totalDue = purchaseRepository.sumDueAmountBySupplierId(supplier.getId());
        if (totalDue == null) totalDue = BigDecimal.ZERO;
        BigDecimal opening = supplier.getOpeningBalance() != null ? supplier.getOpeningBalance() : BigDecimal.ZERO;
        supplier.setCurrentBalance(opening.add(totalDue));
        supplierRepository.save(supplier);
    }

    private String generateReturnNo() {
        Integer lastId = purchaseReturnRepository.findTopByOrderByIdDesc().map(PurchaseReturn::getId).orElse(0);
        return String.format("PRN-%05d", lastId + 1);
    }

    private String generateTransactionNo() {
        Long count = paymentTransactionRepository.count();
        return String.format("TXN-%06d", count + 1);
    }

    private String joinSerials(List<String> serials) {
        return serials == null ? null : String.join(",", serials);
    }

    private void createReturnJournal(PurchaseReturn pr, PaymentMethod method, BigDecimal amount,
                                     Integer staffId, String supplierName) {
        JournalEntryDTO journalDTO = new JournalEntryDTO();
        journalDTO.setReferenceNo(pr.getReturnNo());
        journalDTO.setEntryDate(LocalDateTime.now());
        journalDTO.setDescription("Purchase Return from Supplier: " + supplierName);
        journalDTO.setStaffId(staffId);

        List<JournalDetailDTO> details = new ArrayList<>();

        // Debit Cash/Bank (expected COA 5 or 6 based on payment method)
        JournalDetailDTO drCashBank = new JournalDetailDTO();
        drCashBank.setAccountId(method.getAccount().getId());
        drCashBank.setDebit(amount);
        drCashBank.setCredit(BigDecimal.ZERO);
        details.add(drCashBank);

        // Credit Purchase Return (COA code INC-007)
        JournalDetailDTO crPurchaseReturn = new JournalDetailDTO();
        crPurchaseReturn.setAccountId(accountResolver.purchaseRtn().getId());
        crPurchaseReturn.setDebit(BigDecimal.ZERO);
        crPurchaseReturn.setCredit(amount);
        details.add(crPurchaseReturn);

        journalDTO.setDetails(details);
        journalWriter.write(journalDTO);
    }
}
