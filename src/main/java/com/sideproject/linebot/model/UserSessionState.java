package com.sideproject.linebot.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserSessionState {

    private UserMode mode = UserMode.NORMAL;
    private List<QuizQuestion> activeQuiz = new ArrayList<>();
    private int quizIndex;
    private int quizCorrect;
    private int roleplayTurn;
    private List<String> playedPronunciationWords = new ArrayList<>();

    public UserMode getMode() {
        return mode;
    }

    public void setMode(UserMode mode) {
        this.mode = mode;
    }

    public List<QuizQuestion> getActiveQuiz() {
        return activeQuiz;
    }

    public void setActiveQuiz(List<QuizQuestion> activeQuiz) {
        this.activeQuiz = activeQuiz;
    }

    public int getQuizIndex() {
        return quizIndex;
    }

    public void setQuizIndex(int quizIndex) {
        this.quizIndex = quizIndex;
    }

    public int getQuizCorrect() {
        return quizCorrect;
    }

    public void setQuizCorrect(int quizCorrect) {
        this.quizCorrect = quizCorrect;
    }

    public int getRoleplayTurn() {
        return roleplayTurn;
    }

    public void setRoleplayTurn(int roleplayTurn) {
        this.roleplayTurn = roleplayTurn;
    }

    public List<String> getPlayedPronunciationWords() {
        return playedPronunciationWords;
    }

    public void setPlayedPronunciationWords(List<String> playedPronunciationWords) {
        this.playedPronunciationWords = playedPronunciationWords == null ? new ArrayList<>() : playedPronunciationWords;
    }

    public boolean hasPlayedPronunciation(String word) {
        if (word == null || word.isBlank()) {
            return false;
        }
        String normalized = word.trim().toLowerCase(Locale.ROOT);
        return playedPronunciationWords.stream().anyMatch(w -> normalized.equals(w));
    }

    public void markPronunciationPlayed(String word) {
        if (word == null || word.isBlank()) {
            return;
        }
        String normalized = word.trim().toLowerCase(Locale.ROOT);
        if (!playedPronunciationWords.contains(normalized)) {
            playedPronunciationWords.add(normalized);
        }
    }

    public void clearQuiz() {
        this.activeQuiz = new ArrayList<>();
        this.quizIndex = 0;
        this.quizCorrect = 0;
    }
}
