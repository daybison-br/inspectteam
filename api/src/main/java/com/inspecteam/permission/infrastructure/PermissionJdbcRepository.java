package com.inspecteam.permission.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PermissionJdbcRepository {

    private final JdbcClient jdbc;

    public PermissionJdbcRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public UUID createRole(UUID tenantId, String name, String description) {
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO roles (id, tenant_id, name, description) VALUES (:id, :tenantId, :name, :description)")
                .param("id", id).param("tenantId", tenantId).param("name", name)
                .param("description", description).update();
        return id;
    }

    public List<RoleView> listRoles(UUID tenantId) {
        return jdbc.sql("""
                SELECT r.id, r.name, r.description,
                       COALESCE(array_agg(rp.permission_code) FILTER (WHERE rp.permission_code IS NOT NULL), '{}') permissions
                  FROM roles r LEFT JOIN role_permissions rp ON rp.tenant_id = r.tenant_id AND rp.role_id = r.id
                 WHERE r.tenant_id = :tenantId GROUP BY r.id ORDER BY r.name
                """).param("tenantId", tenantId)
                .query((rs, rowNum) -> new RoleView(rs.getObject("id", UUID.class), rs.getString("name"),
                        rs.getString("description"), List.of((String[]) rs.getArray("permissions").getArray()))).list();
    }

    public void replaceRolePermissions(UUID tenantId, UUID roleId, List<String> permissions) {
        jdbc.sql("DELETE FROM role_permissions WHERE tenant_id = :tenantId AND role_id = :roleId")
                .param("tenantId", tenantId).param("roleId", roleId).update();
        for (String permission : permissions) {
            jdbc.sql("""
                    INSERT INTO role_permissions (tenant_id, role_id, permission_code)
                    VALUES (:tenantId, :roleId, :permission)
                    """).param("tenantId", tenantId).param("roleId", roleId)
                    .param("permission", permission).update();
        }
    }

    public void assignRole(UUID tenantId, UUID membershipId, UUID roleId) {
        jdbc.sql("""
                INSERT INTO membership_roles (tenant_id, membership_id, role_id)
                VALUES (:tenantId, :membershipId, :roleId) ON CONFLICT DO NOTHING
                """).param("tenantId", tenantId).param("membershipId", membershipId).param("roleId", roleId).update();
    }

    public void grantFormToMembership(UUID tenantId, UUID formId, UUID membershipId, String permission) {
        jdbc.sql("""
                INSERT INTO form_grants (tenant_id, form_id, membership_id, permission_code)
                VALUES (:tenantId, :formId, :membershipId, :permission) ON CONFLICT DO NOTHING
                """).param("tenantId", tenantId).param("formId", formId)
                .param("membershipId", membershipId).param("permission", permission).update();
    }

    public void grantFormToRole(UUID tenantId, UUID formId, UUID roleId, String permission) {
        jdbc.sql("""
                INSERT INTO form_grants (tenant_id, form_id, role_id, permission_code)
                VALUES (:tenantId, :formId, :roleId, :permission) ON CONFLICT DO NOTHING
                """).param("tenantId", tenantId).param("formId", formId)
                .param("roleId", roleId).param("permission", permission).update();
    }

    public record RoleView(UUID id, String name, String description, List<String> permissions) {
    }
}
