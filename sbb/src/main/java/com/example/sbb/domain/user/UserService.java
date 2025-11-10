package com.example.sbb.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void createUser(String username, String password, String email) {
        SiteUser user = new SiteUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // 암호화 저장
        user.setEmail(email);
        user.setRole("ROLE_USER"); // ✅ 꼭 ROLE_ 접두사 붙여줘야 함
        userRepository.save(user);
    }
}
