package com.toyProject.service;

import com.toyProject.entity.CartItem;
import com.toyProject.entity.Product;
import com.toyProject.entity.UserEntity;
import com.toyProject.rabbitMQ.MessageProducer;
import com.toyProject.repository.CartRepository;
import com.toyProject.repository.ProductRepository;
import com.toyProject.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CartReminderService {
    private final CartRepository cartRepository;
    private final UserEntityRepository userEntityRepository;
    private final ProductRepository productRepository;
    private final MessageProducer messageProducer;

    @Scheduled(cron = "0 20 10 * * ?")
    public void sendReminderForExpiringProducts() {
        System.out.println("시작");
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        List<Product> expiringProducts = productRepository.findExpiringProducts(tomorrow);
        if(expiringProducts.isEmpty()) {
            return;
        }

        List<Long> userIds = cartRepository.findUserIdsByProducts(expiringProducts);
        for(Long userId : userIds) {
            UserEntity user = userEntityRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            messageProducer.sendMessage(user.getEmail(),expiringProducts);
        }
    }
}
