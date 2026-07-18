package com.inspecteam.permission.api;

import com.inspecteam.permission.application.PermissionService;
import com.inspecteam.permission.infrastructure.PermissionJdbcRepository.RoleView;
import com.inspecteam.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/permissions")
public class PermissionController {

    private final PermissionService service;

    public PermissionController(PermissionService service) {
        this.service = service;
    }

    @GetMapping("/roles")
    List<RoleView> listRoles(@PathVariable UUID tenantId, Authentication auth) {
        return service.listRoles(tenantId, CurrentUser.id(auth), CurrentUser.isPlatformAdmin(auth));
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> createRole(@PathVariable UUID tenantId, @Valid @RequestBody CreateRoleRequest request,
            Authentication auth) {
        UUID id = service.createRole(tenantId, CurrentUser.id(auth), CurrentUser.isPlatformAdmin(auth),
                request.name(), request.description());
        return Map.of("id", id);
    }

    @PutMapping("/roles/{roleId}/permissions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void setPermissions(@PathVariable UUID tenantId, @PathVariable UUID roleId,
            @Valid @RequestBody SetPermissionsRequest request, Authentication auth) {
        service.setPermissions(tenantId, CurrentUser.id(auth), CurrentUser.isPlatformAdmin(auth),
                roleId, request.permissions());
    }

    @PostMapping("/roles/{roleId}/members/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void assignRole(@PathVariable UUID tenantId, @PathVariable UUID roleId,
            @PathVariable UUID membershipId, Authentication auth) {
        service.assignRole(tenantId, CurrentUser.id(auth), CurrentUser.isPlatformAdmin(auth), membershipId, roleId);
    }

    @PostMapping("/forms/{formId}/grants")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void grantForm(@PathVariable UUID tenantId, @PathVariable UUID formId,
            @Valid @RequestBody FormGrantRequest request, Authentication auth) {
        service.grantForm(tenantId, CurrentUser.id(auth), CurrentUser.isPlatformAdmin(auth), formId,
                request.membershipId(), request.roleId(), request.permission());
    }

    record CreateRoleRequest(@NotBlank @Size(max = 100) String name, @Size(max = 240) String description) {
    }

    record SetPermissionsRequest(@NotEmpty List<@NotBlank String> permissions) {
    }

    record FormGrantRequest(UUID membershipId, UUID roleId, @NotNull String permission) {
    }
}
