package com.sideproject.linebot.service;

import com.sideproject.linebot.config.AppRuntimeProperties;
import com.sideproject.linebot.model.AiChatResult;
import com.sideproject.linebot.model.AiCorrectionResult;
import com.sideproject.linebot.model.QuizQuestion;
import com.sideproject.linebot.model.RoleplayScenario;
import com.sideproject.linebot.model.UserMode;
import com.sideproject.linebot.model.UserSessionState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
public class MessageRouterService {

    private final AppRuntimeProperties appRuntimeProperties;
    private final VocabularyService vocabularyService;
    private final GrammarService grammarService;
    private final QuizService quizService;
    private final RoleplayService roleplayService;
    private final AiService aiService;
    private final SessionStateService sessionStateService;

    public MessageRouterService(
            AppRuntimeProperties appRuntimeProperties,
            VocabularyService vocabularyService,
            GrammarService grammarService,
            QuizService quizService,
            RoleplayService roleplayService,
            AiService aiService,
            SessionStateService sessionStateService
    ) {
        this.appRuntimeProperties = appRuntimeProperties;
        this.vocabularyService = vocabularyService;
        this.grammarService = grammarService;
        this.quizService = quizService;
        this.roleplayService = roleplayService;
        this.aiService = aiService;
        this.sessionStateService = sessionStateService;
    }

    public void handleMessage(String userId, String userText, String replyToken, LineMessagingService messagingService) {
        if (userText == null || userText.isBlank()) {
            replyOrPushText(userId, replyToken, "請輸入指令，例如：今日單字、文法、幫助", messagingService);
            return;
        }

        String safeUserId = userId == null ? "anonymous" : userId;
        UserSessionState state = sessionStateService.getOrCreate(safeUserId);
        String normalized = userText.trim();
        String normalizedLower = normalized.toLowerCase(Locale.ROOT);

        try {
            if (state.getMode() == UserMode.FREE_CHAT_MODE) {
                if ("結束聊天".equals(normalized)) {
                    state.setMode(UserMode.NORMAL);
                    replyOrPushText(safeUserId, replyToken, "已結束 AI 聊天模式，回到一般指令模式。", messagingService);
                    return;
                }
                AiChatResult chatResult = aiService.chat(normalized);
                replyOrPushText(safeUserId, replyToken, chatResult.toDisplayText(), messagingService);
                return;
            }

            if (state.getMode() == UserMode.QUIZ_MODE) {
                replyOrPushText(safeUserId, replyToken, "測驗中請按按鈕選擇答案。", messagingService);
                return;
            }

            if (state.getMode() == UserMode.ROLEPLAY_MODE) {
                String reply = handleRoleplayTurn(state);
                replyOrPushText(safeUserId, replyToken, reply, messagingService);
                return;
            }

            if ("今日單字".equals(normalized)) {
                Map<String, Object> flexCard = vocabularyService.getDailyVocabularyFlex();
                if (flexCard != null) {
                    if (!messagingService.replyFlex(replyToken, "今日單字", flexCard)) {
                        replyOrPushText(safeUserId, replyToken, "卡片載入失敗，請稍後再試", messagingService);
                    }
                } else {
                    replyOrPushText(safeUserId, replyToken, "單字資料不可用，請先同步資料", messagingService);
                }
                return;
            }

            if ("文法".equals(normalized)) {
                replyOrPushText(safeUserId, replyToken, grammarService.getRandomGrammarReply(), messagingService);
                return;
            }

            if ("開始測驗".equals(normalized)) {
                startQuizWithFlex(state, replyToken, messagingService);
                return;
            }

            if ("對話練習".equals(normalized)) {
                String reply = startRoleplay(state);
                replyOrPushText(safeUserId, replyToken, reply, messagingService);
                return;
            }

            if ("AI聊天".equals(normalized)) {
                state.setMode(UserMode.FREE_CHAT_MODE);
                replyOrPushText(safeUserId, replyToken, "已進入 AI 聊天模式，輸入 結束聊天 可離開。", messagingService);
                return;
            }

            if (normalized.startsWith("糾錯(簡單) ")) {
                String text = normalized.substring("糾錯(簡單) ".length()).trim();
                AiCorrectionResult result = aiService.correctSentence(text, true);
                replyOrPushText(safeUserId, replyToken, result.toDisplayText(), messagingService);
                return;
            }

            if (normalized.startsWith("糾錯 ")) {
                String text = normalized.substring("糾錯 ".length()).trim();
                AiCorrectionResult result = aiService.correctSentence(text, false);
                replyOrPushText(safeUserId, replyToken, result.toDisplayText(), messagingService);
                return;
            }

            if ("幫助".equals(normalized) || "help".equals(normalizedLower) || "menu".equals(normalizedLower)) {
                String help = "可用指令：今日單字、開始測驗、文法、對話練習、AI聊天、結束聊天、糾錯 [句子]、幫助\nAI_PROVIDER="
                        + appRuntimeProperties.getAi().getProvider()
                        + "\nDEV_ASSISTANT="
                        + appRuntimeProperties.getAi().getDevAssistant();
                replyOrPushText(safeUserId, replyToken, help, messagingService);
                return;
            }

            replyOrPushText(safeUserId, replyToken, "尚未支援此指令，請輸入 幫助 查看功能。", messagingService);
        } finally {
            sessionStateService.save(safeUserId, state);
        }
    }

