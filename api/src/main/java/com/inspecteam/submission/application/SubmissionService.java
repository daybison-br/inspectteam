package com.inspecteam.submission.application;

import com.inspecteam.audit.application.AuditService;
import com.inspecteam.permission.application.TenantAuthorizationService;
import com.inspecteam.shared.exception.ApiException;
import com.inspecteam.submission.domain.SubmissionSummary;
import com.inspecteam.submission.infrastructure.SubmissionJdbcRepository;
import com.inspecteam.tenant.infrastructure.TenantJdbcRepository.Membership;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Service
public class SubmissionService {

    private final SubmissionJdbcRepository submissions;
    private final TenantAuthorizationService authorization;
    private final AuditService audit;

    public SubmissionService(SubmissionJdbcRepository submissions, TenantAuthorizationService authorization,
            AuditService audit) {
        this.submissions = submissions;
        this.authorization = authorization;
        this.audit = audit;
    }

    @Transactional
    public void create(UUID tenantId, UUID userId, boolean platformAdmin, UUID id,
            UUID formId, UUID formVersionId, JsonNode answers, Instant clientCreatedAt) {
        Membership membership = authorization.requireForm(tenantId, userId, platformAdmin, formId, "FORM_USE");
        requireMembership(membership);
        boolean created = submissions.create(id, tenantId, formId, formVersionId,
                membership.id(), answers, clientCreatedAt);
        if (!created && !submissions.exists(tenantId, id, membership.id())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Versão publicada do formulário não encontrada");
        }
        if (created) {
            audit.record(tenantId, userId, membership.id(), "SUBMISSION_CREATED", "SUBMISSION", id,
                    java.util.Map.of("formId", formId));
        }
    }

    @Transactional
    public void updateDraft(UUID tenantId, UUID userId, boolean platformAdmin, UUID formId,
            UUID submissionId, int revision, JsonNode answers) {
        Membership membership = authorization.requireForm(tenantId, userId, platformAdmin, formId, "FORM_USE");
        requireMembership(membership);
        if (!submissions.updateDraft(tenantId, submissionId, membership.id(), revision, answers)) {
            throw new ApiException(HttpStatus.CONFLICT, "Rascunho inexistente, concluído ou modificado em outro dispositivo");
        }
    }

    @Transactional
    public void complete(UUID tenantId, UUID userId, boolean platformAdmin, UUID formId,
            UUID submissionId, int revision, JsonNode answers) {
        Membership membership = authorization.requireForm(tenantId, userId, platformAdmin, formId, "FORM_USE");
        requireMembership(membership);
        if (!submissions.complete(tenantId, submissionId, membership.id(), revision, answers)) {
            throw new ApiException(HttpStatus.CONFLICT, "Submissão inexistente, concluída ou modificada em outro dispositivo");
        }
        audit.record(tenantId, userId, membership.id(), "SUBMISSION_COMPLETED", "SUBMISSION", submissionId,
                java.util.Map.of("formId", formId));
    }

    @Transactional(readOnly = true)
    public List<SubmissionSummary> list(UUID tenantId, UUID userId, boolean platformAdmin, UUID formId) {
        authorization.requireForm(tenantId, userId, platformAdmin, formId, "SUBMISSION_VIEW");
        return submissions.list(tenantId, formId);
    }

    private void requireMembership(Membership membership) {
        if (membership == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Administrador global precisa de membership para enviar formulários");
        }
    }
}
