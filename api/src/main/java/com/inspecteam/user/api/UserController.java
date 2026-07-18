package com.inspecteam.user.api;

import com.inspecteam.shared.security.CurrentUser;
import com.inspecteam.user.application.UserService;
import com.inspecteam.user.domain.TenantUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    List<TenantUser> list(@PathVariable UUID tenantId, Authentication authentication) {
        return service.list(tenantId, CurrentUser.id(authentication), CurrentUser.isPlatformAdmin(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> create(@PathVariable UUID tenantId, @Valid @RequestBody CreateUserRequest request,
            Authentication authentication) {
        UUID membershipId = service.create(tenantId, CurrentUser.id(authentication),
                CurrentUser.isPlatformAdmin(authentication), request.email(), request.displayName(),
                request.temporaryPassword());
        return Map.of("membershipId", membershipId);
    }

    @DeleteMapping("/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void suspend(@PathVariable UUID tenantId, @PathVariable UUID membershipId, Authentication authentication) {
        service.suspend(tenantId, CurrentUser.id(authentication), CurrentUser.isPlatformAdmin(authentication), membershipId);
    }

    record CreateUserRequest(@NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 160) String displayName,
            @NotBlank @Size(min = 10, max = 128) String temporaryPassword) {
    }
}
