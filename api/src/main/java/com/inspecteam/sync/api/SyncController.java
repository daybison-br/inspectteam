package com.inspecteam.sync.api;

import com.inspecteam.shared.security.CurrentUser;
import com.inspecteam.sync.application.SyncService;
import com.inspecteam.sync.application.SyncService.Mutation;
import com.inspecteam.sync.application.SyncService.MutationResult;
import com.inspecteam.sync.application.SyncService.PullResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/sync")
public class SyncController {

    private final SyncService service;

    public SyncController(SyncService service) {
        this.service = service;
    }

    @PostMapping("/devices")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void registerDevice(@PathVariable UUID tenantId, @Valid @RequestBody DeviceRequest request,
            Authentication authentication) {
        service.registerDevice(tenantId, CurrentUser.id(authentication), CurrentUser.isPlatformAdmin(authentication),
                request.deviceId(), request.name(), request.platform());
    }

    @GetMapping("/pull")
    PullResult pull(@PathVariable UUID tenantId, @RequestParam(defaultValue = "0") @PositiveOrZero long cursor,
            @RequestParam(required = false) UUID deviceId, Authentication authentication) {
        return service.pull(tenantId, CurrentUser.id(authentication),
                CurrentUser.isPlatformAdmin(authentication), cursor, deviceId);
    }

    @PostMapping("/push")
    List<MutationResult> push(@PathVariable UUID tenantId, @Valid @RequestBody PushRequest request,
            Authentication authentication) {
        List<Mutation> mutations = request.mutations().stream().map(item -> new Mutation(
                item.mutationId(), item.operation(), item.submissionId(), item.formId(),
                item.formVersionId(), item.revision(), item.answers(), item.clientCreatedAt())).toList();
        return service.push(tenantId, CurrentUser.id(authentication), CurrentUser.isPlatformAdmin(authentication),
                request.deviceId(), mutations);
    }

    record DeviceRequest(@NotNull UUID deviceId, @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 30) String platform) { }

    record PushRequest(@NotNull UUID deviceId, @NotEmpty @Size(max = 100) List<@Valid MutationRequest> mutations) { }

    record MutationRequest(@NotNull UUID mutationId,
            @NotBlank @Pattern(regexp = "CREATE|UPDATE|COMPLETE") String operation,
            @NotNull UUID submissionId, @NotNull UUID formId, UUID formVersionId,
            @PositiveOrZero int revision, @NotNull JsonNode answers, Instant clientCreatedAt) { }
}
