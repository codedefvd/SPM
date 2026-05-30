package com.team.lms.payment;

import com.team.lms.common.enums.FineStatus;
import com.team.lms.entity.Fine;
import com.team.lms.mapper.FineMapper;
import com.team.lms.reader.vo.ReaderFinePaymentOrderVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlipaySandboxPaymentServiceTest {

    @Mock
    private FineMapper fineMapper;

    @Mock
    private AlipaySandboxGateway alipaySandboxGateway;

    private AlipaySignatureSupport signatureSupport;
    private AlipaySandboxProperties properties;
    private AlipaySandboxPaymentService service;

    @BeforeEach
    void setUp() throws Exception {
        KeyPair keyPair = generateKeyPair();
        signatureSupport = new AlipaySignatureSupport();
        properties = new AlipaySandboxProperties();
        properties.setAppId("9021000164624967");
        properties.setGatewayUrl("https://openapi-sandbox.dl.alipaydev.com/gateway.do");
        properties.setNotifyBaseUrl("https://pay-callback.example.test");
        properties.setReturnUrl("http://127.0.0.1:5173/reader/fines");
        properties.setMerchantPrivateKey(encodeKey(keyPair.getPrivate().getEncoded()));
        properties.setAlipayPublicKey(encodeKey(keyPair.getPublic().getEncoded()));
        service = new AlipaySandboxPaymentService(properties, signatureSupport, fineMapper, alipaySandboxGateway);
    }

    @Test
    void createFinePrecreateBuildsSandboxQrOrder() {
        Fine fine = fine(123L, "12.00", FineStatus.UNPAID);
        when(alipaySandboxGateway.precreate(any(AlipayPrecreateOrderRequest.class)))
                .thenReturn(new AlipayPrecreateResult("https://qr.alipay.com/fake-fine-order"));

        ReaderFinePaymentOrderVo order = service.createFinePrecreate(fine);

        assertThat(order.getFineId()).isEqualTo(123L);
        assertThat(order.getOutTradeNo()).startsWith("FINE_123_");
        assertThat(order.getAmount()).isEqualByComparingTo("12.00");
        assertThat(order.getPayUrl()).isEqualTo("https://openapi-sandbox.dl.alipaydev.com/gateway.do");
        assertThat(order.getQrCode()).isEqualTo("https://qr.alipay.com/fake-fine-order");

        ArgumentCaptor<AlipayPrecreateOrderRequest> requestCaptor =
                ArgumentCaptor.forClass(AlipayPrecreateOrderRequest.class);
        verify(alipaySandboxGateway).precreate(requestCaptor.capture());
        AlipayPrecreateOrderRequest request = requestCaptor.getValue();
        assertThat(request.getOutTradeNo()).isEqualTo(order.getOutTradeNo());
        assertThat(request.getAmount()).isEqualByComparingTo("12.00");
        assertThat(request.getNotifyUrl()).isEqualTo("https://pay-callback.example.test/api/payments/alipay/notify");
        assertThat(request.getSubject()).isEqualTo("Library fine " + order.getOutTradeNo());
        assertThat(request.getTimeoutExpress()).isEqualTo("30m");
    }

    @Test
    void handleNotifyMarksMatchingUnpaidFineAsPaid() {
        Fine unpaidFine = fine(123L, "12.00", FineStatus.UNPAID);
        when(fineMapper.selectById(123L)).thenReturn(unpaidFine);
        Map<String, String> notifyParams = signedNotifyParams("FINE_123_20260529231000", "12.00", "TRADE_SUCCESS");

        boolean handled = service.handleNotify(notifyParams);

        assertThat(handled).isTrue();
        ArgumentCaptor<Fine> fineCaptor = ArgumentCaptor.forClass(Fine.class);
        verify(fineMapper).update(fineCaptor.capture());
        assertThat(fineCaptor.getValue().getStatus()).isEqualTo(FineStatus.PAID);
    }

    @Test
    void handleNotifyRejectsMismatchedAmount() {
        Fine unpaidFine = fine(123L, "12.00", FineStatus.UNPAID);
        when(fineMapper.selectById(123L)).thenReturn(unpaidFine);
        Map<String, String> notifyParams = signedNotifyParams("FINE_123_20260529231000", "11.00", "TRADE_SUCCESS");

        boolean handled = service.handleNotify(notifyParams);

        assertThat(handled).isFalse();
        verify(fineMapper, never()).update(unpaidFine);
    }

    private Map<String, String> signedNotifyParams(String outTradeNo, String totalAmount, String tradeStatus) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", properties.getAppId());
        params.put("out_trade_no", outTradeNo);
        params.put("trade_no", "2026052922001400000000000000");
        params.put("trade_status", tradeStatus);
        params.put("total_amount", totalAmount);
        params.put("seller_id", "2088721000000000");
        params.put("sign_type", "RSA2");
        params.put("sign", signatureSupport.signNotify(params, properties.getMerchantPrivateKey()));
        return params;
    }

    private Fine fine(Long id, String amount, FineStatus status) {
        Fine fine = new Fine();
        fine.setId(id);
        fine.setAmount(new BigDecimal(amount));
        fine.setStatus(status);
        return fine;
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String encodeKey(byte[] key) {
        return java.util.Base64.getEncoder().encodeToString(key);
    }

}
