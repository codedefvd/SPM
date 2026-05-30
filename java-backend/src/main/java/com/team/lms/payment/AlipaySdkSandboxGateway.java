package com.team.lms.payment;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.AlipayConstants;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePrecreateModel;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.team.lms.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlipaySdkSandboxGateway implements AlipaySandboxGateway {

    private final AlipaySandboxProperties properties;

    @Override
    public AlipayPrecreateResult precreate(AlipayPrecreateOrderRequest orderRequest) {
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
        request.setNotifyUrl(orderRequest.getNotifyUrl());
        request.setBizModel(buildModel(orderRequest));

        try {
            AlipayTradePrecreateResponse response = alipayClient().execute(request);
            if (!response.isSuccess() || !hasText(response.getQrCode())) {
                throw new BusinessException(500, buildFailureMessage(response));
            }
            return new AlipayPrecreateResult(response.getQrCode());
        } catch (AlipayApiException exception) {
            throw new BusinessException(500, "failed to create alipay sandbox QR order: " + exception.getMessage());
        }
    }

    private AlipayTradePrecreateModel buildModel(AlipayPrecreateOrderRequest orderRequest) {
        AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
        model.setOutTradeNo(orderRequest.getOutTradeNo());
        model.setTotalAmount(orderRequest.getAmount().toPlainString());
        model.setSubject(orderRequest.getSubject());
        model.setTimeoutExpress(orderRequest.getTimeoutExpress());
        model.setProductCode("FACE_TO_FACE_PAYMENT");
        return model;
    }

    private AlipayClient alipayClient() throws AlipayApiException {
        AlipayConfig config = new AlipayConfig();
        config.setServerUrl(properties.getGatewayUrl());
        config.setAppId(properties.getAppId());
        config.setPrivateKey(properties.getMerchantPrivateKey());
        config.setFormat(AlipayConstants.FORMAT_JSON);
        config.setCharset(AlipayConstants.CHARSET_UTF8);
        config.setAlipayPublicKey(properties.getAlipayPublicKey());
        config.setSignType(AlipayConstants.SIGN_TYPE_RSA2);
        config.setConnectTimeout(properties.getConnectTimeoutMs());
        config.setReadTimeout(properties.getReadTimeoutMs());
        return new DefaultAlipayClient(config);
    }

    private String buildFailureMessage(AlipayTradePrecreateResponse response) {
        String detail = response.getSubMsg();
        if (!hasText(detail)) {
            detail = response.getMsg();
        }
        if (!hasText(detail)) {
            detail = response.getSubCode();
        }
        if (!hasText(detail)) {
            detail = response.getCode();
        }
        if (!hasText(detail)) {
            detail = "unknown alipay sandbox error";
        }
        return "failed to create alipay sandbox QR order: " + detail;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
