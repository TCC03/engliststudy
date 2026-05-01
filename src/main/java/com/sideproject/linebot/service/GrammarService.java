package com.sideproject.linebot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sideproject.linebot.config.AppRuntimeProperties;
import com.sideproject.linebot.model.GrammarUnit;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class GrammarService {

    private final AppRuntimeProperties properties;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    public GrammarService(AppRuntimeProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 開始隨機文法單元的對話練習
     * @return 文法單元及教學內容
     */
    public GrammarUnit startRandomGrammarLesson() {
        List<GrammarUnit> units = loadUnits();
        if (units.isEmpty()) {
            return null;
        }
        return units.get(random.nextInt(units.size()));
    }

    /**
     * 生成文法教學的初始訊息
     * @param unit 文法單元
     * @return 教學訊息
     */
    public String generateGrammarLessonMessage(GrammarUnit unit) {
        if (unit == null) {
            return "目前沒有文法資料，請先準備 data/grammar_seed.json";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("📚 文法小教室 - Week ").append(unit.week()).append(" Unit ").append(unit.unit()).append("\n\n");
        builder.append("【").append(unit.title()).append("】\n\n");
        builder.append("📖 說明:\n");
        builder.append(unit.explanationZh()).append("\n\n");

        // Add key points
        if (!unit.keyPoints().isEmpty()) {
            builder.append("🔑 重點:\n");
            for (String point : unit.keyPoints()) {
                builder.append("• ").append(point).append("\n");
            }
            builder.append("\n");
        }

        // Add first example
        if (!unit.examples().isEmpty()) {
            GrammarUnit.Example example = unit.examples().get(0);
            builder.append("💡 例句:\n");
            builder.append("英: ").append(example.en()).append("\n");
            builder.append("中: ").append(example.zh()).append("\n\n");
        }

        // Prompt for practice
        if (!unit.practice().isEmpty()) {
            builder.append("現在讓我們練習! 請回答下面的問題:\n");
            builder.append(unit.practice().get(0).q());
        }

        return builder.toString();
    }

    /**
     * 檢查文法練習答案
     * @param unit 文法單元
     * @param practiceIndex 練習題索引
     * @param userAnswer 使用者答案
     * @return 反饋訊息
     */
    public String checkGrammarAnswer(GrammarUnit unit, int practiceIndex, String userAnswer) {
        if (unit == null || unit.practice().isEmpty() || practiceIndex >= unit.practice().size()) {
            return "練習不存在。";
        }

        GrammarUnit.Practice practice = unit.practice().get(practiceIndex);
        String correctAnswer = practice.a().trim().toLowerCase();
        String normalizedAnswer = userAnswer.trim().toLowerCase();

        // Simple check - can be improved with AI correction
        boolean isCorrect = normalizedAnswer.equals(correctAnswer) 
            || normalizedAnswer.contains(correctAnswer.split(" ")[0]);

        StringBuilder response = new StringBuilder();
        
        if (isCorrect) {
            response.append("✅ 正確! 很棒!\n\n");
        } else {
            response.append("❌ 不太對。\n");
            response.append("你的答案: ").append(userAnswer).append("\n");
            response.append("正確答案: ").append(practice.a()).append("\n\n");
        }

        // Move to next practice or show more examples
        int nextPracticeIndex = practiceIndex + 1;
        if (nextPracticeIndex < unit.practice().size()) {
            response.append("我們繼續下一題:\n");
            response.append(unit.practice().get(nextPracticeIndex).q());
        } else {
            // Show more examples if no more practice questions
            if (unit.examples().size() > 1) {
                response.append("很好! 再看幾個例句:\n");
                for (int i = 1; i < Math.min(3, unit.examples().size()); i++) {
                    GrammarUnit.Example example = unit.examples().get(i);
                    response.append("• ").append(example.en()).append(" = ").append(example.zh()).append("\n");
                }
                response.append("\n練習完成! 輸入「開始測驗」或「文法」繼續學習.");
            } else {
                response.append("\n練習完成! 輸入「開始測驗」或「文法」繼續學習.");
            }
        }

        return response.toString();
    }

    /**
     * 獲取下一個練習題 (如果存在)
     * @param unit 文法單元
     * @param currentIndex 當前練習索引
     * @return 下一個練習題，或空如果沒有更多
     */
    public String getNextPractice(GrammarUnit unit, int currentIndex) {
        if (unit == null || unit.practice().isEmpty()) {
            return null;
        }
        
        if (currentIndex + 1 >= unit.practice().size()) {
            return null;
        }

        return unit.practice().get(currentIndex + 1).q();
    }

    public String getRandomGrammarReply() {
        List<GrammarUnit> units = loadUnits();
        if (units.isEmpty()) {
            return "目前沒有文法資料，請先準備 data/grammar_seed.json";
        }

        GrammarUnit unit = units.get(random.nextInt(units.size()));
        GrammarUnit.Example example = unit.examples().isEmpty() ? null : unit.examples().get(0);
        GrammarUnit.Practice practice = unit.practice().isEmpty() ? null : unit.practice().get(0);

        StringBuilder builder = new StringBuilder();
        builder.append("文法小教室 - Week ").append(unit.week()).append(" Unit ").append(unit.unit()).append("\n");
        builder.append(unit.title()).append("\n");
        builder.append(unit.explanationZh()).append("\n");
        if (example != null) {
            builder.append("例句: ").append(example.en()).append(" / ").append(example.zh()).append("\n");
        }
        if (practice != null) {
            builder.append("練習: ").append(practice.q()).append("\n");
            builder.append("答案: ").append(practice.a());
        }
        return builder.toString();
    }

    private List<GrammarUnit> loadUnits() {
        Path path = Path.of(properties.getData().getGrammarFile());
        if (!Files.exists(path)) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(path.toFile());
            JsonNode units = root.path("grammar_units");
            if (!units.isArray()) {
                return List.of();
            }

            List<GrammarUnit> result = new ArrayList<>();
            for (JsonNode unitNode : units) {
                List<String> keyPoints = new ArrayList<>();
                for (JsonNode item : unitNode.path("key_points")) {
                    keyPoints.add(item.asText());
                }

                List<GrammarUnit.Example> examples = new ArrayList<>();
                for (JsonNode item : unitNode.path("examples")) {
                    examples.add(new GrammarUnit.Example(item.path("en").asText(), item.path("zh").asText()));
                }

                List<GrammarUnit.Practice> practice = new ArrayList<>();
                for (JsonNode item : unitNode.path("practice")) {
                    practice.add(new GrammarUnit.Practice(item.path("q").asText(), item.path("a").asText()));
                }

                result.add(new GrammarUnit(
                        unitNode.path("week").asInt(),
                        unitNode.path("unit").asInt(),
                        unitNode.path("title").asText(),
                        unitNode.path("explanation_zh").asText(),
                        keyPoints,
                        examples,
                        practice
                ));
            }
            return result;
        } catch (IOException ignored) {
            return List.of();
        }
    }
}
