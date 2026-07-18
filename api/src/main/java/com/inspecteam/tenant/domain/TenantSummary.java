package com.inspecteam.tenant.domain;

import java.util.UUID;

public record TenantSummary(
        UUID tenantId,
        String name,
        String slug,
        UUID membershipId,
        String membershipType,
        String membershipStatus) {
}
