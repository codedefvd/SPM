package com.team.lms.reader.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ReaderFinePaymentOrderVo {
    private Long fineId;
    private String outTradeNo;
    private BigDecimal amount;
    private String payUrl;
    private String payForm;
    private String qrCode;
}
