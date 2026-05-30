package com.team.lms.payment;

public interface AlipaySandboxGateway {
    AlipayPrecreateResult precreate(AlipayPrecreateOrderRequest orderRequest);
}
