package com.inspecteam.submission.infrastructure;

import com.inspecteam.submission.domain.SubmissionSummary;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.sql.Types;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;

@Repository
public class SubmissionJdbcRepository {

    private final JdbcClient jdbc;

    public SubmissionJdbcRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean create(UUID id, UUID tenantId, UUID formId, UUID formVersionId,
            UUID membershipId, JsonNode answers, Instant clientCreatedAt) {
        return jdbc.sql("""
                INSERT INTO submissions
                    (id, tenant_id, form_id, form_version_id, submitted_by, answers, client_created_at)
                SELECT :id, :tenantId, :formId, v.id, :membershipId, CAST(:answers AS JSONB), :clientCreatedAt
                  FROM form_versions v
                 WHERE v.tenant_id = :tenantId AND v.form_id = :formId
                   AND v.id = :formVersionId AND v.status = 'PUBLISHED'
                ON CONFLICT (id) DO NOTHING
                """).param("id", id).param("tenantId", tenantId).param("formId", formId)
                .param("formVersionId", formVersionId).param("membershipId", membershipId)
                .param("answers", answers.toString())
                .param("clientCreatedAt", clientCreatedAt == null
                        ? null
                        : OffsetDateTime.ofInstant(clientCreatedAt, ZoneOffset.UTC),
                        Types.TIMESTAMP_WITH_TIMEZONE)
                .update() == 1;
    }

    public boolean exists(UUID tenantId, UUID id, UUID membershipId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM submissions WHERE tenant_id = :tenantId AND id = :id AND submitted_by = :membershipId)")
                .param("tenantId", tenantId).param("id", id).param("membershipId", membershipId)
                .query(Boolean.class).single();
    }

    public boolean updateDraft(UUID tenantId, UUID id, UUID membershipId, int expectedRevision, JsonNode answers) {
        return jdbc.sql("""
                UPDATE submissions
                   SET answers = CAST(:answers AS JSONB), revision = revision + 1, updated_at = NOW()
                 WHERE tenant_id = :tenantId AND id = :id
                   AND submitted_by = :membershipId AND revision = :revision AND status = 'DRAFT'
                """).param("answers", answers.toString()).param("tenantId", tenantId)
                .param("id", id).param("membershipId", membershipId)
                .param("revision", expectedRevision).update() == 1;
    }

    public boolean complete(UUID tenantId, UUID id, UUID membershipId, int expectedRevision, JsonNode answers) {
        return jdbc.sql("""
                UPDATE submissions
                   SET answers = CAST(:answers AS JSONB), revision = revision + 1,
                       status = 'COMPLETED', submitted_at = NOW(), updated_at = NOW()
                 WHERE tenant_id = :tenantId AND id = :id
                   AND submitted_by = :membershipId AND revision = :revision AND status = 'DRAFT'
                """).param("answers", answers.toString()).param("tenantId", tenantId)
                .param("id", id).param("membershipId", membershipId)
                .param("revision", expectedRevision).update() == 1;
    }

    public List<SubmissionSummary> list(UUID tenantId, UUID formId) {
        return jdbc.sql("""
                SELECT id, form_id, form_version_id, submitted_by, status, revision, submitted_at, updated_at
                  FROM submissions
                 WHERE tenant_id = :tenantId AND form_id = :formId
                 ORDER BY created_at DESC
                """).param("tenantId", tenantId).param("formId", formId)
                .query((rs, rowNum) -> new SubmissionSummary(
                        rs.getObject("id", UUID.class), rs.getObject("form_id", UUID.class),
                        rs.getObject("form_version_id", UUID.class), rs.getObject("submitted_by", UUID.class),
                        rs.getString("status"), rs.getInt("revision"),
                        rs.getTimestamp("submitted_at") == null ? null : rs.getTimestamp("submitted_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant())).list();
    }
}
