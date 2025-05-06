package com.toyProject.controller;

import com.toyProject.dto.SignUpUser;
import com.toyProject.entity.UserEntity;
import com.toyProject.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
public class UserEntityController {

    private final UserService userService;

    // 회원등록
    @PostMapping("/signUp")
    public ResponseEntity<UserEntity> createUser(@RequestBody SignUpUser signUpUser) {
        return ResponseEntity.ok(userService.createUser(signUpUser));
    }
}
