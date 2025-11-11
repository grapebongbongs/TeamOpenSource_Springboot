package com.example.sbb.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // 간단하게 여기서 바로 생성해서 사용 (빈으로 주입해도 됨)
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 회원 가입용 유저 생성
     */
    public void createUser(String username, String password, String email) {
        SiteUser user = new SiteUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // 비밀번호 암호화
        user.setEmail(email);
        user.setRole("ROLE_USER"); // Spring Security에서 쓰는 기본 형태

        userRepository.save(user);
    }

    /**
     * username으로 유저 한 명 조회 (로그인 유저 찾을 때 사용)
     */
    public SiteUser getUser(String username) {
        Optional<SiteUser> optionalUser = this.userRepository.findByUsername(username);

        if (optionalUser.isPresent()) {
            return optionalUser.get();
        } else {
            // 없을 때 예외 발생 (나중에 커스텀 예외로 바꿔도 됨)
            throw new RuntimeException("사용자를 찾을 수 없습니다: " + username);
        }
    }

    /**
     * username 중복 체크용 (회원가입 시 사용 가능)
     */
    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }
}
