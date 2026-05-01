package com.sideproject.linebot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sideproject.linebot.config.AppRuntimeProperties;
import com.sideproject.linebot.model.VocabularyItem;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class VocabularyService {

    private final AppRuntimeProperties appRuntimeProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Random random = new Random();

    public VocabularyService(AppRuntimeProperties appRuntimeProperties, ObjectMapper objectMapper) {
        this.appRuntimeProperties = appRuntimeProperties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void syncApiVocabularyToLocalFileOnStartup() {
        if (!appRuntimeProperties.getData().isVocabularySyncOnStartup()) {
            return;
        }

        List<VocabularyItem> apiVocabulary = fetchVocabularyItemsFromApi();
        if (apiVocabulary.isEmpty()) {
            return;
        }

        writeVocabularyItemsToFile(apiVocabulary);
    }

    public String getDailyVocabularyReply() {
        List<VocabularyItem> vocabularyItems = loadVocabularyItemsFromFile();
        if (vocabularyItems.isEmpty()) {
            return "目前沒有本地單字資料，請檢查 data/vocabulary_seed.csv";
        }

        VocabularyItem item = vocabularyItems.get(random.nextInt(vocabularyItems.size()));
        return "今日單字\n"
                + "Word: " + item.word() + "\n"
                + "POS: " + item.pos() + "\n"
                + "中文: " + item.meaningZh() + "\n"
                + "Example: " + item.exampleEn() + "\n"
                + "翻譯: " + item.exampleZh() + "\n"
                + "Level: " + item.level();
    }

    /**
     * 獲取隨機詞彙列表用於對話生成
     * @param count 所需詞彙數量
     * @return 隨機選中的詞彙列表
     */
    public List<String> getRandomVocabularyForDialogue(int count) {
        List<VocabularyItem> vocabularyItems = loadVocabularyItemsFromFile();
        if (vocabularyItems.isEmpty()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        int targetCount = Math.min(count, vocabularyItems.size());
        for (int i = 0; i < targetCount; i++) {
            VocabularyItem item = vocabularyItems.get(random.nextInt(vocabularyItems.size()));
            if (!result.contains(item.word())) {
                result.add(item.word());
            } else {
                i--; // Retry if duplicate
            }
        }
        return result;
    }

    public Map<String, Object> getDailyVocabularyFlex() {
        List<VocabularyItem> vocabularyItems = loadVocabularyItemsFromFile();
        if (vocabularyItems.isEmpty()) {
            return null;
        }

        VocabularyItem item = vocabularyItems.get(random.nextInt(vocabularyItems.size()));
        return buildVocabularyCard(item);
    }

    public String getVocabularyAudioUrl(String word) {
        if (word == null || word.isBlank()) {
            return "";
        }

        String cleanWord = word.trim();
        String encoded = URLEncoder.encode(cleanWord, StandardCharsets.UTF_8);

        try {
            String apiUrl = "https://api.dictionaryapi.dev/api/v2/entries/en/" + encoded;
            ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);
            String body = response.getBody();
            if (body != null && !body.isBlank()) {
                JsonNode root = objectMapper.readTree(body);
                if (root.isArray() && !root.isEmpty()) {
                    JsonNode phonetics = root.get(0).path("phonetics");
                    if (phonetics.isArray()) {
                        for (JsonNode node : phonetics) {
                            String audio = node.path("audio").asText("").trim();
                            if (!audio.isBlank()) {
                                if (audio.startsWith("//")) {
                                    return "https:" + audio;
                                }
                                return audio;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Fallback to public TTS URL below.
        }

        return "https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&tl=en&q=" + encoded;
    }

    private Map<String, Object> buildVocabularyCard(VocabularyItem item) {
        java.util.List<Object> bodyContents = new java.util.ArrayList<>();
        
        // Word with pronunciation button
        bodyContents.add(
                Map.of(
                        "type", "box",
                        "layout", "horizontal",
                        "spacing", "md",
                        "contents", java.util.List.of(
                                Map.of(
                                        "type", "text",
                                        "text", item.word(),
                                        "size", "xl",
                                        "weight", "bold",
                                        "flex", 5
                                ),
                                Map.of(
                                    "type", "text",
                                    "text", "🔊",
                                    "size", "xl",
                                    "align", "end",
                                    "gravity", "center",
                                    "flex", 0,
                                        "action", Map.of(
                                                "type", "postback",
                                                "label", "🔊",
                                                "data", "pronunciation=" + item.word()
                                    )
                                )
                        )
                )
        );

        return Map.of(
                "type", "bubble",
                "header", Map.of(
                        "type", "box",
                        "layout", "vertical",
                        "contents", java.util.List.of(
                                Map.of(
                                        "type", "text",
                                        "text", "Today's Word",
                                        "weight", "bold",
                                        "size", "sm",
                                        "color", "#999999"
                                )
                        )
                ),
                "body", Map.of(
                        "type", "box",
                        "layout", "vertical",
                        "spacing", "md",
                        "contents", addBodyContents(bodyContents, item)
)
                );
    }

    private java.util.List<Object> addBodyContents(java.util.List<Object> contents, VocabularyItem item) {
        contents.add(Map.of(
                "type", "text",
                "text", item.pos(),
                "size", "sm",
                "color", "#666666"
        ));
        contents.add(Map.of(
                "type", "separator"
        ));
        contents.add(Map.of(
                "type", "text",
                "text", "中文",
                "size", "xs",
                "weight", "bold",
                "color", "#999999"
        ));
        contents.add(Map.of(
                "type", "text",
                "text", item.meaningZh(),
                "size", "md",
                "wrap", true
        ));
        
        // Only add example section if there's actual example text
        if (!item.exampleEn().trim().isEmpty() || !item.exampleZh().trim().isEmpty()) {
            contents.add(Map.of(
                    "type", "separator"
            ));
            contents.add(Map.of(
                    "type", "text",
                    "text", "Example",
                    "size", "xs",
                    "weight", "bold",
                    "color", "#999999"
            ));
            if (!item.exampleEn().trim().isEmpty()) {
                contents.add(Map.of(
                        "type", "text",
                        "text", item.exampleEn(),
                        "size", "sm",
                        "wrap", true,
                        "color", "#555555"
                ));
            }
            if (!item.exampleZh().trim().isEmpty()) {
                contents.add(Map.of(
                        "type", "text",
                        "text", item.exampleZh(),
                        "size", "sm",
                        "wrap", true,
                        "color", "#888888"
                ));
            }
        }
        
        contents.add(Map.of(
                "type", "separator"
        ));
        contents.add(Map.of(
                "type", "text",
                "text", "Level: " + item.level(),
                "size", "xs",
                "color", "#999999"
        ));
        return contents;
    }

    private List<VocabularyItem> fetchVocabularyItemsFromApi() {
        String apiUrl = appRuntimeProperties.getData().getVocabularyApiUrl();
        if (apiUrl == null || apiUrl.isBlank()) {
            return List.of();
        }

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(body);
            if (!root.isArray()) {
                return List.of();
            }

            List<VocabularyItem> result = new ArrayList<>();
            for (JsonNode node : root) {
                String rawWord = node.path("word").asText("").trim();
                String meaningZh = node.path("translate").asText("").trim();
                if (rawWord.isBlank()) {
                    continue;
                }

                String normalizedWord = rawWord;
                String pos = "";
                int leftParen = rawWord.indexOf('(');
                int rightParen = rawWord.indexOf(')');
                if (leftParen > 0 && rightParen > leftParen) {
                    normalizedWord = rawWord.substring(0, leftParen).trim();
                    pos = rawWord.substring(leftParen + 1, rightParen).trim();
                }

                result.add(new VocabularyItem(
                        normalizedWord,
                        pos,
                        meaningZh,
                        "",
                        "",
                        "7000",
                        "7000-words"
                ));
            }
            return result;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private void writeVocabularyItemsToFile(List<VocabularyItem> vocabularyItems) {
        String filePath = appRuntimeProperties.getData().getVocabularyFile();
        Path path = Path.of(filePath);

        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            List<String> lines = new ArrayList<>();
            lines.add("word,pos,meaningZh,exampleEn,exampleZh,level,topicTag");
            for (VocabularyItem item : vocabularyItems) {
                lines.add(toCsvLine(List.of(
                        item.word(),
                        item.pos(),
                        item.meaningZh(),
                        item.exampleEn(),
                        item.exampleZh(),
                        item.level(),
                        item.topicTag()
                )));
            }
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Keep existing local file if sync fails.
        }
    }

    private String toCsvLine(List<String> columns) {
        List<String> escaped = new ArrayList<>();
        for (String column : columns) {
            String value = column == null ? "" : column;
            String safe = value.replace("\"", "\"\"");
            escaped.add("\"" + safe + "\"");
        }
        return String.join(",", escaped);
    }

    private List<VocabularyItem> loadVocabularyItemsFromFile() {
        String filePath = appRuntimeProperties.getData().getVocabularyFile();
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            return List.of();
        }

        List<VocabularyItem> result = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                List<String> parts = parseCsvLine(lines.get(i));
                if (parts.size() < 7) {
                    continue;
                }
                result.add(new VocabularyItem(
                        parts.get(0).trim(),
                        parts.get(1).trim(),
                        parts.get(2).trim(),
                        parts.get(3).trim(),
                        parts.get(4).trim(),
                        parts.get(5).trim(),
                        parts.get(6).trim()
                ));
            }
        } catch (IOException ignored) {
            return List.of();
        }
        return result;
    }

    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        result.add(current.toString());
        return result;
    }
}
