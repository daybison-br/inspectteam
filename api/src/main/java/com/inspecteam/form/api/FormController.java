package com.inspecteam.form.api;

import tools.jackson.databind.JsonNode;
import com.inspecteam.form.application.FormService;
import com.inspecteam.form.domain.FormSummary;
import com.inspecteam.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/forms")
public class FormController {

    private final FormService service;

    public FormController(FormService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> create(@PathVariable UUID tenantId, @Valid @RequestBody CreateFormRequest request,
            Authentication authentication) {
        UUID id = service.create(tenantId, CurrentUser.id(authentication), CurrentUser.isPlatformAdmin(authentication),
                request.name(), request.description(), request.definition());
        return Map.of("id", id);
    }

    @GetMapping
    List<FormSummary> list(@PathVariable UUID tenantId, Authentication authentication) {
        return service.list(tenantId, CurrentUser.id(authentication), CurrentUser.isPlatformAdmin(authentication));
    }

    @PatchMapping("/{formId}/draft")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateDraft(@PathVariable UUID tenantId, @PathVariable UUID formId,
            @Valid @RequestBody UpdateDraftRequest request, Authentication authentication) {
        service.updateDraft(tenantId, formId, CurrentUser.id(authentication),
                CurrentUser.isPlatformAdmin(authentication), request.definition());
    }

    @PostMapping("/{formId}/publish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void publish(@PathVariable UUID tenantId, @PathVariable UUID formId, Authentication authentication) {
        service.publish(tenantId, formId, CurrentUser.id(authentication), CurrentUser.isPlatformAdmin(authentication));
    }

    record CreateFormRequest(@NotBlank @Size(max = 180) String name,
            @Size(max = 4000) String description, @NotNull JsonNode definition) {
    }

    record UpdateDraftRequest(@NotNull JsonNode definition) {
    }
}
