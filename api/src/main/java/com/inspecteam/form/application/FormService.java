package com.inspecteam.form.application;

import tools.jackson.databind.JsonNode;
import com.inspecteam.form.domain.FormDefinitionValidator;
import com.inspecteam.form.domain.FormSummary;
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

    public FormService(FormJdbcRepository forms, FormDefinitionValidator validator,
            TenantAuthorizationService authorization) {
        this.forms = forms;
        this.validator = validator;
        this.authorization = authorization;
    }

    @Transactional
    public UUID create(UUID tenantId, UUID userId, boolean platformAdmin,
            String name, String description, JsonNode definition) {
        Membership membership = authorization.require(tenantId, userId, platformAdmin, "FORM_CREATE");
        if (membership == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Administrador global precisa de membership para criar formulários");
        }
        validator.validate(definition);
        return forms.create(tenantId, membership.id(), name.trim(), description, definition);
    }

    @Transactional(readOnly = true)
    public List<FormSummary> list(UUID tenantId, UUID userId, boolean platformAdmin) {
        authorization.require(tenantId, userId, platformAdmin, "FORM_VIEW");
        return forms.list(tenantId);
    }

    @Transactional
    public void updateDraft(UUID tenantId, UUID formId, UUID userId,
            boolean platformAdmin, JsonNode definition) {
        authorization.require(tenantId, userId, platformAdmin, "FORM_EDIT");
        validator.validate(definition);
        var draft = forms.findDraft(tenantId, formId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rascunho do formulário não encontrado"));
        forms.updateDraft(tenantId, draft.id(), definition);
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
    }
}
