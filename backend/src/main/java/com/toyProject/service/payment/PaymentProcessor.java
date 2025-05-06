package com.toyProject.service.payment;

import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import com.toyProject.dto.PaymentCallbackDTO;
import com.toyProject.entity.UserEntity;

public interface PaymentProcessor {
    IamportResponse<Payment> verifyPayment(PaymentCallbackDTO request);
    void cancelPayment(UserEntity user, Long productId);
}
