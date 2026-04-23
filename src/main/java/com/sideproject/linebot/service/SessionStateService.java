package com.sideproject.linebot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sideproject.linebot.config.AppRuntimeProperties;
import com.sideproject.linebot.model.UserSessionState;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionStateService {

    private static final String KEY_PREFIX = "linebot:session:";

    private final AppRuntimeProperties properties;
    @Nullable
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, UserSessionState> sessionMap = new ConcurrentHashMap<>();

    public SessionStateService(
            AppRuntimeProperties properties,
            @Nullable StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public UserSessionState getOrCreate(String userId) {
        UserSessionState local = sessionMap.get(userId);
        if (local != null) {
            return local;
        }

        UserSessionState loaded = loadFromRedis(userId);
        sessionMap.put(userId, loaded);
        return loaded;
    }

    public void save(String userId, UserSessionState state) {
        sessionMap.put(userId, state);
        saveToRedis(userId, state);
    }

    private UserSessionState loadFromRedis(String userId) {
        if (redisTemplate == null) {
            return new UserSessionState();
        }

        try {
            String data = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
            if (data == null || data.isBlank()) {
                return new UserSessionState();
            }
            return objectMapper.readValue(data, UserSessionState.class);
        } catch (Exception ignored) {
            return new UserSessionState();
        }
    }

    private void saveToRedis(String userId, UserSessionState state) {
        if (redisTemplate == null) {
            return;
        }

        try {
            String data = objectMapper.writeValueAsString(state);
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + userId,
                    data,
                    Duration.ofSeconds(Math.max(60, properties.getSession().getTtlSeconds()))
            );
        } catch (Exception ignored) {
            // Fallback to in-memory only when Redis is unavailable.
        }
    }
}
