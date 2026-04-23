package com.sideproject.linebot.model;

public record VocabularyItem(
        String word,
        String pos,
        String meaningZh,
        String exampleEn,
        String exampleZh,
        String level,
        String topicTag
) {
}
