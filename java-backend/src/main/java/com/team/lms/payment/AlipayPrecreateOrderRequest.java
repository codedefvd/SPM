package com.team.lms.payment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AlipayPrecreateOrderRequest {
    private String outTradeNo;
    private BigDecimal amount;
    private String subject;
    private String notifyUrl;
    private String timeoutExpress;
}
