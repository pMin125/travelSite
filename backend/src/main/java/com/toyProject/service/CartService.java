package com.toyProject.service;

import com.toyProject.entity.Cart;
import com.toyProject.entity.CartItem;
import com.toyProject.entity.Product;
import com.toyProject.entity.UserEntity;
import com.toyProject.exception.ErrorCode;
import com.toyProject.exception.ParticipationException;
import com.toyProject.repository.CartRepository;
import com.toyProject.repository.ProductRepository;
import com.toyProject.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.toyProject.exception.ErrorCode.*;

@RequiredArgsConstructor
@Service
public class CartService {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserEntityRepository userEntityRepository;


    @Transactional
    public void addToCart(@AuthenticationPrincipal UserEntity user, Long productId, int quantity) {
        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = createNewCart(user);
                    return newCart;
                });

        Optional<Product> productOptional = productRepository.findById(productId);
        if (productOptional.isEmpty()) {
            throw new ParticipationException(PRODUCT_NOT_FOUND);
        }
        Product product = productOptional.get();

        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            throw new ParticipationException(ALREADY_ADD_PRODUCT);
            // 이미 존재하면 수량 업데이트
//            CartItem cartItem = existingItem.get();
//            cartItem.setQuantity(cartItem.getQuantity() + quantity);
//            System.out.println("기존 상품 수량 업데이트: " + cartItem.getQuantity());
        } else {
            // 존재하지 않으면 새 CartItem 생성
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
            cart.getCartItems().add(cartItem);
        }
        cartRepository.save(cart);
    }

    // 새 장바구니 생성 메서드
    private Cart createNewCart(UserEntity user) {
        Cart newCart = new Cart();
        newCart.setUser(user);
        return cartRepository.save(newCart);
    }

    @Transactional
    public void removeFromCart(UserEntity user, Long productId) {
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ParticipationException(CART_NOT_FOUND));

        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem cartItem = existingItem.get();
            cart.getCartItems().remove(cartItem);
        } else {
            throw new ParticipationException(ALREADY_ADD_PRODUCT);
        }
        cartRepository.save(cart);
    }

    public List<Long> getHeartProductIds(UserEntity user) {
        return cartRepository.findByUserIdWithItems(user.getUserId())
                .map(cart -> cart.getCartItems().stream()
                        .map(cartItem -> cartItem.getProduct().getId())
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }
}
