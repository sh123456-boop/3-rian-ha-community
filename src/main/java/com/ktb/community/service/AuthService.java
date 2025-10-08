package com.ktb.community.service;

import com.ktb.community.dto.request.JoinRequestDto;
import com.ktb.community.entity.Role;
import com.ktb.community.entity.User;
import com.ktb.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    // 회원가입
    public void join(JoinRequestDto dto) {
        String email = dto.getEmail();
        String password = dto.getPassword();
        String nickname = dto.getNickname();

        boolean isExistEmail = userRepository.existsByEmail(email);
        boolean isExistNickname = userRepository.existsByNickname(nickname);
        if  (isExistEmail) {
            throw new RuntimeException("이메일이 중복됩니다.");
        }
        if (isExistNickname) {
            throw new RuntimeException("이메일이 중복됩니다.");
        }

        User user = User.builder()
                .nickname(nickname)
                .password(bCryptPasswordEncoder.encode(password))
                .email(email)
                .role(Role.valueOf("USER"))
                .build();
        userRepository.save(user);


    }
}