    public void handlePostback(String userId, String postbackData, String replyToken, LineMessagingService messagingService) {
        if (postbackData == null || postbackData.isBlank()) {
            messagingService.replyText(replyToken, "無效的命令");
            return;
        }

        String safeUserId = userId == null ? "anonymous" : userId;
        UserSessionState state = sessionStateService.getOrCreate(safeUserId);

        try {
            // Handle pronunciation playback
            if (postbackData.startsWith("pronunciation=")) {
                String word = postbackData.substring("pronunciation=".length());
                if (state.hasPlayedPronunciation(word)) {
                    replyOrPushText(safeUserId, replyToken, "這個單字的發音你已經聽過了，請換一個單字。", messagingService);
                    return;
                }

                String audioUrl = vocabularyService.getVocabularyAudioUrl(word);
                if (!audioUrl.isBlank()) {
                    if (!messagingService.replyAudio(replyToken, audioUrl, 2500)) {
                        if (!messagingService.pushAudio(safeUserId, audioUrl, 2500)) {
                            replyOrPushText(safeUserId, replyToken,
                                    "無法直接播放，請點這個連結收聽「" + word + "」發音：\n" + audioUrl,
                                    messagingService);
                        }
                    }
                    state.markPronunciationPlayed(word);
                } else {
                    replyOrPushText(safeUserId, replyToken, "找不到「" + word + "」的發音來源", messagingService);
                }
                return;
            }
            
            // Handle Rich Menu actions
            if (postbackData.startsWith("action=")) {
                String action = postbackData.substring("action=".length());
                switch (action) {
                    case "start_quiz":
                        startQuizWithFlex(state, replyToken, messagingService);
                        return;
                    case "daily_vocabulary":
                        Map<String, Object> flexCard = vocabularyService.getDailyVocabularyFlex();
                        if (flexCard != null) {
                            messagingService.replyFlex(replyToken, "今日單字", flexCard);
                        } else {
                            messagingService.replyText(replyToken, "單字資料不可用");
                        }
                        return;
                    case "my_vocabulary":
                        messagingService.replyText(replyToken, "我的字庫功能開發中...");
                        return;
                    default:
                        messagingService.replyText(replyToken, "未知的功能");
                        return;
                }
            }

            if (postbackData.startsWith("quiz_answer_")) {
                handleQuizAnswerPostback(state, postbackData, replyToken, messagingService);
                return;
            }

            messagingService.replyText(replyToken, "未知的回覆命令");
        } finally {
            sessionStateService.save(safeUserId, state);
        }
    }

    private void startQuizWithFlex(UserSessionState state, String replyToken, LineMessagingService messagingService) {
        List<QuizQuestion> quizSet = quizService.createQuizSet();
        if (quizSet.isEmpty()) {
            messagingService.replyText(replyToken, "目前沒有測驗題庫，請先準備 data/quiz_seed.csv");
            return;
        }
        state.setMode(UserMode.QUIZ_MODE);
        state.setActiveQuiz(quizSet);
        state.setQuizIndex(0);
        state.setQuizCorrect(0);

        QuizQuestion question = quizSet.get(0);
        List<Map<String, Object>> items = buildQuizQuickReplyItems(question);
        messagingService.replyQuickReply(replyToken, "測驗 1/" + quizSet.size() + "\n" + question.questionText(), items);
    }

    private void replyOrPushText(String userId, String replyToken, String message, LineMessagingService messagingService) {
        if (!messagingService.replyText(replyToken, message)) {
            messagingService.pushText(userId, message);
        }
    }

