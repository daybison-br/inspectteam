package com.inspecteam.form.application;

import com.inspecteam.audit.application.AuditService;
import tools.jackson.databind.JsonNode;
import com.inspecteam.form.domain.FormDefinitionValidator;
import com.inspecteam.form.domain.FormSummary;
import com.inspecteam.form.domain.PublishedFormDetails;
import com.inspecteam.form.infrastructure.FormJdbcRepository;
import com.inspecteam.permission.application.TenantAuthorizationService;
import com.inspecteam.shared.exception.ApiException;
import com.inspecteam.tenant.infrastructure.TenantJdbcRepository.Membership;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FormService {

    private final FormJdbcRepository forms;
    private final FormDefinitionValidator validator;
    private final TenantAuthorizationService authorization;
    private final AuditService audit;

    public FormService(FormJdbcRepository forms, FormDefinitionValidator validator,
            TenantAuthorizationService authorization, AuditService audit) {
        this.forms = forms;
        this.validator = validator;
        this.authorization = authorization;
        this.audit = audit;
    }

    @Transactional
    public UUID create(UUID tenantId, UUID userId, boolean platformAdmin,
            String name, String description, JsonNode definition) {
        Membership membership = authorization.require(tenantId, userId, platformAdmin, "FORM_CREATE");
        if (membership == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Administrador global precisa de membership para criar formulários");
        }
        validator.validate(definition);
        UUID formId = forms.create(tenantId, membership.id(), name.trim(), description, definition);
        audit.record(tenantId, userId, membership.id(), "FORM_CREATED", "FORM", formId,
                java.util.Map.of("name", name.trim()));
        return formId;
    }

    @Transactional(readOnly = true)
    public List<FormSummary> list(UUID tenantId, UUID userId, boolean platformAdmin) {
        authorization.require(tenantId, userId, platformAdmin, "FORM_VIEW");
        return forms.list(tenantId);
    }

    @Transactional(readOnly = true)
    public List<FormSummary> available(UUID tenantId, UUID userId, boolean platformAdmin) {
        Membership membership = authorization.activate(tenantId, userId, platformAdmin);
        UUID membershipId = membership == null ? new UUID(0, 0) : membership.id();
        return forms.listAvailable(tenantId, membershipId,
                platformAdmin || membership != null && membership.owner());
    }

    @Transactional(readOnly = true)
    public PublishedFormDetails published(UUID tenantId, UUID formId, UUID userId, boolean platformAdmin) {
        authorization.requireForm(tenantId, userId, platformAdmin, formId, "FORM_USE");
        return forms.findPublished(tenantId, formId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Formulário publicado não encontrado"));
    }
    @Transactional
    public void updateDraft(UUID tenantId, UUID formId, UUID userId,
            boolean platformAdmin, JsonNode definition) {
        authorization.require(tenantId, userId, platformAdmin, "FORM_EDIT");
        validator.validate(definition);
        var draft = forms.findDraft(tenantId, formId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rascunho do formulário não encontrado"));
        forms.updateDraft(tenantId, draft.id(), definition);
        audit.record(tenantId, userId, null, "FORM_DRAFT_UPDATED", "FORM", formId, java.util.Map.of());
    }

    @Transactional
    public void publish(UUID tenantId, UUID formId, UUID userId, boolean platformAdmin) {
        Membership membership = authorization.require(tenantId, userId, platformAdmin, "FORM_PUBLISH");
        if (membership == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Administrador global precisa de membership para publicar formulários");
        }
        var draft = forms.findDraft(tenantId, formId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rascunho do formulário não encontrado"));
        validator.validate(draft.definition());
        forms.publish(tenantId, formId, draft, membership.id());
        audit.record(tenantId, userId, membership.id(), "FORM_PUBLISHED", "FORM", formId,
                java.util.Map.of("version", draft.version()));
    }
}
