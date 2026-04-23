package com.sideproject.linebot.service;

import com.sideproject.linebot.config.AppRuntimeProperties;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LineSignatureServiceTest {

    @Test
    void shouldReturnTrueForValidSignature() throws Exception {
        AppRuntimeProperties props = new AppRuntimeProperties();
        props.getLine().setChannelSecret("test-secret");
        LineSignatureService service = new LineSignatureService(props);

        String payload = "{\"events\":[]}";
        String signature = createSignature("test-secret", payload);

        assertTrue(service.isValidSignature(payload, signature));
    }

    @Test
    void shouldReturnFalseForInvalidSignature() {
        AppRuntimeProperties props = new AppRuntimeProperties();
        props.getLine().setChannelSecret("test-secret");
        LineSignatureService service = new LineSignatureService(props);

        String payload = "{\"events\":[]}";
        assertFalse(service.isValidSignature(payload, "invalid-signature"));
    }

    @Test
    void shouldBypassValidationWhenDisabled() {
        AppRuntimeProperties props = new AppRuntimeProperties();
        props.getLine().setSignatureValidationEnabled(false);
        LineSignatureService service = new LineSignatureService(props);

        String payload = "{\"events\":[]}";
        assertTrue(service.isValidSignature(payload, "invalid-signature"));
    }

    private String createSignature(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(digest);
    }
}
