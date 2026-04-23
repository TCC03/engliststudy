package com.sideproject.linebot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppRuntimeProperties {

    private final Line line = new Line();
    private final Ai ai = new Ai();
    private final Data data = new Data();
    private final Session session = new Session();

    public Line getLine() {
        return line;
    }

    public Ai getAi() {
        return ai;
    }

    public Data getData() {
        return data;
    }

    public Session getSession() {
        return session;
    }

    public static class Line {
        private String channelSecret = "";
        private String channelAccessToken = "";
        private boolean signatureValidationEnabled = true;

        public String getChannelSecret() {
            return channelSecret;
        }

        public void setChannelSecret(String channelSecret) {
            this.channelSecret = channelSecret;
        }

        public String getChannelAccessToken() {
            return channelAccessToken;
        }

        public void setChannelAccessToken(String channelAccessToken) {
            this.channelAccessToken = channelAccessToken;
        }

        public boolean isSignatureValidationEnabled() {
            return signatureValidationEnabled;
        }

        public void setSignatureValidationEnabled(boolean signatureValidationEnabled) {
            this.signatureValidationEnabled = signatureValidationEnabled;
        }
    }

    public static class Ai {
        private String provider = "gemini";
        private String devAssistant = "gpt";
        private String geminiApiKey = "";
        private String geminiModel = "gemini-1.5-flash";

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getDevAssistant() {
            return devAssistant;
        }

        public void setDevAssistant(String devAssistant) {
            this.devAssistant = devAssistant;
        }

        public String getGeminiApiKey() {
            return geminiApiKey;
        }

        public void setGeminiApiKey(String geminiApiKey) {
            this.geminiApiKey = geminiApiKey;
        }

        public String getGeminiModel() {
            return geminiModel;
        }

        public void setGeminiModel(String geminiModel) {
            this.geminiModel = geminiModel;
        }
    }

    public static class Data {
        private String vocabularyFile = "data/vocabulary_seed.csv";
        private String vocabularyApiUrl = "https://raw.githubusercontent.com/zenkarsha/words7000/main/words.json";
        private boolean vocabularySyncOnStartup = true;
        private String quizFile = "data/quiz_seed.csv";
        private String grammarFile = "data/grammar_seed.json";
        private String roleplayFile = "data/roleplay_seed.json";

        public String getVocabularyFile() {
            return vocabularyFile;
        }

        public void setVocabularyFile(String vocabularyFile) {
            this.vocabularyFile = vocabularyFile;
        }

        public String getVocabularyApiUrl() {
            return vocabularyApiUrl;
        }

        public void setVocabularyApiUrl(String vocabularyApiUrl) {
            this.vocabularyApiUrl = vocabularyApiUrl;
        }

        public boolean isVocabularySyncOnStartup() {
            return vocabularySyncOnStartup;
        }

        public void setVocabularySyncOnStartup(boolean vocabularySyncOnStartup) {
            this.vocabularySyncOnStartup = vocabularySyncOnStartup;
        }

        public String getQuizFile() {
            return quizFile;
        }

        public void setQuizFile(String quizFile) {
            this.quizFile = quizFile;
        }

        public String getGrammarFile() {
            return grammarFile;
        }

        public void setGrammarFile(String grammarFile) {
            this.grammarFile = grammarFile;
        }

        public String getRoleplayFile() {
            return roleplayFile;
        }

        public void setRoleplayFile(String roleplayFile) {
            this.roleplayFile = roleplayFile;
        }
    }

    public static class Session {
        private int maxQuizQuestions = 5;
        private int ttlSeconds = 3600;

        public int getMaxQuizQuestions() {
            return maxQuizQuestions;
        }

        public void setMaxQuizQuestions(int maxQuizQuestions) {
            this.maxQuizQuestions = maxQuizQuestions;
        }

        public int getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(int ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }
}
