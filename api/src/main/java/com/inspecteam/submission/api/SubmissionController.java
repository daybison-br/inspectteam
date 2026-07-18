package com.inspecteam.submission.api;

import com.inspecteam.shared.security.CurrentUser;
import com.inspecteam.submission.application.SubmissionService;
import com.inspecteam.submission.domain.SubmissionSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/submissions")
public class SubmissionController {

    private final SubmissionService service;

    public SubmissionController(SubmissionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    void create(@PathVariable UUID tenantId, @Valid @RequestBody CreateSubmissionRequest request,
            Authentication authentication) {
        service.create(tenantId, CurrentUser.id(authentication), CurrentUser.isPlatformAdmin(authentication),
                request.id(), request.formId(), request.formVersionId(), request.answers(), request.clientCreatedAt());
    }

    @PatchMapping("/{submissionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void update(@PathVariable UUID tenantId, @PathVariable UUID submissionId,
            @Valid @RequestBody UpdateSubmissionRequest request, Authentication authentication) {
        service.updateDraft(tenantId, CurrentUser.id(authentication), CurrentUser.isPlatformAdmin(authentication),
                request.formId(), submissionId, request.revision(), request.answers());
    }

    @PostMapping("/{submissionId}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void complete(@PathVariable UUID tenantId, @PathVariable UUID submissionId,
            @Valid @RequestBody UpdateSubmissionRequest request, Authentication authentication) {
        service.complete(tenantId, CurrentUser.id(authentication), CurrentUser.isPlatformAdmin(authentication),
                request.formId(), submissionId, request.revision(), request.answers());
    }

    @GetMapping
    List<SubmissionSummary> list(@PathVariable UUID tenantId, @RequestParam UUID formId,
            Authentication authentication) {
        return service.list(tenantId, CurrentUser.id(authentication),
                CurrentUser.isPlatformAdmin(authentication), formId);
    }

    record CreateSubmissionRequest(@NotNull UUID id, @NotNull UUID formId, @NotNull UUID formVersionId,
            @NotNull JsonNode answers, Instant clientCreatedAt) {
    }

    record UpdateSubmissionRequest(@NotNull UUID formId, @PositiveOrZero int revision, @NotNull JsonNode answers) {
    }
}
