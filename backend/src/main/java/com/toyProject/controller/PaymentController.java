package com.toyProject.controller;

import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import com.toyProject.dto.PaymentCallbackDTO;
import com.toyProject.entity.UserEntity;
import com.toyProject.service.IamportPaymentProcessor;
import com.toyProject.service.payment.PaymentProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentProcessor paymentProcessor;
    //결제 후
    @ResponseBody
    @PostMapping
    public ResponseEntity<IamportResponse<Payment>> validationPayment(@AuthenticationPrincipal UserEntity user, @RequestBody PaymentCallbackDTO request) {
        return ResponseEntity.ok(paymentProcessor.verifyPayment(request));
    }

    //취소
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelPayment(@AuthenticationPrincipal UserEntity user, @RequestParam Long productId) {
        paymentProcessor.cancelPayment(user, productId);
        return ResponseEntity.ok("참여 취소 완료");
    }

}
