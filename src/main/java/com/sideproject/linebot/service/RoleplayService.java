package com.sideproject.linebot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sideproject.linebot.config.AppRuntimeProperties;
import com.sideproject.linebot.model.RoleplayScenario;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class RoleplayService {

    private final AppRuntimeProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Random random = new Random();
    private volatile List<RoleplayScenario> cachedScenarios = List.of();

    public RoleplayService(AppRuntimeProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void preloadRoleplayCache() {
        cachedScenarios = List.copyOf(loadScenarios());
    }

    public RoleplayScenario getDefaultScenario() {
        List<RoleplayScenario> scenarios = getScenarios();
        return scenarios.isEmpty() ? null : scenarios.get(0);
    }

    public RoleplayScenario getRandomScenario() {
        List<RoleplayScenario> scenarios = getScenarios();
        if (scenarios.isEmpty()) {
            return null;
        }
        return scenarios.get(random.nextInt(scenarios.size()));
    }

    /**
     * AI 生成對話場景（基於目標詞彙）
     * @param targetVocabulary 目標詞彙列表
     * @param languageLevel 語言難度 (A1, A2, B1, B2)
     * @param topic 主題 (e.g., "restaurant", "shopping", "travel")
     * @return 生成的對話場景或 null（若 AI 生成失敗）
     */
    public RoleplayScenario generateAiScenario(List<String> targetVocabulary, String languageLevel, String topic) {
        String apiKey = properties.getAi().getGeminiApiKey();
        if (apiKey == null || apiKey.isBlank() || !"gemini".equalsIgnoreCase(properties.getAi().getProvider())) {
            return null;
        }

        String vocab = String.join(", ", targetVocabulary);
        String prompt = "Generate a conversational roleplay scenario in strict JSON format. "
                + "Use ONLY English for context and dialogue. "
                + "The JSON must have this structure: {\"title\": \"...\", \"context_zh\": \"...\", \"turns\": [{\"turn\": 1, \"ai_prompt\": \"...\", \"sample_ai_reply\": \"...\"}, ...]} "
                + "Parameters: "
                + "- Language level: " + languageLevel
                + "- Topic: " + topic
                + "- Must use vocabulary: " + vocab
                + "- 4-5 conversation turns "
                + "- Each turn should be natural dialogue "
                + "- context_zh should explain the scenario in Traditional Chinese"
                + "Return ONLY valid JSON, no markdown or extra text.";

        try {
            JsonNode result = callGemini(prompt, apiKey);
            String jsonText = extractText(result);
            
            // Parse the generated JSON
            JsonNode parsed = objectMapper.readTree(jsonText);
            
            // Extract turns
            List<RoleplayScenario.Turn> turns = new ArrayList<>();
            JsonNode turnsNode = parsed.path("turns");
            if (turnsNode.isArray()) {
                for (JsonNode turnNode : turnsNode) {
                    turns.add(new RoleplayScenario.Turn(
                            turnNode.path("turn").asInt(),
                            turnNode.path("ai_prompt").asText(),
                            turnNode.path("sample_ai_reply").asText()
                    ));
                }
            }
            
            // Create scenario
            return new RoleplayScenario(
                    System.identityHashCode(parsed),  // Use hash as unique ID
                    parsed.path("title").asText("AI Generated Scenario"),
                    languageLevel,
                    parsed.path("context_zh").asText(""),
                    targetVocabulary,
                    turns
            );
        } catch (Exception ex) {
            // Log error and return null
            System.err.println("Failed to generate AI scenario: " + ex.getMessage());
            return null;
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

    private List<RoleplayScenario> loadScenarios() {
        Path path = Path.of(properties.getData().getRoleplayFile());
        if (!Files.exists(path)) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(path.toFile());
            JsonNode scenariosNode = root.path("scenarios");
            if (!scenariosNode.isArray()) {
                return List.of();
            }

            List<RoleplayScenario> result = new ArrayList<>();
            for (JsonNode scenarioNode : scenariosNode) {
                List<String> targetVocabulary = new ArrayList<>();
                for (JsonNode item : scenarioNode.path("target_vocabulary")) {
                    targetVocabulary.add(item.asText());
                }

                List<RoleplayScenario.Turn> turns = new ArrayList<>();
                for (JsonNode turnNode : scenarioNode.path("turns")) {
                    turns.add(new RoleplayScenario.Turn(
                            turnNode.path("turn").asInt(),
                            turnNode.path("ai_prompt").asText(),
                            turnNode.path("sample_ai_reply").asText()
                    ));
                }

                result.add(new RoleplayScenario(
                        scenarioNode.path("id").asInt(),
                        scenarioNode.path("title").asText(),
                        scenarioNode.path("level").asText(),
                        scenarioNode.path("context_zh").asText(),
                        targetVocabulary,
                        turns
                ));
            }
            return result;
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private List<RoleplayScenario> getScenarios() {
        if (!cachedScenarios.isEmpty()) {
            return cachedScenarios;
        }

        cachedScenarios = List.copyOf(loadScenarios());
        return cachedScenarios;
    }
}
