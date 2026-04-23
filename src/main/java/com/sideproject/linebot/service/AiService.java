package com.sideproject.linebot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sideproject.linebot.config.AppRuntimeProperties;
import com.sideproject.linebot.model.AiChatResult;
import com.sideproject.linebot.model.AiCorrectionResult;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AiService {

    private final AppRuntimeProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public AiService(AppRuntimeProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public AiCorrectionResult correctSentence(String inputText, boolean simpleMode) {
        if (!"gemini".equalsIgnoreCase(properties.getAi().getProvider())) {
            return localCorrection(inputText);
        }

        String apiKey = properties.getAi().getGeminiApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return localCorrection(inputText);
        }

        try {
            String prompt = "You are an English tutor. Return strict JSON with fields original_text, corrected_text, error_explanations(array), natural_alternative. "
                    + "User text: " + inputText
                    + (simpleMode ? " (simple explanation for A1-A2)." : " (normal explanation for B1).");
            JsonNode result = callGemini(prompt, apiKey);
            String jsonText = extractText(result);
            JsonNode parsed = objectMapper.readTree(jsonText);
            return new AiCorrectionResult(
                    parsed.path("original_text").asText(inputText),
                    parsed.path("corrected_text").asText(inputText),
                    parsed.path("error_explanations").isArray()
                            ? objectMapper.convertValue(parsed.path("error_explanations"), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class))
                            : List.of("請再確認句型與時態"),
                    parsed.path("natural_alternative").asText(inputText)
            );
        } catch (Exception ignored) {
            return localCorrection(inputText);
        }
    }

    public AiChatResult chat(String userText) {
        if (!"gemini".equalsIgnoreCase(properties.getAi().getProvider())) {
            return localChat(userText);
        }

        String apiKey = properties.getAi().getGeminiApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return localChat(userText);
        }

        try {
            String prompt = "You are a friendly English chat partner. Return strict JSON with fields has_grammar_error(boolean), corrected_user_text, correction_note_zh, ai_reply_en. "
                    + "User text: " + userText;
            JsonNode result = callGemini(prompt, apiKey);
            String jsonText = extractText(result);
            JsonNode parsed = objectMapper.readTree(jsonText);
            return new AiChatResult(
                    parsed.path("has_grammar_error").asBoolean(false),
                    parsed.path("corrected_user_text").asText(userText),
                    parsed.path("correction_note_zh").asText(""),
                    parsed.path("ai_reply_en").asText("Nice! Tell me more.")
            );
        } catch (Exception ignored) {
            return localChat(userText);
        }
    }

    private JsonNode callGemini(String prompt, String apiKey) {
        String model = properties.getAi().getGeminiModel();
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model
                + ":generateContent?key="
                + apiKey;

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);
        return response.getBody();
    }

    private String extractText(JsonNode root) {
        return root.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText("{}");
    }

    private AiCorrectionResult localCorrection(String inputText) {
        String corrected = inputText
                .replace("I goed", "I went")
                .replace("She don't", "She doesn't")
                .replace("He don't", "He doesn't");
        return new AiCorrectionResult(
                inputText,
                corrected,
                List.of("請確認主詞動詞一致與時態使用"),
                corrected
        );
    }

    private AiChatResult localChat(String userText) {
        boolean looksBadGrammar = userText.toLowerCase(Locale.ROOT).contains("i goed");
        if (looksBadGrammar) {
            return new AiChatResult(true, "I went", "goed 不是過去式，go 的過去式是 went", "Great effort! What did you do at school?");
        }
        return new AiChatResult(false, userText, "", "Nice! Can you share one more detail?");
    }
}
