package com.sideproject.linebot.model;

import java.util.List;

public record AiCorrectionResult(
        String originalText,
        String correctedText,
        List<String> errorExplanations,
        String naturalAlternative
) {
    public String toDisplayText() {
        String explain = (errorExplanations == null || errorExplanations.isEmpty())
                ? "- 無"
                : "- " + String.join("\n- ", errorExplanations);
        return "糾錯結果\n"
                + "原句: " + originalText + "\n"
                + "修正: " + correctedText + "\n"
                + "說明:\n" + explain + "\n"
                + "自然說法: " + naturalAlternative;
    }
}
