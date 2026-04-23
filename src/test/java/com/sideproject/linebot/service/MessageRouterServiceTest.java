package com.sideproject.linebot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sideproject.linebot.config.AppRuntimeProperties;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageRouterServiceTest {

    @Test
    void shouldReplyToHelpAliasFromRichMenu() {
        AppRuntimeProperties properties = new AppRuntimeProperties();
        SessionStateService sessionStateService = new SessionStateService(properties, null, new ObjectMapper());
        MessageRouterService routerService = new MessageRouterService(
                properties,
                mock(VocabularyService.class),
                mock(GrammarService.class),
                mock(QuizService.class),
                mock(RoleplayService.class),
                mock(AiService.class),
                sessionStateService
        );

        LineMessagingService messagingService = mock(LineMessagingService.class);
        when(messagingService.replyText(anyString(), anyString())).thenReturn(true);

        routerService.handleMessage("U1", "help", "reply-token", messagingService);

        verify(messagingService).replyText(anyString(), anyString());
    }

    @Test
    void shouldFallbackToPushWhenReplyFails() {
        AppRuntimeProperties properties = new AppRuntimeProperties();
        SessionStateService sessionStateService = new SessionStateService(properties, null, new ObjectMapper());
        MessageRouterService routerService = new MessageRouterService(
                properties,
                mock(VocabularyService.class),
                mock(GrammarService.class),
                mock(QuizService.class),
                mock(RoleplayService.class),
                mock(AiService.class),
                sessionStateService
        );

        LineMessagingService messagingService = mock(LineMessagingService.class);
        when(messagingService.replyText(anyString(), anyString())).thenReturn(false);
        when(messagingService.pushText(anyString(), anyString())).thenReturn(true);

        routerService.handleMessage("U1", "未知指令", "reply-token", messagingService);

        verify(messagingService).pushText("U1", "尚未支援此指令，請輸入 幫助 查看功能。");
    }

    @Test
    void shouldFallbackToPushWhenDailyVocabularyReplyFails() {
        AppRuntimeProperties properties = new AppRuntimeProperties();
        SessionStateService sessionStateService = new SessionStateService(properties, null, new ObjectMapper());
        VocabularyService vocabularyService = mock(VocabularyService.class);
        when(vocabularyService.getDailyVocabularyFlex()).thenReturn(Map.of(
                "type", "bubble",
                "body", Map.of("type", "box", "layout", "vertical", "contents", java.util.List.of())
        ));

        MessageRouterService routerService = new MessageRouterService(
                properties,
                vocabularyService,
                mock(GrammarService.class),
                mock(QuizService.class),
                mock(RoleplayService.class),
                mock(AiService.class),
                sessionStateService
        );

        LineMessagingService messagingService = mock(LineMessagingService.class);
        when(messagingService.replyFlex(anyString(), anyString(), anyMap())).thenReturn(false);
        when(messagingService.replyText(anyString(), anyString())).thenReturn(false);
        when(messagingService.pushText(anyString(), anyString())).thenReturn(true);

        routerService.handleMessage("U1", "今日單字", "reply-token", messagingService);

        verify(messagingService).pushText("U1", "卡片載入失敗，請稍後再試");
    }

    @Test
    void shouldNotReplayPronunciationForSameWordInSameSession() {
        AppRuntimeProperties properties = new AppRuntimeProperties();
        SessionStateService sessionStateService = new SessionStateService(properties, null, new ObjectMapper());
        VocabularyService vocabularyService = mock(VocabularyService.class);
        when(vocabularyService.getVocabularyAudioUrl("able")).thenReturn("https://example.com/able.mp3");

        MessageRouterService routerService = new MessageRouterService(
                properties,
                vocabularyService,
                mock(GrammarService.class),
                mock(QuizService.class),
                mock(RoleplayService.class),
                mock(AiService.class),
                sessionStateService
        );

        LineMessagingService messagingService = mock(LineMessagingService.class);
        when(messagingService.replyAudio(anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(true);
        when(messagingService.replyText(anyString(), anyString())).thenReturn(true);

        routerService.handlePostback("U1", "pronunciation=able", "reply-token-1", messagingService);
        routerService.handlePostback("U1", "pronunciation=able", "reply-token-2", messagingService);

        verify(messagingService).replyAudio(org.mockito.ArgumentMatchers.eq("reply-token-1"), org.mockito.ArgumentMatchers.eq("https://example.com/able.mp3"), org.mockito.ArgumentMatchers.eq(2500));
        verify(messagingService).replyText(org.mockito.ArgumentMatchers.eq("reply-token-2"), contains("已經聽過"));
    }
}