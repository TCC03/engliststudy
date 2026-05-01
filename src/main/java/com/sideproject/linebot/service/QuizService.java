package com.sideproject.linebot.service;

import com.sideproject.linebot.config.AppRuntimeProperties;
import com.sideproject.linebot.model.QuizQuestion;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class QuizService {

    private final AppRuntimeProperties properties;
    private final AiService aiService;

    public QuizService(AppRuntimeProperties properties, AiService aiService) {
        this.properties = properties;
        this.aiService = aiService;
    }

    public List<QuizQuestion> createQuizSet() {
        List<QuizQuestion> aiQuestions = aiService.generateQuizQuestions(properties.getSession().getMaxQuizQuestions());
        if (!aiQuestions.isEmpty()) {
            return aiQuestions;
        }

        List<QuizQuestion> all = loadQuizItems();
        if (all.isEmpty()) {
            return List.of();
        }
        Collections.shuffle(all);
        int max = Math.min(properties.getSession().getMaxQuizQuestions(), all.size());
        return new ArrayList<>(all.subList(0, max));
    }

    private List<QuizQuestion> loadQuizItems() {
        Path path = Path.of(properties.getData().getQuizFile());
        if (!Files.exists(path)) {
            return List.of();
        }

        List<QuizQuestion> list = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                String[] parts = line.split(",", -1);
                if (parts.length < 8) {
                    continue;
                }
                list.add(new QuizQuestion(
                        parts[0].trim(),
                        parts[1].trim(),
                        parts[2].trim(),
                        parts[3].trim(),
                        parts[4].trim(),
                        parts[5].trim().toUpperCase(),
                        parts[6].trim(),
                        parts[7].trim()
                ));
            }
        } catch (IOException ignored) {
            return List.of();
        }
        return list;
    }
}
