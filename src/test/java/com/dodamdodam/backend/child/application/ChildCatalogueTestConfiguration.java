package com.dodamdodam.backend.child.application;

import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import com.dodamdodam.backend.child.infrastructure.ChildRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class ChildCatalogueTestConfiguration {

    /** 합성 캐릭터와 고정 시계는 등록 계약 테스트에서만 주입한다. */
    @Bean
    @Primary
    ChildService configuredChildService(ChildRepository repository, SessionSecurityRepository securityRepository) {
        return new ChildService(repository, securityRepository,
                Clock.fixed(Instant.parse("2026-10-31T15:00:00Z"), ZoneOffset.UTC), Set.of("test-character"));
    }
}
