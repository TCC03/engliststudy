package com.sideproject.linebot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sideproject.linebot.config.AppRuntimeProperties;
import com.sideproject.linebot.model.AiChatResult;
import com.sideproject.linebot.model.AiCorrectionResult;
import com.sideproject.linebot.model.GrammarUnit;
import com.sideproject.linebot.model.QuizQuestion;
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
import java.util.ArrayList;
import java.util.Random;

@Service
public class AiService {

    private final AppRuntimeProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Random random = new Random();

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

    public List<QuizQuestion> generateQuizQuestions(int count) {
        if (!"gemini".equalsIgnoreCase(properties.getAi().getProvider())) {
            return List.of();
        }

        String apiKey = properties.getAi().getGeminiApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return List.of();
        }

        int questionCount = Math.max(3, Math.min(count, 10));
        String[] topics = {"daily conversation", "grammar", "travel", "shopping", "school", "work"};
        String topic = topics[random.nextInt(topics.length)];
        String prompt = "Generate " + questionCount + " English learning multiple-choice questions in strict JSON. "
                + "Return ONLY valid JSON with this structure: {\"questions\":[{\"question_text\":\"...\",\"option_a\":\"...\",\"option_b\":\"...\",\"option_c\":\"...\",\"option_d\":\"...\",\"correct_option\":\"A\",\"difficulty\":\"easy|medium|hard\",\"topic\":\"...\"}]} "
                + "Rules: "
                + "1) Questions should be about " + topic + ". "
                + "2) Make options plausible and only one correct answer. "
                + "3) correct_option must be exactly A, B, C, or D. "
                + "4) Use Traditional Chinese only for explanation is NOT needed; all question text and options should be in English or Chinese as appropriate for an English-learning quiz. "
                + "5) No markdown, no code fences, no extra commentary.";

        try {
            JsonNode result = callGemini(prompt, apiKey);
            String jsonText = cleanJsonText(extractText(result));
            JsonNode parsed = objectMapper.readTree(jsonText);
            JsonNode questionsNode = parsed.path("questions");
            if (!questionsNode.isArray()) {
                return List.of();
            }

            List<QuizQuestion> questions = new ArrayList<>();
            for (JsonNode node : questionsNode) {
                QuizQuestion question = new QuizQuestion(
                        node.path("question_text").asText(""),
                        node.path("option_a").asText(""),
                        node.path("option_b").asText(""),
                        node.path("option_c").asText(""),
                        node.path("option_d").asText(""),
                        node.path("correct_option").asText("A").trim().toUpperCase(Locale.ROOT),
                        node.path("difficulty").asText("easy"),
                        node.path("topic").asText("general")
                );
                if (!question.questionText().isBlank()
                        && !question.optionA().isBlank()
                        && !question.optionB().isBlank()
                        && !question.optionC().isBlank()
                        && !question.optionD().isBlank()) {
                    questions.add(question);
                }
            }
            return questions;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    public GrammarUnit generateGrammarUnit() {
        if (!"gemini".equalsIgnoreCase(properties.getAi().getProvider())) {
            return null;
        }

        String apiKey = properties.getAi().getGeminiApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }

        String[] levels = {"A1", "A2", "B1"};
        String[] topics = {"present simple", "be verb", "present continuous", "past simple", "future simple", "can and could"};
        String level = levels[random.nextInt(levels.length)];
        String topic = topics[random.nextInt(topics.length)];

        String prompt = "Generate one English grammar lesson in strict JSON. "
                + "Return ONLY valid JSON with this structure: {\"week\":1,\"unit\":1,\"title\":\"...\",\"explanation_zh\":\"...\",\"key_points\":[\"...\"],\"examples\":[{\"en\":\"...\",\"zh\":\"...\"}],\"practice\":[{\"q\":\"...\",\"a\":\"...\"}]} "
                + "Rules: "
                + "1) Lesson level: " + level + ". "
                + "2) Topic: " + topic + ". "
                + "3) explanation_zh must be Traditional Chinese. "
                + "4) Provide 3-5 key points, 3 examples, and 3 practice questions. "
                + "5) No markdown, no fences, no extra text.";

        try {
            JsonNode result = callGemini(prompt, apiKey);
            String jsonText = cleanJsonText(extractText(result));
            JsonNode parsed = objectMapper.readTree(jsonText);

            List<String> keyPoints = new ArrayList<>();
            for (JsonNode node : parsed.path("key_points")) {
                keyPoints.add(node.asText());
            }

            List<GrammarUnit.Example> examples = new ArrayList<>();
            for (JsonNode node : parsed.path("examples")) {
                examples.add(new GrammarUnit.Example(
                        node.path("en").asText(""),
                        node.path("zh").asText("")));
            }

            List<GrammarUnit.Practice> practice = new ArrayList<>();
            for (JsonNode node : parsed.path("practice")) {
                practice.add(new GrammarUnit.Practice(
                        node.path("q").asText(""),
                        node.path("a").asText("")));
            }

            if (parsed.path("title").asText("").isBlank() || keyPoints.isEmpty() || examples.isEmpty() || practice.isEmpty()) {
                return null;
            }

            return new GrammarUnit(
                    parsed.path("week").asInt(1),
                    parsed.path("unit").asInt(1),
                    parsed.path("title").asText("Grammar Lesson"),
                    parsed.path("explanation_zh").asText(""),
                    keyPoints,
                    examples,
                    practice
            );
        } catch (Exception ignored) {
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

    private String cleanJsonText(String rawText) {
        if (rawText == null) {
            return "{}";
        }
        String text = rawText.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline >= 0) {
                text = text.substring(firstNewline + 1);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
        }
        return text.trim();
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
