package com.inspecteam.audit.api;

import com.inspecteam.permission.application.TenantAuthorizationService;
import com.inspecteam.shared.security.CurrentUser;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/audit")
public class AuditController {

    private final JdbcClient jdbc;
    private final TenantAuthorizationService authorization;

    public AuditController(JdbcClient jdbc, TenantAuthorizationService authorization) {
        this.jdbc = jdbc;
        this.authorization = authorization;
    }

    @GetMapping
    @Transactional(readOnly = true)
    List<AuditEventView> list(@PathVariable UUID tenantId,
            @RequestParam(defaultValue = "100") int limit, Authentication authentication) {
        authorization.require(tenantId, CurrentUser.id(authentication),
                CurrentUser.isPlatformAdmin(authentication), "TENANT_MANAGE");
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return jdbc.sql("""
                SELECT id, actor_user_id, action, resource_type, resource_id, details::text, occurred_at
                  FROM audit_events WHERE tenant_id = :tenantId
                 ORDER BY occurred_at DESC LIMIT :limit
                """).param("tenantId", tenantId).param("limit", safeLimit)
                .query((rs, rowNum) -> new AuditEventView(rs.getObject("id", UUID.class),
                        rs.getObject("actor_user_id", UUID.class), rs.getString("action"),
                        rs.getString("resource_type"), rs.getObject("resource_id", UUID.class),
                        rs.getString("details"), rs.getTimestamp("occurred_at").toInstant())).list();
    }

    record AuditEventView(UUID id, UUID actorUserId, String action, String resourceType,
            UUID resourceId, String details, Instant occurredAt) { }
}
