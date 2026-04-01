package com.fivebingo.config;

import com.fivebingo.domain.User;
import com.fivebingo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // application.yml 의 admin.username / password / nickname 값을 읽음
    // 없으면 기본값 사용
    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:admin1234}")
    private String adminPassword;

    @Value("${admin.nickname:방장}")
    private String adminNickname;

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            User host = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .nickname(adminNickname)
                    .build();
            userRepository.save(host);
            log.info("==============================================");
            log.info("방장 계정 생성 완료");
            log.info("  아이디  : {}", adminUsername);
            log.info("  닉네임  : {}", adminNickname);
            log.info("  비밀번호 : application.yml 의 admin.password 참고");
            log.info("==============================================");
        } else {
            log.info("방장 계정이 이미 존재합니다. (username={})", adminUsername);
        }
    }
}