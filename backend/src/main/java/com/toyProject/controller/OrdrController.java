package com.toyProject.controller;

import com.toyProject.dto.response.OrderResponse;
import com.toyProject.dto.OrderRequest;
import com.toyProject.entity.Order;
import com.toyProject.entity.Product;
import com.toyProject.entity.UserEntity;
import com.toyProject.service.OrdrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/order")
public class OrdrController {

    private final OrdrService ordrService;

    //장바구니 주문
    @PostMapping("/createOrder")
    public ResponseEntity<OrderResponse> addOrder(@AuthenticationPrincipal UserEntity user,
                                                  @RequestBody OrderRequest orderRequest) {
        return ResponseEntity.ok(ordrService.addOrder(user, orderRequest));
    }

    //단일 상품 주문
    @PostMapping("/single")
    public ResponseEntity<Order> addSingleOrder(@AuthenticationPrincipal UserEntity user, @RequestParam Long productId) {
        Order order = ordrService.createSingleOrder(user, productId);
        return ResponseEntity.ok(order);
    }


    @GetMapping("/orderProduct")
    public ResponseEntity<List<Product>> orderProduct(@AuthenticationPrincipal UserEntity user) {
        List<Product> products = ordrService.orderProduct(user);
        return ResponseEntity.ok(products);
    }
}
