package com.denisbondd111.authservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate redisTemplate;

    public void storeRefreshToken(String token, Duration ttl) {
        redisTemplate.opsForValue().set(token, "refresh", ttl);
    }

    public boolean isRefreshTokenValid(String token) {
        return redisTemplate.hasKey(token);
    }

    public void blacklistToken(String token, Duration ttl) {
        redisTemplate.opsForValue().set("blacklist:" + token, "1", ttl);
    }

    public boolean isBlacklisted(String token) {
        return redisTemplate.hasKey("blacklist:" + token);
    }

    public void deleteRefreshToken(String token) {
        redisTemplate.delete(token);
    }
}