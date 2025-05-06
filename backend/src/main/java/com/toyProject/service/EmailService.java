package com.toyProject.service;

import com.toyProject.entity.Product;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendExpirationReminder(String userEmail, List<Product> expiringProducts) {
        String subject = "장바구니 상품 마감 임박 알림";
        StringBuilder body = new StringBuilder();
        body.append("안녕하세요!\n다음 상품들이 곧 마감됩니다:\n\n");

        for (Product product : expiringProducts) {
            body.append("- ")
                    .append(product.getProductName())
                    .append(" (마감일: ")
                    .append(product.getEndDate())
                    .append(")\n");
        }

        body.append("\n서둘러 구매해주세요!");

        // 실제 메일 전송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userEmail);
        message.setSubject(subject);
        message.setText(body.toString());

        mailSender.send(message);
    }
}
