package com.inspecteam.user.domain;

import java.time.Instant;
import java.util.UUID;

public record TenantUser(UUID membershipId, UUID userId, String email, String displayName,
        String membershipType, String status, Instant createdAt) {
}
