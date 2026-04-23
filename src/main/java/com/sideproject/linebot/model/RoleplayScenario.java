package com.sideproject.linebot.model;

import java.util.List;

public record RoleplayScenario(
        int id,
        String title,
        String level,
        String contextZh,
        List<String> targetVocabulary,
        List<Turn> turns
) {
    public record Turn(int turn, String aiPrompt, String sampleAiReply) {
    }
}
