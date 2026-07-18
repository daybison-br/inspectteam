package com.inspecteam.submission.domain;

import java.time.Instant;
import java.util.UUID;

public record SubmissionSummary(UUID id, UUID formId, UUID formVersionId, UUID submittedBy,
        String status, int revision, Instant submittedAt, Instant updatedAt) {
}
