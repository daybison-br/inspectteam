package com.inspecteam.sync.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Repository
public class SyncJdbcRepository {

    private final JdbcClient jdbc;
    private final ObjectMapper objectMapper;

    public SyncJdbcRepository(JdbcClient jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void registerDevice(UUID tenantId, UUID membershipId, UUID deviceId, String name, String platform) {
        jdbc.sql("""
                INSERT INTO devices (id, tenant_id, membership_id, name, platform, last_seen_at)
                VALUES (:id, :tenantId, :membershipId, :name, :platform, NOW())
                ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, platform = EXCLUDED.platform,
                    last_seen_at = NOW(), revoked_at = NULL
                """).param("id", deviceId).param("tenantId", tenantId).param("membershipId", membershipId)
                .param("name", name).param("platform", platform).update();
    }

    public boolean mutationExists(UUID tenantId, UUID deviceId, UUID mutationId) {
        return jdbc.sql("""
                SELECT EXISTS (SELECT 1 FROM sync_mutations
                 WHERE tenant_id = :tenantId AND device_id = :deviceId AND mutation_id = :mutationId)
                """).param("tenantId", tenantId).param("deviceId", deviceId).param("mutationId", mutationId)
                .query(Boolean.class).single();
    }

    public boolean deviceBelongsTo(UUID tenantId, UUID deviceId, UUID membershipId) {
        return jdbc.sql("""
                SELECT EXISTS (SELECT 1 FROM devices
                 WHERE tenant_id = :tenantId AND id = :deviceId
                   AND membership_id = :membershipId AND revoked_at IS NULL)
                """).param("tenantId", tenantId).param("deviceId", deviceId)
                .param("membershipId", membershipId).query(Boolean.class).single();
    }

    public void recordMutation(UUID tenantId, UUID deviceId, UUID mutationId,
            UUID entityId, String operation) {
        jdbc.sql("""
                INSERT INTO sync_mutations
                    (tenant_id, device_id, mutation_id, entity_type, entity_id, operation, status)
                VALUES (:tenantId, :deviceId, :mutationId, 'SUBMISSION', :entityId, :operation, 'APPLIED')
                """).param("tenantId", tenantId).param("deviceId", deviceId).param("mutationId", mutationId)
                .param("entityId", entityId).param("operation", operation).update();
    }

    public List<PublishedForm> findPublishedForms(UUID tenantId, UUID membershipId, boolean owner) {
        return jdbc.sql("""
                SELECT f.id form_id, f.name, v.id version_id, v.version_number,
                       v.definition::text definition, v.published_at
                  FROM forms f JOIN form_versions v ON v.tenant_id = f.tenant_id AND v.form_id = f.id
                 WHERE f.tenant_id = :tenantId AND f.deleted = FALSE AND v.status = 'PUBLISHED'
                   AND (:owner OR EXISTS (
                       SELECT 1 FROM membership_roles mr
                       JOIN role_permissions rp ON rp.tenant_id = mr.tenant_id AND rp.role_id = mr.role_id
                       WHERE mr.tenant_id = :tenantId AND mr.membership_id = :membershipId
                         AND rp.permission_code = 'FORM_USE'
                   ) OR EXISTS (
                       SELECT 1 FROM form_grants fg
                       WHERE fg.tenant_id = :tenantId AND fg.form_id = f.id
                         AND fg.membership_id = :membershipId AND fg.permission_code = 'FORM_USE'
                   ) OR EXISTS (
                       SELECT 1 FROM form_grants fg
                       JOIN membership_roles mr ON mr.tenant_id = fg.tenant_id AND mr.role_id = fg.role_id
                       WHERE fg.tenant_id = :tenantId AND fg.form_id = f.id
                         AND mr.membership_id = :membershipId AND fg.permission_code = 'FORM_USE'
                   ))
                 ORDER BY f.name
                """).param("tenantId", tenantId).param("membershipId", membershipId).param("owner", owner)
                .query((rs, rowNum) -> new PublishedForm(
                        rs.getObject("form_id", UUID.class), rs.getString("name"),
                        rs.getObject("version_id", UUID.class), rs.getInt("version_number"),
                        readJson(rs.getString("definition")), rs.getTimestamp("published_at").toInstant())).list();
    }

    public List<Tombstone> findTombstones(UUID tenantId, long cursor) {
        return jdbc.sql("""
                SELECT sequence_id, entity_type, entity_id, deleted_at FROM sync_tombstones
                 WHERE tenant_id = :tenantId AND sequence_id > :cursor ORDER BY sequence_id LIMIT 1000
                """).param("tenantId", tenantId).param("cursor", cursor)
                .query((rs, rowNum) -> new Tombstone(rs.getLong("sequence_id"), rs.getString("entity_type"),
                        rs.getObject("entity_id", UUID.class), rs.getTimestamp("deleted_at").toInstant())).list();
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record PublishedForm(UUID formId, String name, UUID versionId, int version,
            JsonNode definition, Instant publishedAt) { }
    public record Tombstone(long cursor, String entityType, UUID entityId, Instant deletedAt) { }
}
