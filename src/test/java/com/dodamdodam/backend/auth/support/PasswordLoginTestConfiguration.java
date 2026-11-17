package com.dodamdodam.backend.auth.support;

import com.dodamdodam.backend.auth.config.PasswordConfig;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration(proxyBeanMethods = false)
public class PasswordLoginTestConfiguration {
    /** 실제 encoder 실행을 유지하면서 비교 횟수와 비교 직후 경합만 관측한다. */
    @Bean
    @Primary
    ObservedPasswords observedPasswords(PasswordConfig config) {
        return new ObservedPasswords(config.passwordEncoder());
    }

    public static final class ObservedPasswords implements PasswordEncoder {
        public final AtomicInteger comparisons = new AtomicInteger();
        public final AtomicReference<Runnable> afterComparison = new AtomicReference<>();
        private final PasswordEncoder delegate;

        /** 운영 정책과 동일한 실제 해시 연산을 위임한다. */
        private ObservedPasswords(PasswordEncoder delegate) {
            this.delegate = delegate;
        }

        /** 합성 계정에 저장할 실제 버전 해시를 만든다. */
        @Override
        public String encode(CharSequence raw) {
            return delegate.encode(raw);
        }

        /** 실제 비교 뒤 한 번만 DB 변경을 실행해 잠금 재검사를 검증한다. */
        @Override
        public boolean matches(CharSequence raw, String encoded) {
            comparisons.incrementAndGet();
            boolean result = delegate.matches(raw, encoded);
            var action = afterComparison.getAndSet(null);
            if (action != null) {
                action.run();
            }
            return result;
        }

        /** 테스트 사이에 관측 상태만 정리하며 운영 저장소는 건드리지 않는다. */
        public void reset() {
            comparisons.set(0);
            afterComparison.set(null);
        }
    }
}
