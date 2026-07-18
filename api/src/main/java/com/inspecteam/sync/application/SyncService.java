package com.inspecteam.sync.application;

import com.inspecteam.permission.application.TenantAuthorizationService;
import com.inspecteam.shared.exception.ApiException;
import com.inspecteam.submission.application.SubmissionService;
import com.inspecteam.sync.infrastructure.SyncJdbcRepository;
import com.inspecteam.sync.infrastructure.SyncJdbcRepository.PublishedForm;
import com.inspecteam.sync.infrastructure.SyncJdbcRepository.Tombstone;
import com.inspecteam.tenant.infrastructure.TenantJdbcRepository.Membership;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Service
public class SyncService {

    private final SyncJdbcRepository sync;
    private final SubmissionService submissions;
    private final TenantAuthorizationService authorization;

    public SyncService(SyncJdbcRepository sync, SubmissionService submissions,
            TenantAuthorizationService authorization) {
        this.sync = sync;
        this.submissions = submissions;
        this.authorization = authorization;
    }

    @Transactional
    public void registerDevice(UUID tenantId, UUID userId, boolean admin,
            UUID deviceId, String name, String platform) {
        Membership membership = requireMembership(authorization.activate(tenantId, userId, admin));
        sync.registerDevice(tenantId, membership.id(), deviceId, name, platform);
    }

    @Transactional(readOnly = true)
    public PullResult pull(UUID tenantId, UUID userId, boolean admin, long cursor) {
        Membership membership = requireMembership(authorization.activate(tenantId, userId, admin));
        List<PublishedForm> forms = sync.findPublishedForms(tenantId, membership.id(), membership.owner());
        List<Tombstone> tombstones = sync.findTombstones(tenantId, cursor);
        long nextCursor = tombstones.isEmpty() ? cursor : tombstones.getLast().cursor();
        return new PullResult(Instant.now(), nextCursor, forms, tombstones);
    }

    @Transactional
    public List<MutationResult> push(UUID tenantId, UUID userId, boolean admin,
            UUID deviceId, List<Mutation> mutations) {
        Membership membership = requireMembership(authorization.activate(tenantId, userId, admin));
        if (!sync.deviceBelongsTo(tenantId, deviceId, membership.id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Dispositivo não registrado para este usuário");
        }
        List<MutationResult> results = new ArrayList<>();
        for (Mutation mutation : mutations) {
            if (sync.mutationExists(tenantId, deviceId, mutation.mutationId())) {
                results.add(new MutationResult(mutation.mutationId(), "ALREADY_APPLIED"));
                continue;
            }
            apply(tenantId, userId, admin, mutation);
            sync.recordMutation(tenantId, deviceId, mutation.mutationId(),
                    mutation.submissionId(), mutation.operation());
            results.add(new MutationResult(mutation.mutationId(), "APPLIED"));
        }
        return results;
    }

    private void apply(UUID tenantId, UUID userId, boolean admin, Mutation mutation) {
        switch (mutation.operation()) {
            case "CREATE" -> submissions.create(tenantId, userId, admin, mutation.submissionId(),
                    mutation.formId(), mutation.formVersionId(), mutation.answers(), mutation.clientCreatedAt());
            case "UPDATE" -> submissions.updateDraft(tenantId, userId, admin, mutation.formId(),
                    mutation.submissionId(), mutation.revision(), mutation.answers());
            case "COMPLETE" -> submissions.complete(tenantId, userId, admin, mutation.formId(),
                    mutation.submissionId(), mutation.revision(), mutation.answers());
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Operação de sincronização não suportada");
        }
    }

    private Membership requireMembership(Membership membership) {
        if (membership == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Administrador global precisa de membership para sincronizar");
        }
        return membership;
    }

    public record Mutation(UUID mutationId, String operation, UUID submissionId, UUID formId,
            UUID formVersionId, int revision, JsonNode answers, Instant clientCreatedAt) { }
    public record MutationResult(UUID mutationId, String status) { }
    public record PullResult(Instant serverTime, long nextCursor,
            List<PublishedForm> forms, List<Tombstone> tombstones) { }
}
