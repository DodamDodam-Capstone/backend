package com.dodamdodam.backend.child;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ChildService {

    private final ChildRepository repository;

    public ChildService(ChildRepository repository) {
        this.repository = repository;
    }

    public ChildView requireChildAccess(UUID accountId, UUID childId) {
        return repository.findOwned(accountId, childId).orElseThrow(ChildException::notFound);
    }

    /** requireAuthenticatedSession → requireChildAccess 순서로 검증한 뒤 호출하는 내부 조회다. */
    public ChildContext getChildContext(UUID childId) {
        var child = repository.findById(childId).orElseThrow(ChildException::notFound);
        return new ChildContext(child.childId(), child.name(), child.nickname(), child.birthDate(),
                child.interests(), child.characterId(), child.createdAt());
    }
}
