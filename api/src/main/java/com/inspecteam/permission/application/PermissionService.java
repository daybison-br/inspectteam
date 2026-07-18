package com.inspecteam.permission.application;

import com.inspecteam.audit.application.AuditService;
import com.inspecteam.permission.infrastructure.PermissionJdbcRepository;
import com.inspecteam.permission.infrastructure.PermissionJdbcRepository.RoleView;
import com.inspecteam.shared.exception.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionService {

    private final PermissionJdbcRepository permissions;
    private final TenantAuthorizationService authorization;
    private final AuditService audit;

    public PermissionService(PermissionJdbcRepository permissions, TenantAuthorizationService authorization,
            AuditService audit) {
        this.permissions = permissions;
        this.authorization = authorization;
        this.audit = audit;
    }

    @Transactional
    public UUID createRole(UUID tenantId, UUID userId, boolean admin, String name, String description) {
        authorization.require(tenantId, userId, admin, "TENANT_MANAGE");
        UUID roleId = permissions.createRole(tenantId, name.trim(), description);
        audit.record(tenantId, userId, null, "ROLE_CREATED", "ROLE", roleId,
                java.util.Map.of("name", name.trim()));
        return roleId;
    }

    @Transactional(readOnly = true)
    public List<RoleView> listRoles(UUID tenantId, UUID userId, boolean admin) {
        authorization.require(tenantId, userId, admin, "TENANT_MANAGE");
        return permissions.listRoles(tenantId);
    }

    @Transactional
    public void setPermissions(UUID tenantId, UUID userId, boolean admin, UUID roleId, List<String> values) {
        authorization.require(tenantId, userId, admin, "TENANT_MANAGE");
        permissions.replaceRolePermissions(tenantId, roleId, values.stream().distinct().toList());
        audit.record(tenantId, userId, null, "ROLE_PERMISSIONS_CHANGED", "ROLE", roleId,
                java.util.Map.of("permissions", values));
    }

    @Transactional
    public void assignRole(UUID tenantId, UUID userId, boolean admin, UUID membershipId, UUID roleId) {
        authorization.require(tenantId, userId, admin, "TENANT_MANAGE");
        permissions.assignRole(tenantId, membershipId, roleId);
        audit.record(tenantId, userId, null, "ROLE_ASSIGNED", "MEMBERSHIP", membershipId,
                java.util.Map.of("roleId", roleId));
    }

    @Transactional
    public void grantForm(UUID tenantId, UUID userId, boolean admin, UUID formId,
            UUID membershipId, UUID roleId, String permission) {
        authorization.require(tenantId, userId, admin, "FORM_MANAGE_ACCESS");
        if ((membershipId == null) == (roleId == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Informe exatamente um membershipId ou roleId");
        }
        if (membershipId != null) {
            permissions.grantFormToMembership(tenantId, formId, membershipId, permission);
        } else {
            permissions.grantFormToRole(tenantId, formId, roleId, permission);
        }
        audit.record(tenantId, userId, null, "FORM_ACCESS_GRANTED", "FORM", formId,
                java.util.Map.of("permission", permission));
    }
}
