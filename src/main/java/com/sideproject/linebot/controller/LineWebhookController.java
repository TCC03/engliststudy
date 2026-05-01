package com.sideproject.linebot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.sideproject.linebot.service.LineMessagingService;
import com.sideproject.linebot.service.LineSignatureService;
import com.sideproject.linebot.service.MessageRouterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/line")
public class LineWebhookController {

    private final ObjectMapper objectMapper;
    private final MessageRouterService messageRouterService;
    private final LineSignatureService lineSignatureService;
    private final LineMessagingService lineMessagingService;

    public LineWebhookController(
            ObjectMapper objectMapper,
            MessageRouterService messageRouterService,
            LineSignatureService lineSignatureService,
            LineMessagingService lineMessagingService
    ) {
        this.objectMapper = objectMapper;
        this.messageRouterService = messageRouterService;
        this.lineSignatureService = lineSignatureService;
        this.lineMessagingService = lineMessagingService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-Line-Signature", required = false) String signature,
            @RequestBody String rawPayload
    ) {
        if (!lineSignatureService.isValidSignature(rawPayload, signature)) {
            return ResponseEntity.badRequest().body("invalid signature");
        }

        JsonNode payload;
        try {
            payload = objectMapper.readTree(rawPayload);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("invalid payload");
        }

        JsonNode eventsNode = payload.path("events");
        if (!eventsNode.isArray()) {
            return ResponseEntity.ok("ok");
        }

        for (JsonNode event : eventsNode) {
            String type = event.path("type").asText("");
            String replyToken = event.path("replyToken").asText("");
            String userId = event.path("source").path("userId").asText("anonymous");

            if ("message".equals(type)) {
                String messageType = event.path("message").path("type").asText("");
                if ("text".equals(messageType)) {
                    String userText = event.path("message").path("text").asText("");
                    messageRouterService.handleMessage(userId, userText, replyToken, lineMessagingService);
                }
            } else if ("postback".equals(type)) {
                String data = event.path("postback").path("data").asText("");
                messageRouterService.handlePostback(userId, data, replyToken, lineMessagingService);
            }
        }

        return ResponseEntity.ok("ok");
    }
}
