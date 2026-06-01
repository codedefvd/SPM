package com.team.lms.payment;

import com.team.lms.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
public class AlipaySignatureSupport {

    public String sign(Map<String, String> params, String privateKeyText) {
        return sign(params, privateKeyText, false);
    }

    public String signNotify(Map<String, String> params, String privateKeyText) {
        return sign(params, privateKeyText, true);
    }

    private String sign(Map<String, String> params, String privateKeyText, boolean excludeSignType) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(parsePrivateKey(privateKeyText));
            signature.update(buildSignContent(params, excludeSignType).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception exception) {
            throw new BusinessException(500, "failed to sign alipay request");
        }
    }

    public boolean verify(Map<String, String> params, String publicKeyText) {
        return verify(params, publicKeyText, false);
    }

    public boolean verifyNotify(Map<String, String> params, String publicKeyText) {
        return verify(params, publicKeyText, true);
    }

    private boolean verify(Map<String, String> params, String publicKeyText, boolean excludeSignType) {
        String sign = params.get("sign");
        if (sign == null || sign.isBlank()) {
            return false;
        }

        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(parsePublicKey(publicKeyText));
            signature.update(buildSignContent(params, excludeSignType).getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception exception) {
            return false;
        }
    }

    private String buildSignContent(Map<String, String> params, boolean excludeSignType) {
        return new TreeMap<>(params).entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> !entry.getValue().isBlank())
                .filter(entry -> !"sign".equals(entry.getKey()))
                .filter(entry -> !excludeSignType || !"sign_type".equals(entry.getKey()))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    private PrivateKey parsePrivateKey(String privateKeyText) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(normalizeKey(privateKeyText));
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private PublicKey parsePublicKey(String publicKeyText) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(normalizeKey(publicKeyText));
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    private String normalizeKey(String keyText) {
        if (keyText == null) {
            return "";
        }
        return keyText
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    }
}
