package com.sideproject.linebot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sideproject.linebot.config.AppRuntimeProperties;
import com.sideproject.linebot.model.UserMode;
import com.sideproject.linebot.model.UserSessionState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SessionStateServiceTest {

    @Test
    void shouldPersistStateInMemoryWhenRedisIsNotConfigured() {
        AppRuntimeProperties props = new AppRuntimeProperties();
        SessionStateService service = new SessionStateService(props, null, new ObjectMapper());

        UserSessionState state = service.getOrCreate("u1");
        state.setMode(UserMode.FREE_CHAT_MODE);
        service.save("u1", state);

        UserSessionState loaded = service.getOrCreate("u1");
        assertEquals(UserMode.FREE_CHAT_MODE, loaded.getMode());
    }
}
