package com.dodamdodam.backend.global.config;

import com.dodamdodam.backend.auth.session.LoginSecurityContextRepository;
import com.dodamdodam.backend.auth.session.LoginSessionAuthenticationStrategy;
import com.dodamdodam.backend.auth.session.SessionLifecycleService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

@Configuration(proxyBeanMethods = false)
public class SessionConfig {

    @Bean
    SessionRepositoryCustomizer<JdbcIndexedSessionRepository> explicitSessionDeletion() {
        // 4.1.1 기본 deleteById SQL은 음수 timeout을 제외하므로 명시적 폐기는 ID만으로 삭제한다.
        return repository -> repository.setDeleteSessionQuery("DELETE FROM %TABLE_NAME% WHERE SESSION_ID = ?");
    }

    @Bean
    LoginSessionAuthenticationStrategy loginSessionAuthenticationStrategy(SessionLifecycleService lifecycle,
            CookieCsrfTokenRepository csrfRepository, CsrfTokenRequestHandler csrfRequestHandler) {
        return new LoginSessionAuthenticationStrategy(lifecycle, csrfRepository, csrfRequestHandler);
    }

    @Bean
    SecurityContextRepository securityContextRepository(SessionLifecycleService lifecycle) {
        return new DelegatingSecurityContextRepository(new RequestAttributeSecurityContextRepository(),
                new LoginSecurityContextRepository(lifecycle));
    }
}
