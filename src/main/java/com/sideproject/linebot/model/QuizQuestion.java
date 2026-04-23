package com.sideproject.linebot.model;

public record QuizQuestion(
        String questionText,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        String correctOption,
        String difficulty,
        String topic
) {
    public String toDisplayText(int index, int total) {
        return "測驗 " + index + "/" + total + "\n"
                + questionText + "\n"
                + "A. " + optionA + "\n"
                + "B. " + optionB + "\n"
                + "C. " + optionC + "\n"
                + "D. " + optionD + "\n"
                + "請回覆 A/B/C/D";
    }
}
