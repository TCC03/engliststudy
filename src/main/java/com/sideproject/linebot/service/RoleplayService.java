package com.sideproject.linebot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sideproject.linebot.config.AppRuntimeProperties;
import com.sideproject.linebot.model.RoleplayScenario;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class RoleplayService {

    private final AppRuntimeProperties properties;
    private final ObjectMapper objectMapper;

    public RoleplayService(AppRuntimeProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public RoleplayScenario getDefaultScenario() {
        List<RoleplayScenario> scenarios = loadScenarios();
        return scenarios.isEmpty() ? null : scenarios.get(0);
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
}
