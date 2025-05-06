package com.toyProject.rabbitMQ;

import com.toyProject.entity.ExpiringProductsMessage;
import com.toyProject.entity.Product;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 메시지를 특정 큐에 전송
    public void sendMessage(String userEmail, List<Product> expiringProducts) {
        ExpiringProductsMessage message = new ExpiringProductsMessage(userEmail, expiringProducts);
        rabbitTemplate.convertAndSend("queue_name", message);  // "queue_name"은 큐 이름
        System.out.println("Sent message: " + userEmail); // 보낸 메시지 확인용 로그
    }
}

