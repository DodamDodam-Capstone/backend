package com.dodamdodam.backend.auth.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

public class LoginSecurityContextRepository extends HttpSessionSecurityContextRepository {

    private final SessionLifecycleService lifecycle;

    public LoginSecurityContextRepository(SessionLifecycleService lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        super.saveContext(context, request, response);
        lifecycle.contextSaved(request);
    }
}
