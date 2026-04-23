package com.sideproject.linebot.model;

import java.util.List;

public record GrammarUnit(
        int week,
        int unit,
        String title,
        String explanationZh,
        List<String> keyPoints,
        List<Example> examples,
        List<Practice> practice
) {
    public record Example(String en, String zh) {
    }

    public record Practice(String q, String a) {
    }
}
