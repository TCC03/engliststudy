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
