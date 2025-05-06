package com.toyProject.service;

import com.toyProject.entity.UserEntity;
import com.toyProject.exception.ParticipationException;
import com.toyProject.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static com.toyProject.exception.ErrorCode.USERNAME_NOT_FOUND;

@RequiredArgsConstructor
@Service
public class CustomUserService implements UserDetailsService {

    private final UserEntityRepository userEntityRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userEntityRepository.findByUsername(username)
                .orElseThrow(() -> new ParticipationException(USERNAME_NOT_FOUND));
        return user;
    }
}
