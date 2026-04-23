package com.sideproject.linebot.model;

public record AiChatResult(
        boolean hasGrammarError,
        String correctedUserText,
        String correctionNoteZh,
        String aiReplyEn
) {
    public String toDisplayText() {
        String correctionPart = hasGrammarError
                ? "修正: " + correctedUserText + "\n說明: " + correctionNoteZh + "\n"
                : "修正: 無\n";
        return correctionPart + "AI: " + aiReplyEn;
    }
}
