package org.sspd.servicemgmt.saleoptions.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalePaymentDTO {
    private BigDecimal paidAmount;
    private Integer paymentMethodId;
    private Integer paymentAccountId; // Cash=5, Bank=6
    private String transactionNo;
    private Integer arAccountId; // override default AR
    private Integer staffId;
    private String note;
}
