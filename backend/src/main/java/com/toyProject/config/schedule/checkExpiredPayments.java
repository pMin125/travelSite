package com.toyProject.config.schedule;

import com.toyProject.service.IamportPaymentProcessor;
import lombok.RequiredArgsConstructor;


import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
@RequiredArgsConstructor
public class checkExpiredPayments {
    private final IamportPaymentProcessor iamportPaymentProcessor;
    private final RedisTemplate<String, String> redisTemplate;

    @Scheduled(fixedRate = 10000)
    public void checkExpiredPayment1s() {
        log.info("[Scheduler] checkExpiredPayments 실행됨 - " );

        Set<String> keys = redisTemplate.keys("payment:expire:*");
        if (keys == null) {
            return;
        }

        for (String key : keys) {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            log.info("확인 중: " + key + " (TTL=" + ttl + ")");

            if (ttl <= 10) {
                log.info("TTL 10초 이하 → 조기 만료 처리: {}", key);
                iamportPaymentProcessor.handleExpiredPayment(key);
            }
        }
    }

}
