package com.toyProject.service;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.IamportResponse;
import com.toyProject.dto.PaymentCallbackDTO;
import com.toyProject.entity.*;
import com.toyProject.exception.ParticipationException;
import com.toyProject.repository.*;
import com.toyProject.service.payment.PaymentProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;

import static com.toyProject.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IamportPaymentProcessor implements PaymentProcessor {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserEntityRepository userEntityRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final IamportClient iamportClient;
    private final ParticipationRepository participationRepository;
    private final QueueNotificationService queueNotificationService;
    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public IamportResponse<com.siot.IamportRestClient.response.Payment> verifyPayment(PaymentCallbackDTO request) {
        try {
            IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse = iamportClient.paymentByImpUid(request.getPaymentUid());

            Order order = orderRepository.findOrderAndPayment(request.getOrderUid())
                    .orElseThrow(() -> new ParticipationException(ORDER_NOT_FOUND));

            if (!iamportResponse.getResponse().getStatus().equals("paid")) {
                orderRepository.delete(order);
                paymentRepository.delete(order.getPayment());
                throw new ParticipationException(PAYMENT_NOT_COMPLETED);
            }

            Long price = order.getPayment().getPrice();
            int iamportPrice = iamportResponse.getResponse().getAmount().intValue();

            if (iamportPrice != price) {
                orderRepository.delete(order);
                paymentRepository.delete(order.getPayment());
                iamportClient.cancelPaymentByImpUid(new CancelData(iamportResponse.getResponse().getImpUid(), true, new BigDecimal(iamportPrice)));

                throw new ParticipationException(PAYMENT_TAMPERED);
            }
            order.setOrderStatus(Order.OrderStatus.SUCCESS);
            order.getPayment().changePaymentBySuccess(Payment.PaymentStatus.SUCCESS, iamportResponse.getResponse().getImpUid());

            orderRepository.save(order);
            paymentRepository.save(order.getPayment());
            return iamportResponse;

        } catch (IamportResponseException | IOException e) {
            throw new ParticipationException(PAYMENT_VERIFICATION_FAILED);
        }
    }

    @Override
    @Transactional
    public void cancelPayment(UserEntity user, Long productId) {
        // 1. 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ParticipationException(PRODUCT_NOT_FOUND));

        // 2. 주문 조회
        Order order = orderRepository.findTopOrderByUserAndProductAndStatus(
                        user, product, Order.OrderStatus.SUCCESS)
                .orElseThrow(() -> new ParticipationException(ORDER_NOT_FOUND));

        // 3. 결제 정보 확인
        Payment payment = order.getPayment();
        if (payment == null || payment.getPaymentUid() == null) {
            throw new ParticipationException(PAYMENT_NOT_FOUND);
        }

        // 4. 아임포트 결제 취소 요청
        try {
            CancelData cancelData = new CancelData(payment.getPaymentUid(), true);
            iamportClient.cancelPaymentByImpUid(cancelData);
        } catch (IamportResponseException | IOException e) {
            throw new ParticipationException(PAYMENT_CANCELED_FAILED);
        }

        // 5. 주문/결제 상태 변경
        order.setOrderStatus(Order.OrderStatus.CANCELED);
        payment.changePaymentStatus(Payment.PaymentStatus.CANCELLED);
        orderRepository.save(order);
        paymentRepository.save(payment);

        // 6. 참여 정보 취소
        participationRepository.findActiveParticipationByUserAndProduct(user, product)
                .ifPresentOrElse(participation -> {
                    participation.setStatus(Participation.ParticipationStatus.CANCELLED);
                    participationRepository.save(participation);
                }, () -> {
                    throw new ParticipationException(PARTICIPATION_NOT_FOUND);
                });

        // 7. 다음 대기자 알림
        queueNotificationService.notifyNextUserInQueue(productId);
    }


    public void handleExpiredPayment(String key) {
        // Key 형식: payment:expire:{productId}:{username}
        try {
            String[] parts = key.split(":");
            Long productId = Long.parseLong(parts[2]);
            String username = parts[3];

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ParticipationException(PRODUCT_NOT_FOUND));
            UserEntity user = userEntityRepository.findByUsername(username)
                    .orElseThrow(() -> new ParticipationException(USERNAME_NOT_FOUND));

            // 참여 상태 CANCELLED 로 변경
            participationRepository.findActiveParticipationByUserAndProduct(user, product)
                    .ifPresent(participation -> {
                        participation.setStatus(Participation.ParticipationStatus.CANCELLED);
                        participationRepository.save(participation);
                        log.info("결제 미완료로 참여 취소됨: {}", username);
                    });

            // Redis에서 키 삭제
            redisTemplate.delete(key);

            // 다음 대기자 알림 로직 호출
            queueNotificationService.notifyNextUserInQueue(productId);

        } catch (Exception e) {
            throw new ParticipationException(EXPIRED_PAYMENT_FAIL);
        }
    }

