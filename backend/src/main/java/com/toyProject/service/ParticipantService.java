package com.toyProject.service;

import com.toyProject.dto.PopularTravelDto;
import com.toyProject.entity.Chat;
import com.toyProject.entity.Participation;
import com.toyProject.entity.Product;
import com.toyProject.entity.UserEntity;
import com.toyProject.exception.ParticipationException;
import com.toyProject.repository.ParticipationRepository;
import com.toyProject.repository.ProductRepository;
import com.toyProject.repository.TravelQueryRepository;
import com.toyProject.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.toyProject.entity.Participation.ParticipationStatus.*;
import static com.toyProject.exception.ErrorCode.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class ParticipantService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ProductRepository productRepository;
    private final ParticipationRepository participationRepository;
    private final TravelQueryRepository travelQueryRepository;
    private final UserEntityRepository userEntityRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final TransactionalService transactionalService;
    private final RedissonClient redissonClient;

    @Async
    public void updateCache() {
        String cacheKey = "popular:travel";
        redisTemplate.delete(cacheKey);  // 기존 캐시 삭제

        // 새로운 인기 여행 상품 데이터 가져오기
        List<PopularTravelDto> freshData = travelQueryRepository.findPopularTravels(10);
        redisTemplate.opsForValue().set(cacheKey, freshData.toString(), Duration.ofMinutes(10)); // 새 데이터로 캐시 갱신
        System.out.println("비동기 캐시 갱신 완료");
    }

    //참여 등록
    public Participation.ParticipationStatus participate(UserEntity user, Long productId) {
        String lockKey = "lock:participate:" + productId;
        RLock lock = redissonClient.getLock(lockKey);

        boolean isLocked = false;
        try {
            isLocked = lock.tryLock(2, 0, TimeUnit.SECONDS);
            if (!isLocked) throw new RuntimeException("참여 시도 중 락 획득 실패");

            Participation.ParticipationStatus result = transactionalService.doParticipate(user, productId);

            updateCache();

            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 획득 중단", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public Map<String, Object> getSummaryForProduct(Long productId) {
        Map<String, Object> result = new HashMap<>();

        // JOINED 상태 참여자 조회
        List<UserEntity> users = participationRepository.findUsersByProductIdAndStatus(productId, JOINED);

        // 연령대 통계 계산
        Map<String, Long> ageStats = new HashMap<>();
        for (UserEntity user : users) {
            LocalDate birthDate = user.getBirthDate();
            if (birthDate == null) continue;

            int age = calculateAge(birthDate);
            String group = (age < 20) ? "10대 이하" :
                    (age < 30) ? "20대" :
                            (age < 40) ? "30대" : "40대 이상";

            ageStats.put(group, ageStats.getOrDefault(group, 0L) + 1);
        }

        result.put("ageStats", ageStats);
        result.put("joinedCount", users.size());

        return result;
    }

    private int calculateAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public Map<String, String> getParticipationStatusMap(Long productId, UserEntity user) {
        Product product = getProductOrThrow(productId);

        Optional<Participation> participationOpt = participationRepository.findActiveParticipationByUserAndProduct(user, product);

        String status = participationOpt
                .map(p -> p.getStatus().name())
                .orElse("NONE");

        return Map.of("status", status);
    }

    public void cancelWaiting(UserEntity user, Long productId) {
        Product product = getProductOrThrow(productId);

        Participation participation = getActiveParticipation(user, product);

        if (participation.getStatus() != WAITING_LIST) {
            throw new ParticipationException(NOT_WAITING_USER);
        }

        participationRepository.delete(participation);

        String listKey = "queue:product:" + productId;
        String setKey = listKey + ":waitingSet";

        redisTemplate.opsForList().remove(listKey, 0, user.getUsername());
        redisTemplate.opsForSet().remove(setKey, user.getUsername());
    }

    public void cancelJoin(UserEntity user, Long productId) {
        Product product = getProductOrThrow(productId);

        Participation participation = getActiveParticipation(user, product);

        if (participation.getStatus() != JOINED) {
            throw new ParticipationException(PARTICIPATION_NOT_FOUND);
        }

        participationRepository.delete(participation);

        String listKey = "queue:product:" + productId;
        String setKey = listKey + ":waitingSet";

        String nextUsername = (String)redisTemplate.opsForList().rightPop(listKey); // ← 제일 오래 기다린 사람

        if (nextUsername != null) {
            UserEntity nextUser = userEntityRepository.findByUsername(nextUsername)
                    .orElseThrow(() -> new ParticipationException(NOT_WAITING_USER));

            Participation waiting = getActiveParticipation(nextUser, product);

            Chat upgradeMessage = Chat.builder()
                    .type(Chat.MessageType.UPGRADE)
                    .roomId(productId.toString())
                    .sender(nextUsername) // 승급된 사용자
                    .message("여행에 합류하게 되었어요 30분내에 입금을해주세요!")
                    .build();

            messagingTemplate.convertAndSend("/sub/notify/" + productId, upgradeMessage);
        } else {
            Chat message = Chat.builder()
                    .roomId(productId.toString())
                    .sender("system")
                    .message("참여 인원이 변경되었습니다.")
                    .type(Chat.MessageType.UPDATE)
                    .build();

            messagingTemplate.convertAndSend("/sub/notify/" + productId, message);
        }

        updateCache();
    }

    private Participation getActiveParticipation(UserEntity user, Product product) {
        return participationRepository.findActiveParticipationByUserAndProduct(user, product)
                .orElseThrow(() -> new ParticipationException(PARTICIPATION_NOT_FOUND));
    }

    private Product getProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ParticipationException(PRODUCT_NOT_FOUND));
    }

    public Long getPaymentTTL(UserEntity user, Long productId) {
        String key = "payment:expire:" + productId + ":" + user.getUsername();
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : 0L;
    }
}
