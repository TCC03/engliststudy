package com.sideproject.linebot.service;

import com.sideproject.linebot.config.AppRuntimeProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Service
public class LineMessagingService {

    private static final Logger log = LoggerFactory.getLogger(LineMessagingService.class);

    private static final String REPLY_API_URL = "https://api.line.me/v2/bot/message/reply";
    private static final String PUSH_API_URL = "https://api.line.me/v2/bot/message/push";

    private final AppRuntimeProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public LineMessagingService(AppRuntimeProperties properties) {
        this.properties = properties;
    }

    public boolean replyText(String replyToken, String messageText) {
        String token = properties.getLine().getChannelAccessToken();
        if (token == null || token.isBlank() || replyToken == null || replyToken.isBlank()) {
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "replyToken", replyToken,
                "messages", List.of(
                        Map.of("type", "text", "text", messageText)
                )
        );

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(REPLY_API_URL, request, String.class);
            return true;
        } catch (Exception ex) {
            log.warn("LINE replyText failed: {}", ex.getMessage());
            return false;
        }
    }

    public boolean replyFlex(String replyToken, String altText, Map<String, Object> flexContents) {
        String token = properties.getLine().getChannelAccessToken();
        if (token == null || token.isBlank() || replyToken == null || replyToken.isBlank()) {
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> message = Map.of(
                "type", "flex",
                "altText", altText,
                "contents", flexContents
        );

        Map<String, Object> body = Map.of(
                "replyToken", replyToken,
                "messages", List.of(message)
        );

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(REPLY_API_URL, request, String.class);
            return true;
        } catch (Exception ex) {
            log.warn("LINE replyFlex failed: {}", ex.getMessage());
            return false;
        }
    }

    public boolean replyQuickReply(String replyToken, String messageText, List<Map<String, Object>> quickReplyItems) {
        String token = properties.getLine().getChannelAccessToken();
        if (token == null || token.isBlank() || replyToken == null || replyToken.isBlank()) {
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> message = Map.of(
                "type", "text",
                "text", messageText,
                "quickReply", Map.of("items", quickReplyItems)
        );

        Map<String, Object> body = Map.of(
                "replyToken", replyToken,
                "messages", List.of(message)
        );

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(REPLY_API_URL, request, String.class);
            return true;
        } catch (Exception ex) {
            log.warn("LINE replyQuickReply failed: {}", ex.getMessage());
            return false;
        }
    }

    public boolean replyAudio(String replyToken, String audioUrl, int durationMillis) {
        String token = properties.getLine().getChannelAccessToken();
        if (token == null || token.isBlank() || replyToken == null || replyToken.isBlank()
                || audioUrl == null || audioUrl.isBlank()) {
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> message = Map.of(
                "type", "audio",
                "originalContentUrl", audioUrl,
                "duration", Math.max(durationMillis, 1000)
        );

        Map<String, Object> body = Map.of(
                "replyToken", replyToken,
                "messages", List.of(message)
        );

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(REPLY_API_URL, request, String.class);
            return true;
        } catch (Exception ex) {
            log.warn("LINE replyAudio failed: {}", ex.getMessage());
            return false;
        }
    }

    public boolean pushText(String userId, String messageText) {
        String token = properties.getLine().getChannelAccessToken();
        if (token == null || token.isBlank() || userId == null || userId.isBlank() || "anonymous".equals(userId)) {
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "to", userId,
                "messages", List.of(
                        Map.of("type", "text", "text", messageText)
                )
        );

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(PUSH_API_URL, request, String.class);
            return true;
        } catch (Exception ex) {
            log.warn("LINE pushText failed: {}", ex.getMessage());
            return false;
        }
    }

    public boolean pushAudio(String userId, String audioUrl, int durationMillis) {
        String token = properties.getLine().getChannelAccessToken();
        if (token == null || token.isBlank() || userId == null || userId.isBlank() || "anonymous".equals(userId)
                || audioUrl == null || audioUrl.isBlank()) {
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> message = Map.of(
                "type", "audio",
                "originalContentUrl", audioUrl,
                "duration", Math.max(durationMillis, 1000)
        );

        Map<String, Object> body = Map.of(
                "to", userId,
                "messages", List.of(message)
        );

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(PUSH_API_URL, request, String.class);
            return true;
        } catch (Exception ex) {
            log.warn("LINE pushAudio failed: {}", ex.getMessage());
            return false;
        }
    }
}
