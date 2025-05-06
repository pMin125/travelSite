package com.toyProject.rabbitMQ;

import com.toyProject.entity.ExpiringProductsMessage;
import com.toyProject.entity.Product;
import com.toyProject.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class MessageConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = "queue_name")  // 큐 이름과 일치해야 함
    public void receiveMessage(ExpiringProductsMessage message) {
        String useremail = message.getUserEmail();
        List<Product> expiringProducts = message.getExpiringProducts();

        emailService.sendExpirationReminder(useremail, expiringProducts);
        System.out.println("Received message: " + useremail);
    }
}

