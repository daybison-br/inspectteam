package com.inspecteam.audit.application;

import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class AuditService {

    private final JdbcClient jdbc;
    private final ObjectMapper objectMapper;

    public AuditService(JdbcClient jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void record(UUID tenantId, UUID actorUserId, UUID actorMembershipId,
            String action, String resourceType, UUID resourceId, Map<String, ?> details) {
        try {
            jdbc.sql("""
                    INSERT INTO audit_events
                        (tenant_id, actor_user_id, actor_membership_id, action, resource_type, resource_id, details)
                    VALUES (:tenantId, :actorUserId, :actorMembershipId, :action, :resourceType,
                        :resourceId, CAST(:details AS JSONB))
                    """).param("tenantId", tenantId).param("actorUserId", actorUserId)
                    .param("actorMembershipId", actorMembershipId).param("action", action)
                    .param("resourceType", resourceType).param("resourceId", resourceId)
                    .param("details", objectMapper.writeValueAsString(details)).update();
        } catch (JacksonException exception) {
            throw new IllegalStateException("Falha ao serializar evento de auditoria", exception);
        }
    }
}
