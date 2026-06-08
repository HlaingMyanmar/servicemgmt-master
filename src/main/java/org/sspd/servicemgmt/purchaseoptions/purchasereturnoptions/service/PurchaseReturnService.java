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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        BigDecimal oldDue = purchase.getDueAmount() != null ? purchase.getDueAmount() : BigDecimal.ZERO;

        if (dto.getReturnDate() != null && purchase.getPurchaseDate() != null
                && dto.getReturnDate().isBefore(purchase.getPurchaseDate())) {
            throw new RuntimeException("Return date cannot be before purchase date");
        }

        if (dto.getReason() == null || dto.getReason().isBlank()) {
            throw new RuntimeException("Return reason is required");
        }

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

            int qty = dDto.getQty() != null ? dDto.getQty() : 0;
            if (qty <= 0) {
                throw new RuntimeException("Return qty must be greater than zero for product: " + product.getName());
            }
            if (dDto.getUnitPrice() == null || dDto.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Unit price is required for product: " + product.getName());
            }

            int purchasedQty = purchasedQty(purchase, product.getId());
            if (purchasedQty <= 0) {
                throw new RuntimeException("Product does not belong to selected purchase: " + product.getName());
            }
            int alreadyReturned = returnedQty(purchase.getId(), product.getId(), null);
            int returnableQty = purchasedQty - alreadyReturned;
            if (qty > returnableQty) {
                throw new RuntimeException("Return qty exceeds returnable qty for product: " + product.getName()
                        + ". Returnable qty: " + returnableQty);
            }

            List<String> serials = normalizeSerials(dDto.getSerialNumbers());
            Set<String> purchasedSerials = purchasedSerials(purchase, product.getId());
            boolean serialTracked = !purchasedSerials.isEmpty() || Boolean.TRUE.equals(product.getHasSerial());

            if (serialTracked) {
                if (serials.size() != qty) {
                    throw new RuntimeException("Serial count must match qty for product: " + product.getName());
                }
                for (String sn : serials) {
                    if (!purchasedSerials.contains(sn.toUpperCase())) {
                        throw new RuntimeException("Serial number '" + sn + "' was not purchased on this voucher");
                    }
                    if (isSerialAlreadyReturned(purchase.getId(), product.getId(), sn, null)) {
                        throw new RuntimeException("Serial number '" + sn + "' was already returned");
                    }
                    ProductSerial serial = productSerialRepository.findBySerialNumber(sn)
                            .orElseThrow(() -> new RuntimeException("Serial number '" + sn + "' not found in inventory"));
                    if (!serial.getProduct().getId().equals(product.getId())) {
                        throw new RuntimeException("Serial number '" + sn + "' does not belong to product: " + product.getName());
                    }
                    if (serial.getStatus() != SerialStatus.Available) {
                        throw new RuntimeException("Serial number '" + sn + "' is not available for return");
                    }
                    productSerialRepository.delete(serial);
                }
            } else {
                int current = product.getStockQty() != null ? product.getStockQty() : 0;
                if (current < qty) {
                    throw new RuntimeException("Available stock is not enough for return: " + product.getName()
                            + ". Available qty: " + current);
                }
                product.setStockQty(current - qty);
                productRepository.save(product);
                serials = List.of();
            }

            BigDecimal subtotal = dDto.getUnitPrice().multiply(BigDecimal.valueOf(qty));
            total = total.add(subtotal);

            PurchaseReturnDetail detail = PurchaseReturnDetail.builder()
                    .purchaseReturn(entity)
                    .product(product)
                    .qty(qty)
                    .unitPrice(dDto.getUnitPrice())
                    .subtotal(subtotal)
                    .serialNumber(joinSerials(serials))
                    .build();
            detailEntities.add(detail);
        }

        BigDecimal previousReturns = safe(purchase.getReturnAmount());
        BigDecimal purchaseTotal = safe(purchase.getTotalAmount());
        BigDecimal paidAmount = safe(purchase.getPaidAmount());
        BigDecimal netAfterThisReturn = purchaseTotal.subtract(previousReturns.add(total));
        if (netAfterThisReturn.compareTo(BigDecimal.ZERO) < 0) {
            netAfterThisReturn = BigDecimal.ZERO;
        }

        BigDecimal creditBeforeRefund = paidAmount.subtract(netAfterThisReturn);
        if (creditBeforeRefund.compareTo(BigDecimal.ZERO) < 0) {
            creditBeforeRefund = BigDecimal.ZERO;
        }

        BigDecimal refundAmount = dto.getRefundAmount() != null ? dto.getRefundAmount() : BigDecimal.ZERO;
        if (refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Refund amount cannot be negative");
        }
        if (refundAmount.compareTo(creditBeforeRefund) > 0) {
            throw new RuntimeException("Refund amount exceeds supplier credit. Max refund: " + creditBeforeRefund);
        }

        entity.setDetails(detailEntities);
        entity.setTotalReturnAmount(total);
        entity.setRefundAmount(refundAmount);

        PurchaseReturn savedEntity = purchaseReturnRepository.save(entity);

        for (PurchaseReturnDetail detail : detailEntities) {
            stockMovementService.recordMovement(StockMovement.builder()
                    .product(detail.getProduct())
                    .movementType(MovementType.OUT)
                    .qty(detail.getQty())
                    .referenceId(savedEntity.getId())
                    .referenceType("PurchaseReturn")
                    .build());
        }

        recalculatePurchaseFinancials(purchase);

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
            createReturnJournal(savedEntity, method, refundAmount, oldDue.min(total), staffId, supplier != null ? supplier.getName() : "");
        } else if (oldDue.compareTo(BigDecimal.ZERO) > 0) {
            Integer staffId = purchase.getStaff() != null ? purchase.getStaff().getId() : null;
            createReturnJournal(savedEntity, null, BigDecimal.ZERO, oldDue.min(total), staffId, supplier != null ? supplier.getName() : "");
        }

        if (supplier != null) {
            syncSupplierBalance(supplier);
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
        throw new RuntimeException("Confirmed purchase return cannot be edited. Create a reversal/void workflow instead.");
    }

    @PreAuthorize("hasAuthority('CAN_ACCESS_PURCHASE_RETURN_DELETE')")
    @Transactional
    public void delete(Integer id) {
        throw new RuntimeException("Confirmed purchase return cannot be deleted. Create a reversal/void workflow instead.");
    }

    private void syncSupplierBalance(Supplier supplier) {
        BigDecimal totalDue = purchaseRepository.sumDueAmountBySupplierId(supplier.getId());
        if (totalDue == null) totalDue = BigDecimal.ZERO;
        BigDecimal supplierCredit = purchaseRepository.sumSupplierCreditAmountBySupplierId(supplier.getId());
        if (supplierCredit == null) supplierCredit = BigDecimal.ZERO;
        BigDecimal opening = supplier.getOpeningBalance() != null ? supplier.getOpeningBalance() : BigDecimal.ZERO;
        supplier.setCurrentBalance(opening.add(totalDue).subtract(supplierCredit));
        supplierRepository.save(supplier);
    }

    private void recalculatePurchaseFinancials(Purchase purchase) {
        BigDecimal returnAmount = purchaseReturnRepository.findByPurchaseId(purchase.getId()).stream()
                .map(r -> safe(r.getTotalReturnAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundedAmount = purchaseReturnRepository.findByPurchaseId(purchase.getId()).stream()
                .map(r -> safe(r.getRefundAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = safe(purchase.getTotalAmount());
        BigDecimal paid = safe(purchase.getPaidAmount());
        BigDecimal net = total.subtract(returnAmount);
        if (net.compareTo(BigDecimal.ZERO) < 0) net = BigDecimal.ZERO;

        BigDecimal due = net.subtract(paid);
        if (due.compareTo(BigDecimal.ZERO) < 0) due = BigDecimal.ZERO;

        BigDecimal supplierCredit = paid.subtract(net).subtract(refundedAmount);
        if (supplierCredit.compareTo(BigDecimal.ZERO) < 0) supplierCredit = BigDecimal.ZERO;

        purchase.setReturnAmount(returnAmount);
        purchase.setRefundAmount(refundedAmount);
        purchase.setNetAmount(net);
        purchase.setDueAmount(due);
        purchase.setSupplierCreditAmount(supplierCredit);

        if (due.compareTo(BigDecimal.ZERO) <= 0) {
            purchase.setPaymentStatus(PaymentStatus.Paid);
        } else if (paid.compareTo(BigDecimal.ZERO) > 0 || returnAmount.compareTo(BigDecimal.ZERO) > 0) {
            purchase.setPaymentStatus(PaymentStatus.Partial);
        } else {
            purchase.setPaymentStatus(PaymentStatus.Pending);
        }
        purchaseRepository.save(purchase);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private int purchasedQty(Purchase purchase, Integer productId) {
        if (purchase.getDetails() == null) return 0;
        return purchase.getDetails().stream()
                .filter(d -> d.getProduct() != null && productId.equals(d.getProduct().getId()))
                .mapToInt(d -> d.getQty() != null ? d.getQty() : 0)
                .sum();
    }

    private int returnedQty(Integer purchaseId, Integer productId, Integer excludeReturnId) {
        return purchaseReturnRepository.findByPurchaseId(purchaseId).stream()
                .filter(r -> excludeReturnId == null || !excludeReturnId.equals(r.getId()))
                .flatMap(r -> r.getDetails().stream())
                .filter(d -> d.getProduct() != null && productId.equals(d.getProduct().getId()))
                .mapToInt(d -> d.getQty() != null ? d.getQty() : 0)
                .sum();
    }

    private Set<String> purchasedSerials(Purchase purchase, Integer productId) {
        Set<String> serials = new HashSet<>();
        if (purchase.getDetails() == null) return serials;
        purchase.getDetails().stream()
                .filter(d -> d.getProduct() != null && productId.equals(d.getProduct().getId()))
                .filter(d -> d.getWarrantyItems() != null)
                .flatMap(d -> d.getWarrantyItems().stream())
                .map(w -> w.getSerialNumber())
                .filter(sn -> sn != null && !sn.isBlank())
                .map(sn -> sn.trim().toUpperCase())
                .forEach(serials::add);
        return serials;
    }

    private boolean isSerialAlreadyReturned(Integer purchaseId, Integer productId, String serial, Integer excludeReturnId) {
        String normalized = serial == null ? "" : serial.trim().toUpperCase();
        return purchaseReturnRepository.findByPurchaseId(purchaseId).stream()
                .filter(r -> excludeReturnId == null || !excludeReturnId.equals(r.getId()))
                .flatMap(r -> r.getDetails().stream())
                .filter(d -> d.getProduct() != null && productId.equals(d.getProduct().getId()))
                .flatMap(d -> normalizeSerials(d.getSerialNumber() == null ? List.of() : List.of(d.getSerialNumber())).stream())
                .anyMatch(sn -> sn.equalsIgnoreCase(normalized));
    }

    private List<String> normalizeSerials(List<String> serials) {
        if (serials == null) return List.of();
        return serials.stream()
                .flatMap(sn -> sn == null ? java.util.stream.Stream.empty() : java.util.Arrays.stream(sn.split(",")))
                .map(String::trim)
                .filter(sn -> !sn.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .toList();
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

    private void createReturnJournal(PurchaseReturn pr, PaymentMethod method, BigDecimal refundAmount,
                                     BigDecimal payableReduction, Integer staffId, String supplierName) {
        JournalEntryDTO journalDTO = new JournalEntryDTO();
        journalDTO.setReferenceNo(pr.getReturnNo());
        journalDTO.setEntryDate(LocalDateTime.now());
        journalDTO.setDescription("Purchase Return from Supplier: " + supplierName);
        journalDTO.setStaffId(staffId);

        List<JournalDetailDTO> details = new ArrayList<>();

        BigDecimal totalCredit = BigDecimal.ZERO;

        if (payableReduction != null && payableReduction.compareTo(BigDecimal.ZERO) > 0) {
            JournalDetailDTO drPayable = new JournalDetailDTO();
            drPayable.setAccountId(accountResolver.payable().getId());
            drPayable.setDebit(payableReduction);
            drPayable.setCredit(BigDecimal.ZERO);
            details.add(drPayable);
            totalCredit = totalCredit.add(payableReduction);
        }

        if (refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            JournalDetailDTO drCashBank = new JournalDetailDTO();
            drCashBank.setAccountId(method.getAccount().getId());
            drCashBank.setDebit(refundAmount);
            drCashBank.setCredit(BigDecimal.ZERO);
            details.add(drCashBank);
            totalCredit = totalCredit.add(refundAmount);
        }

        // Credit Purchase Return (COA code INC-007)
        JournalDetailDTO crPurchaseReturn = new JournalDetailDTO();
        crPurchaseReturn.setAccountId(accountResolver.purchaseRtn().getId());
        crPurchaseReturn.setDebit(BigDecimal.ZERO);
        crPurchaseReturn.setCredit(totalCredit);
        details.add(crPurchaseReturn);

        if (totalCredit.compareTo(BigDecimal.ZERO) > 0) {
            journalDTO.setDetails(details);
            journalWriter.write(journalDTO);
        }
    }
}
