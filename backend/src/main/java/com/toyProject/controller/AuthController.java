 package com.toyProject.controller;

import com.toyProject.dto.LoginUser;
import com.toyProject.dto.response.LoginResponse;
import com.toyProject.entity.UserEntity;
import com.toyProject.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequiredArgsConstructor
@RestController
public class AuthController {

    private final AuthService authService;

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginUser loginUser, HttpServletResponse response) {
        LoginResponse loginResponse = authService.login(loginUser, response);
        return ResponseEntity.ok(loginResponse);
    }

     // 로그인여부
     @GetMapping("/auth/me")
     public ResponseEntity<?> checkLogin(@AuthenticationPrincipal UserEntity user) {
         return ResponseEntity.ok(authService.checkLogin(user));
     }
}
