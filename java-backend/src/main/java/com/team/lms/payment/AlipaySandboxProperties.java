package com.team.lms.payment;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "alipay.sandbox")
public class AlipaySandboxProperties {
    private String appId;
    private String merchantPrivateKey;
    private String alipayPublicKey;
    private String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private String notifyBaseUrl;
    private String returnUrl = "http://127.0.0.1:5173/reader/fines";
    private int connectTimeoutMs = 10000;
    private int readTimeoutMs = 60000;
}
