package com.toyProject.controller;

import com.toyProject.entity.Cart;
import com.toyProject.entity.CartItem;
import com.toyProject.entity.UserEntity;
import com.toyProject.repository.CartRepository;
import com.toyProject.service.CartService;
import com.toyProject.service.CustomUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/cart")
public class CartController {
    private final CustomUserService customUserService;
    private final CartService cartService;
    private final CartRepository cartRepository;

    //카트 추가
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@AuthenticationPrincipal UserEntity user,
                                            @RequestParam("productId") Long productId,
                                            @RequestParam("quantity") int quantity) {
        cartService.addToCart(user, productId, quantity);
        return ResponseEntity.ok("상품이 장바구니에 추가되었습니다.");
    }

    @PostMapping("/remove")
    public ResponseEntity<?> removeFromCart(@AuthenticationPrincipal UserEntity user,
                                                 @RequestParam("productId") Long productId) {
        cartService.removeFromCart(user, productId);
        return ResponseEntity.ok(Map.of("message", "상품이 장바구니에서 제거되었습니다."));
    }
    //카트 조회
    @GetMapping("/items")
    public List<CartItem> getCartItems(@AuthenticationPrincipal UserEntity user) {
        return cartRepository.findByUserIdWithDetails(user.getUserId())
                .map(Cart::getCartItems)
                .orElse(Collections.emptyList());
    }

    // 하트 기능
    @GetMapping("/heart/me")
    public ResponseEntity<?> getHeartStatus(@AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인하지 않았습니다."));
        }
        List<Long> heartProductIds = cartService.getHeartProductIds(user);
        return ResponseEntity.ok(Map.of("likedProductIds", heartProductIds));
    }
}