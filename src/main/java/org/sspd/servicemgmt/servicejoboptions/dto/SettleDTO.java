package org.sspd.servicemgmt.servicejoboptions.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SettleDTO {
    private BigDecimal finalCost;
    private BigDecimal discountAmount;
    private Boolean foc;
    private BigDecimal paidAmount;
    private LocalDate dueDate;
    private Integer paymentMethodId;
    private Integer paymentAccountId;
    private String transactionNo;
    private Integer staffId;
}
