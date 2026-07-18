package com.inspecteam.form.domain;

import java.time.Instant;
import java.util.UUID;

public record FormSummary(UUID id, String name, String description, String status,
        Integer publishedVersion, Integer draftVersion, Instant updatedAt) {
}
