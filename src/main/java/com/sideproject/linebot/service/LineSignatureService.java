package com.sideproject.linebot.service;

import com.sideproject.linebot.config.AppRuntimeProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class LineSignatureService {

    private final AppRuntimeProperties properties;

    public LineSignatureService(AppRuntimeProperties properties) {
        this.properties = properties;
    }

    public boolean isValidSignature(String payload, String signatureHeader) {
        if (!properties.getLine().isSignatureValidationEnabled()) {
            return true;
        }

        String secret = properties.getLine().getChannelSecret();
        if (secret == null || secret.isBlank()) {
            return false;
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = Base64.getEncoder().encodeToString(digest);
            return expected.equals(signatureHeader);
        } catch (Exception ex) {
            return false;
        }
    }
}
