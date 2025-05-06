package com.toyProject.service;

import com.toyProject.dto.LoginUser;
import com.toyProject.dto.response.LoginResponse;
import com.toyProject.entity.UserEntity;
import com.toyProject.exception.AuthException;
import com.toyProject.exception.ErrorCode;
import com.toyProject.repository.UserEntityRepository;
import com.toyProject.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.Cookie;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserEntityRepository userEntityRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final CustomUserService customUserService;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginUser loginUser, HttpServletResponse response) {

        UserEntity user = userEntityRepository.findByUsername(loginUser.getUsername())
                .orElseThrow(() -> new AuthException(ErrorCode.USERNAME_NOT_FOUND));


        if (!passwordEncoder.matches(loginUser.getPassword(), user.getPassword())) {
            throw new AuthException(ErrorCode.LOGIN_FAILED);
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginUser.getUsername(),
                        loginUser.getPassword()
                )
        );

        String token = jwtUtil.generateToken(user.getUsername());

        /*헤더방식으로 전환*/
        Cookie cookie = new Cookie("onion_token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60);
        cookie.setSecure(false);
        response.addCookie(cookie);

        return new LoginResponse(token, user.getNickName(), user.getUsername());
    }

    public Map<String, Object> checkLogin(UserEntity user) {
        if (user == null) {
            throw new AuthException(ErrorCode.UNAUTHORIZED);
        }

        return Map.of("id", user.getUserId());
    }
}