//    public IamportResponse<com.siot.IamportRestClient.response.Payment> paymentByCallback(PaymentCallbackDTO request) {
//        try {
//            IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse = iamportClient.paymentByImpUid(request.getPaymentUid());
//
//            Order order = orderRepository.findOrderAndPayment(request.getOrderUid())
//                    .orElseThrow(() -> new ParticipationException(ORDER_NOT_FOUND));
//
//            if (!iamportResponse.getResponse().getStatus().equals("paid")) {
//                orderRepository.delete(order);
//                paymentRepository.delete(order.getPayment());
//                throw new ParticipationException(PAYMENT_NOT_COMPLETED);
//            }
//
//            Long price = order.getPayment().getPrice();
//            int iamportPrice = iamportResponse.getResponse().getAmount().intValue();
//
//            if (iamportPrice != price) {
//                orderRepository.delete(order);
//                paymentRepository.delete(order.getPayment());
//                iamportClient.cancelPaymentByImpUid(new CancelData(iamportResponse.getResponse().getImpUid(), true, new BigDecimal(iamportPrice)));
//
//                throw new ParticipationException(PAYMENT_TAMPERED);
//            }
//            order.setOrderStatus(Order.OrderStatus.SUCCESS);
//            order.getPayment().changePaymentBySuccess(Payment.PaymentStatus.SUCCESS, iamportResponse.getResponse().getImpUid());
//
//            orderRepository.save(order);
//            paymentRepository.save(order.getPayment());
//            return iamportResponse;
//
//        } catch (IamportResponseException | IOException e) {
//            throw new ParticipationException(PAYMENT_VERIFICATION_FAILED);
//        }
//    }
//
//    @Transactional
//    public void cancelPayment(UserEntity user, Long productId) {
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new ParticipationException(PRODUCT_NOT_FOUND));
//
//        Order order = orderRepository.findTopOrderByUserAndProductAndStatus(user, product, Order.OrderStatus.SUCCESS)
//                .orElseThrow(() -> new ParticipationException(ORDER_NOT_FOUND));
//
//        Payment payment = order.getPayment();
//        if (payment == null || payment.getPaymentUid() == null) {
//            throw new ParticipationException(PAYMENT_NOT_FOUND);
//        }
//
//        // 1. iamport 결제 취소 요청
//        try {
//            CancelData cancelData = new CancelData(payment.getPaymentUid(), true);
//            iamportClient.cancelPaymentByImpUid(cancelData);
//        } catch (IamportResponseException | IOException e) {
//            throw new ParticipationException(PAYMENT_CANCELED_FAILED);
//        }
//
//        // 2. 상태 업데이트
//        order.setOrderStatus(Order.OrderStatus.CANCELED);
//        payment.changePaymentStatus(Payment.PaymentStatus.CANCELLED);
//        orderRepository.save(order);
//        paymentRepository.save(payment);
//
//        // 3. Participation도 취소
//        Participation participation = participationRepository.findActiveParticipationByUserAndProduct(user, product)
//                .orElseThrow(() -> new ParticipationException(PARTICIPATION_NOT_FOUND));
//
//        participation.setStatus(Participation.ParticipationStatus.CANCELLED);
//        participationRepository.save(participation);
//
//        queueNotificationService.notifyNextUserInQueue(productId);
//    }
}