    private List<Map<String, Object>> buildQuizQuickReplyItems(QuizQuestion question) {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(Map.of("type", "action", "action", Map.of(
                "type", "postback", "label", "A. " + question.optionA(), "data", "quiz_answer_A")));
        items.add(Map.of("type", "action", "action", Map.of(
                "type", "postback", "label", "B. " + question.optionB(), "data", "quiz_answer_B")));
        items.add(Map.of("type", "action", "action", Map.of(
                "type", "postback", "label", "C. " + question.optionC(), "data", "quiz_answer_C")));
        items.add(Map.of("type", "action", "action", Map.of(
                "type", "postback", "label", "D. " + question.optionD(), "data", "quiz_answer_D")));
        return items;
    }

    private void handleQuizAnswerPostback(UserSessionState state, String postbackData, String replyToken, LineMessagingService messagingService) {
        if (state.getMode() != UserMode.QUIZ_MODE) {
            messagingService.replyText(replyToken, "目前不在測驗模式");
            return;
        }

        String answer = postbackData.substring("quiz_answer_".length()).toUpperCase();
        List<QuizQuestion> quizSet = state.getActiveQuiz();
        int index = state.getQuizIndex();

        if (index >= quizSet.size()) {
            messagingService.replyText(replyToken, "測驗已結束");
            return;
        }

        QuizQuestion current = quizSet.get(index);
        boolean correct = current.correctOption().equals(answer);
        if (correct) {
            state.setQuizCorrect(state.getQuizCorrect() + 1);
        }

        int next = index + 1;
        if (next >= quizSet.size()) {
            int score = (int) Math.round((state.getQuizCorrect() * 100.0) / quizSet.size());
            state.setMode(UserMode.NORMAL);
            state.clearQuiz();
            messagingService.replyText(replyToken, "測驗完成！\n分數: " + score + "\n建議：針對錯題再練習一次。");
            return;
        }

        state.setQuizIndex(next);
        String feedback = correct ? "✓ 答對了！" : "✗ 答錯了，正確答案是 " + current.correctOption();
        QuizQuestion nextQuestion = quizSet.get(next);
        List<Map<String, Object>> items = buildQuizQuickReplyItems(nextQuestion);
        messagingService.replyQuickReply(replyToken, feedback + "\n\n測驗 " + (next + 1) + "/" + quizSet.size() + "\n" + nextQuestion.questionText(), items);
    }

    private String startRoleplay(UserSessionState state) {
        // 嘗試使用 AI 生成新的對話場景
        List<String> targetVocabForAi = vocabularyService.getRandomVocabularyForDialogue(5);
        
        RoleplayScenario aiScenario = null;
        if (!targetVocabForAi.isEmpty()) {
            // 隨機選擇語言難度和主題
            String[] levels = {"A1", "A2", "B1"};
            String[] topics = {"restaurant", "shopping", "hotel", "airport", "office", "hospital"};
            String level = levels[new java.util.Random().nextInt(levels.length)];
            String topic = topics[new java.util.Random().nextInt(topics.length)];
            
            aiScenario = roleplayService.generateAiScenario(targetVocabForAi, level, topic);
        }

        // 如果 AI 生成失敗或無詞彙，使用預設或隨機場景
        RoleplayScenario scenario = aiScenario != null ? aiScenario : roleplayService.getRandomScenario();
        
        if (scenario == null || scenario.turns().isEmpty()) {
            return "目前沒有對話練習資料，請先準備 data/roleplay_seed.json";
        }
        
        state.setMode(UserMode.ROLEPLAY_MODE);
        state.setRoleplayTurn(0);
        state.setCurrentRoleplayScenario(scenario);  // Store scenario in session for multi-turn handling
        
        return "已進入對話練習：" + scenario.title() + "\n" + scenario.contextZh() + "\n"
                + scenario.turns().get(0).sampleAiReply();
    }

    private String handleRoleplayTurn(UserSessionState state) {
        RoleplayScenario scenario = state.getCurrentRoleplayScenario();
        
        // Fallback to default scenario if not stored in session
        if (scenario == null) {
            scenario = roleplayService.getDefaultScenario();
        }
        
        if (scenario == null || scenario.turns().isEmpty()) {
            state.setMode(UserMode.NORMAL);
            return "對話練習資料讀取失敗，已回到一般模式。";
        }

        int nextTurn = state.getRoleplayTurn() + 1;
        if (nextTurn >= scenario.turns().size()) {
            state.setMode(UserMode.NORMAL);
            state.setRoleplayTurn(0);
            state.setCurrentRoleplayScenario(null);
            return "本次對話練習完成！\n分數: 80\n常見錯誤: 時態、主詞動詞一致\n建議主題: 日常口說";
        }

        state.setRoleplayTurn(nextTurn);
        return "很棒，繼續！\n" + scenario.turns().get(nextTurn).sampleAiReply();
    }
}
