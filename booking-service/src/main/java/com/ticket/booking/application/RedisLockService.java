package com.ticket.booking.application;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

//Кастомный редис локер для блокировки мест при бронировании
@Service
public class RedisLockService {

    private final StringRedisTemplate redisTemplate;

    public RedisLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    public boolean tryLock(String lockKey, String lockValue, Duration timeout) {

        Boolean isLocked = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, timeout);

        return Boolean.TRUE.equals(isLocked);
    }


    public void releaseLock(String lockKey, String lockValue) {
        String currentValue = redisTemplate.opsForValue().get(lockKey);
        if (lockValue.equals(currentValue)) {
            redisTemplate.delete(lockKey);
        }
    }
}