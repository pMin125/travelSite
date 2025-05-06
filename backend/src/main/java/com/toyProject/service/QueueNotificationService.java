package com.toyProject.service;

import com.toyProject.entity.Participation;
import com.toyProject.entity.Product;
import com.toyProject.entity.UserEntity;
import com.toyProject.repository.ParticipationRepository;
import com.toyProject.repository.ProductRepository;
import com.toyProject.repository.UserEntityRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QueueNotificationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserEntityRepository userEntityRepository;
    private final ProductRepository productRepository;
    private final ParticipationRepository participationRepository;

    public void handleExpiredPayment(String key) {
        // Key 형식: payment:expire:{productId}:{username}
        try {
            String[] parts = key.split(":");
            Long productId = Long.parseLong(parts[2]);
            String username = parts[3];

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("상품 없음"));
            UserEntity user = userEntityRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("유저 없음"));

            // 참여 상태 CANCELLED 로 변경
            participationRepository.findActiveParticipationByUserAndProduct(user, product)
                    .ifPresent(participation -> {
                        participation.setStatus(Participation.ParticipationStatus.CANCELLED);
                        participationRepository.save(participation);
                        System.out.print("❌ 결제 미완료로 참여 취소됨: {}"+ username);
                    });

            // Redis에서 키 삭제
            redisTemplate.delete(key);

            // 다음 대기자 알림 로직 호출
            notifyNextUserInQueue(productId);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.print("오류");
        }
    }

    @Transactional
    public void notifyNextUserInQueue(Long productId) {
        String listKey = "queue:product:" + productId;
        String setKey = listKey + ":waitingSet";

        String nextUsername = redisTemplate.opsForList().leftPop(listKey);
        if (nextUsername == null) return;

        redisTemplate.opsForSet().remove(setKey, nextUsername);

        UserEntity user = userEntityRepository.findByUsername(nextUsername)
                .orElseThrow(() -> new RuntimeException("유저 없음"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품 없음"));
        Participation participation = participationRepository.findActiveParticipationByUserAndProduct(user, product)
                .orElseThrow(() -> new RuntimeException("참여 없음"));

        participation.setStatus(Participation.ParticipationStatus.WAITING_PAYMENT);
        participationRepository.save(participation);

        Map<String, Object> payload = Map.of(
                "productId", productId,
                "message", "대기 순번이 되었습니다! 30분 안에 결제하세요.",
                "sender", user.getUsername(),
                "type", "WAITING_NOTIFY"
        );

        messagingTemplate.convertAndSend("/sub/notify/" + productId, payload);

        String ttlKey = "payment:expire:" + productId + ":" + user.getUsername();
        redisTemplate.opsForValue().set(ttlKey, "waiting", Duration.ofMinutes(10));
    }
}
