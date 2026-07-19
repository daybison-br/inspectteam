package com.inspecteam.sync.application;

import com.inspecteam.permission.application.TenantAuthorizationService;
import com.inspecteam.shared.exception.ApiException;
import com.inspecteam.submission.application.SubmissionService;
import com.inspecteam.sync.application.SyncService.Mutation;
import com.inspecteam.sync.application.SyncService.MutationResult;
import com.inspecteam.sync.infrastructure.SyncJdbcRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class SyncMutationExecutor {

    private final SyncJdbcRepository sync;
    private final SubmissionService submissions;
    private final TenantAuthorizationService authorization;
    private final TransactionTemplate transaction;

    public SyncMutationExecutor(SyncJdbcRepository sync, SubmissionService submissions,
            TenantAuthorizationService authorization, PlatformTransactionManager transactionManager) {
        this.sync = sync;
        this.submissions = submissions;
        this.authorization = authorization;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public MutationResult execute(UUID tenantId, UUID userId, boolean admin,
            UUID deviceId, Mutation mutation) {
        String previousStatus = transaction.execute(status -> {
            var membership = authorization.activate(tenantId, userId, admin);
            if (membership == null || !sync.deviceBelongsTo(tenantId, deviceId, membership.id())) {
                throw new ApiException(HttpStatus.FORBIDDEN,
                        "Dispositivo não registrado para este usuário");
            }
            return sync.mutationStatus(tenantId, deviceId, mutation.mutationId());
        });
        if (previousStatus != null) {
            return new MutationResult(mutation.mutationId(),
                    "APPLIED".equals(previousStatus) ? "ALREADY_APPLIED" : previousStatus, null);
        }

        String outcome = "APPLIED";
        String message = null;
        try {
            transaction.executeWithoutResult(status -> apply(tenantId, userId, admin, mutation));
        } catch (ApiException exception) {
            outcome = exception.status() == HttpStatus.CONFLICT ? "CONFLICT" : "REJECTED";
            message = exception.getMessage();
        } catch (RuntimeException exception) {
            outcome = "REJECTED";
            message = "Falha inesperada ao processar mutação";
        }

        String finalOutcome = outcome;
        transaction.executeWithoutResult(status -> {
            authorization.activate(tenantId, userId, admin);
            sync.recordMutation(tenantId, deviceId, mutation.mutationId(),
                    mutation.submissionId(), mutation.operation(), finalOutcome);
        });
        return new MutationResult(mutation.mutationId(), outcome, message);
    }

    private void apply(UUID tenantId, UUID userId, boolean admin, Mutation mutation) {
        switch (mutation.operation()) {
            case "CREATE" -> submissions.create(tenantId, userId, admin, mutation.submissionId(),
                    mutation.formId(), mutation.formVersionId(), mutation.answers(), mutation.clientCreatedAt());
            case "UPDATE" -> submissions.updateDraft(tenantId, userId, admin, mutation.formId(),
                    mutation.submissionId(), mutation.revision(), mutation.answers());
            case "COMPLETE" -> submissions.complete(tenantId, userId, admin, mutation.formId(),
                    mutation.submissionId(), mutation.revision(), mutation.answers());
            default -> throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Operação de sincronização não suportada");
        }
    }
}
