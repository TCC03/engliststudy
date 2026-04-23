package com.sideproject.linebot.service;

import com.sideproject.linebot.config.AppRuntimeProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class LineMessagingServiceTest {

    @Test
    void shouldReturnFalseWhenTokenMissingForReply() {
        AppRuntimeProperties props = new AppRuntimeProperties();
        props.getLine().setChannelAccessToken("");

        LineMessagingService service = new LineMessagingService(props);
        boolean result = service.replyText("reply-token", "hello");

        assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenTokenMissingForPush() {
        AppRuntimeProperties props = new AppRuntimeProperties();
        props.getLine().setChannelAccessToken("");

        LineMessagingService service = new LineMessagingService(props);
        boolean result = service.pushText("U123", "hello");

        assertFalse(result);
    }
}
