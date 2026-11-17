package com.dodamdodam.backend.auth.config;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm;

@Configuration(proxyBeanMethods = false)
public class PasswordConfig {

    /** 가입·로그인·재설정이 같은 버전 식별자와 원문 호환성을 갖는 해시를 사용한다. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        var pbkdf2 = new Pbkdf2PasswordEncoder("", 16, 600_000, SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
        return new DelegatingPasswordEncoder("pbkdf2-sha256-v1", Map.of("pbkdf2-sha256-v1", pbkdf2));
    }
}
