package com.dodamdodam.backend.auth.session;

import java.util.UUID;

public record AuthenticatedSession(UUID accountId, UUID securityContextId) {
}
