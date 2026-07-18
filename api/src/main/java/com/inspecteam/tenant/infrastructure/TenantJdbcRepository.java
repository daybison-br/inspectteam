package com.inspecteam.tenant.infrastructure;

import com.inspecteam.tenant.domain.TenantSummary;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TenantJdbcRepository {

    private final JdbcClient jdbc;

    public TenantJdbcRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<TenantSummary> findForUser(UUID userId) {
        return jdbc.sql("""
                SELECT t.id tenant_id, t.name, t.slug, m.id membership_id,
                       m.membership_type, m.status membership_status
                  FROM tenant_memberships m
                  JOIN tenants t ON t.id = m.tenant_id
                 WHERE m.user_id = :userId AND t.status <> 'ARCHIVED'
                 ORDER BY t.name
                """)
                .param("userId", userId)
                .query((rs, rowNum) -> new TenantSummary(
                        rs.getObject("tenant_id", UUID.class),
                        rs.getString("name"),
                        rs.getString("slug"),
                        rs.getObject("membership_id", UUID.class),
                        rs.getString("membership_type"),
                        rs.getString("membership_status")))
                .list();
    }

    public Optional<Membership> findActiveMembership(UUID tenantId, UUID userId) {
        return jdbc.sql("""
                SELECT id, tenant_id, user_id, membership_type
                  FROM tenant_memberships
                 WHERE tenant_id = :tenantId AND user_id = :userId AND status = 'ACTIVE'
                """)
                .param("tenantId", tenantId)
                .param("userId", userId)
                .query((rs, rowNum) -> new Membership(
                        rs.getObject("id", UUID.class),
                        rs.getObject("tenant_id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getString("membership_type")))
                .optional();
    }

    public void activateRls(UUID tenantId) {
        jdbc.sql("SELECT set_config('app.current_tenant_id', :tenantId, TRUE)")
                .param("tenantId", tenantId.toString())
                .query(String.class)
                .single();
    }

    public boolean hasPermission(UUID tenantId, UUID membershipId, String permission) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1
                      FROM membership_roles mr
                      JOIN role_permissions rp ON rp.tenant_id = mr.tenant_id AND rp.role_id = mr.role_id
                     WHERE mr.tenant_id = :tenantId
                       AND mr.membership_id = :membershipId
                       AND rp.permission_code = :permission
                )
                """)
                .param("tenantId", tenantId)
                .param("membershipId", membershipId)
                .param("permission", permission)
                .query(Boolean.class)
                .single();
    }

    public record Membership(UUID id, UUID tenantId, UUID userId, String type) {
        public boolean owner() {
            return "OWNER".equals(type);
        }
    }
}
