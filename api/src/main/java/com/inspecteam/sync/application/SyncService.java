package com.inspecteam.sync.application;

import com.inspecteam.permission.application.TenantAuthorizationService;
import com.inspecteam.shared.exception.ApiException;
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
    private final SyncMutationExecutor mutationExecutor;
    private final TenantAuthorizationService authorization;

    public SyncService(SyncJdbcRepository sync, SyncMutationExecutor mutationExecutor,
            TenantAuthorizationService authorization) {
        this.sync = sync;
        this.mutationExecutor = mutationExecutor;
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
        return pull(tenantId, userId, admin, cursor, null);
    }

    @Transactional
    public PullResult pull(UUID tenantId, UUID userId, boolean admin, long cursor, UUID deviceId) {
        Membership membership = requireMembership(authorization.activate(tenantId, userId, admin));
        if (deviceId != null) {
            if (!sync.deviceBelongsTo(tenantId, deviceId, membership.id())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Dispositivo não registrado para este usuário");
            }
            sync.touchDevice(tenantId, deviceId);
        }
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
        sync.touchDevice(tenantId, deviceId);
        List<MutationResult> results = new ArrayList<>();
        for (Mutation mutation : mutations) {
            results.add(mutationExecutor.execute(tenantId, userId, admin, deviceId, mutation));
        }
        return results;
    }

    private Membership requireMembership(Membership membership) {
        if (membership == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Administrador global precisa de membership para sincronizar");
        }
        return membership;
    }

    public record Mutation(UUID mutationId, String operation, UUID submissionId, UUID formId,
            UUID formVersionId, int revision, JsonNode answers, Instant clientCreatedAt) { }
    public record MutationResult(UUID mutationId, String status, String message) { }
    public record PullResult(Instant serverTime, long nextCursor,
            List<PublishedForm> forms, List<Tombstone> tombstones) { }
}
