package com.team.lms.payment;

import com.team.lms.common.enums.FineStatus;
import com.team.lms.entity.Fine;
import com.team.lms.exception.BusinessException;
import com.team.lms.mapper.FineMapper;
import com.team.lms.reader.vo.ReaderFinePaymentOrderVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlipaySandboxPaymentService {

    private static final DateTimeFormatter TRADE_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final AlipaySandboxProperties properties;
    private final AlipaySignatureSupport signatureSupport;
    private final FineMapper fineMapper;
    private final AlipaySandboxGateway alipaySandboxGateway;

    public ReaderFinePaymentOrderVo createFinePrecreate(Fine fine) {
        requireConfigured();
        if (fine == null || fine.getId() == null) {
            throw new BusinessException(404, "fine not found");
        }
        if (fine.getStatus() != FineStatus.UNPAID) {
            throw new BusinessException(400, "only unpaid fines can be paid");
        }

        BigDecimal amount = normalizeAmount(fine.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "fine amount must be greater than zero");
        }

        String outTradeNo = "FINE_" + fine.getId() + "_" + LocalDateTime.now().format(TRADE_NO_TIME);
        String subject = "Library fine " + outTradeNo;
        AlipayPrecreateResult result = alipaySandboxGateway.precreate(AlipayPrecreateOrderRequest.builder()
                .outTradeNo(outTradeNo)
                .amount(amount)
                .subject(subject)
                .notifyUrl(buildNotifyUrl())
                .timeoutExpress("30m")
                .build());

        return ReaderFinePaymentOrderVo.builder()
                .fineId(fine.getId())
                .outTradeNo(outTradeNo)
                .amount(amount)
                .payUrl(properties.getGatewayUrl())
                .qrCode(result.getQrCode())
                .build();
    }

    public boolean handleNotify(Map<String, String> params) {
        if (!hasText(properties.getAppId()) || !hasText(properties.getAlipayPublicKey())) {
            return false;
        }
        if (!signatureSupport.verifyNotify(params, properties.getAlipayPublicKey())) {
            return false;
        }
        if (!properties.getAppId().equals(params.get("app_id"))) {
            return false;
        }

        String tradeStatus = params.get("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            return true;
        }

        Long fineId = parseFineId(params.get("out_trade_no"));
        if (fineId == null) {
            return false;
        }

        Fine fine = fineMapper.selectById(fineId);
        if (fine == null) {
            return false;
        }
        if (normalizeAmount(fine.getAmount()).compareTo(normalizeAmount(params.get("total_amount"))) != 0) {
            return false;
        }
        if (fine.getStatus() == FineStatus.PAID) {
            return true;
        }
        if (fine.getStatus() != FineStatus.UNPAID) {
            return false;
        }

        fine.setStatus(FineStatus.PAID);
        fineMapper.update(fine);
        return true;
    }

    private void requireConfigured() {
        if (!hasText(properties.getAppId())
                || !hasText(properties.getMerchantPrivateKey())
                || !hasText(properties.getAlipayPublicKey())
                || !hasText(properties.getNotifyBaseUrl())) {
            throw new BusinessException(500, "alipay sandbox is not configured");
        }
    }

    private String buildNotifyUrl() {
        String baseUrl = properties.getNotifyBaseUrl().replaceAll("/+$", "");
        return baseUrl + "/api/payments/alipay/notify";
    }

    private Long parseFineId(String outTradeNo) {
        if (!hasText(outTradeNo) || !outTradeNo.startsWith("FINE_")) {
            return null;
        }
        String[] parts = outTradeNo.split("_");
        if (parts.length < 3) {
            return null;
        }
        try {
            return Long.parseLong(parts[1]);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private BigDecimal normalizeAmount(String amount) {
        if (!hasText(amount)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return normalizeAmount(new BigDecimal(amount));
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
