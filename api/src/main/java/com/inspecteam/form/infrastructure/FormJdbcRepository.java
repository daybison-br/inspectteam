package com.inspecteam.form.infrastructure;

import com.inspecteam.form.domain.FormSummary;
import com.inspecteam.form.domain.PublishedFormDetails;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Repository
public class FormJdbcRepository {

    private final JdbcClient jdbc;
    private final ObjectMapper objectMapper;

    public FormJdbcRepository(JdbcClient jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public UUID create(UUID tenantId, UUID membershipId, String name, String description, JsonNode definition) {
        UUID formId = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO forms (id, tenant_id, name, description, created_by)
                VALUES (:id, :tenantId, :name, :description, :createdBy)
                """).param("id", formId).param("tenantId", tenantId).param("name", name)
                .param("description", description).param("createdBy", membershipId).update();
        jdbc.sql("""
                INSERT INTO form_versions (tenant_id, form_id, version_number, definition, created_by)
                VALUES (:tenantId, :formId, 1, CAST(:definition AS JSONB), :createdBy)
                """).param("tenantId", tenantId).param("formId", formId)
                .param("definition", definition.toString()).param("createdBy", membershipId).update();
        return formId;
    }

    public List<FormSummary> list(UUID tenantId) {
        return jdbc.sql("""
                SELECT f.id, f.name, f.description, f.status, f.updated_at,
                       MAX(v.version_number) FILTER (WHERE v.status = 'PUBLISHED') published_version,
                       MAX(v.version_number) FILTER (WHERE v.status = 'DRAFT') draft_version
                  FROM forms f LEFT JOIN form_versions v ON v.tenant_id = f.tenant_id AND v.form_id = f.id
                 WHERE f.tenant_id = :tenantId AND f.deleted = FALSE
                 GROUP BY f.id ORDER BY f.updated_at DESC
                """).param("tenantId", tenantId).query(this::summary).list();
    }

    public List<FormSummary> listAvailable(UUID tenantId, UUID membershipId, boolean allAccess) {
        return jdbc.sql("""
                SELECT f.id, f.name, f.description, f.status, f.updated_at,
                       v.version_number published_version, NULL::INTEGER draft_version
                  FROM forms f JOIN form_versions v ON v.tenant_id=f.tenant_id AND v.form_id=f.id
                 WHERE f.tenant_id=:tenantId AND f.deleted=FALSE AND f.status='PUBLISHED'
                   AND v.status='PUBLISHED' AND (:allAccess OR EXISTS (
                       SELECT 1 FROM membership_roles mr JOIN role_permissions rp
                         ON rp.tenant_id=mr.tenant_id AND rp.role_id=mr.role_id
                        WHERE mr.tenant_id=:tenantId AND mr.membership_id=:membershipId
                          AND rp.permission_code='FORM_USE'
                   ) OR EXISTS (
                       SELECT 1 FROM form_grants fg WHERE fg.tenant_id=:tenantId AND fg.form_id=f.id
                         AND fg.membership_id=:membershipId AND fg.permission_code='FORM_USE'
                   ) OR EXISTS (
                       SELECT 1 FROM form_grants fg JOIN membership_roles mr
                         ON mr.tenant_id=fg.tenant_id AND mr.role_id=fg.role_id
                        WHERE fg.tenant_id=:tenantId AND fg.form_id=f.id
                          AND mr.membership_id=:membershipId AND fg.permission_code='FORM_USE'
                   ))
                 ORDER BY f.updated_at DESC
                """).param("tenantId", tenantId).param("membershipId", membershipId)
                .param("allAccess", allAccess).query(this::summary).list();
    }

    public Optional<PublishedFormDetails> findPublished(UUID tenantId, UUID formId) {
        return jdbc.sql("""
                SELECT f.id, f.name, f.description, v.id version_id, v.version_number, v.definition::text definition
                  FROM forms f JOIN form_versions v ON v.tenant_id=f.tenant_id AND v.form_id=f.id
                 WHERE f.tenant_id=:tenantId AND f.id=:formId AND f.deleted=FALSE
                   AND f.status='PUBLISHED' AND v.status='PUBLISHED'
                """).param("tenantId", tenantId).param("formId", formId)
                .query((rs, row) -> new PublishedFormDetails(rs.getObject("id", UUID.class), rs.getString("name"),
                        rs.getString("description"), rs.getObject("version_id", UUID.class),
                        rs.getInt("version_number"), readJson(rs.getString("definition")))).optional();
    }

    public Optional<FormVersion> findDraft(UUID tenantId, UUID formId) {
        return jdbc.sql("""
                SELECT v.id, v.version_number, v.definition::text definition
                  FROM form_versions v JOIN forms f ON f.tenant_id=v.tenant_id AND f.id=v.form_id
                 WHERE v.tenant_id=:tenantId AND v.form_id=:formId AND v.status='DRAFT' AND f.deleted=FALSE
                """).param("tenantId", tenantId).param("formId", formId)
                .query((rs, rowNum) -> new FormVersion(rs.getObject("id", UUID.class),
                        rs.getInt("version_number"), readJson(rs.getString("definition")))).optional();
    }

    public void updateDraft(UUID tenantId, UUID versionId, JsonNode definition) {
        jdbc.sql("UPDATE form_versions SET definition = CAST(:definition AS JSONB) WHERE tenant_id = :tenantId AND id = :id")
                .param("definition", definition.toString()).param("tenantId", tenantId).param("id", versionId).update();
    }

    public void publish(UUID tenantId, UUID formId, FormVersion draft, UUID membershipId) {
        jdbc.sql("UPDATE form_versions SET status = 'RETIRED' WHERE tenant_id = :tenantId AND form_id = :formId AND status = 'PUBLISHED'")
                .param("tenantId", tenantId).param("formId", formId).update();
        jdbc.sql("UPDATE form_versions SET status = 'PUBLISHED', published_at = NOW() WHERE tenant_id = :tenantId AND id = :id")
                .param("tenantId", tenantId).param("id", draft.id()).update();
        jdbc.sql("UPDATE forms SET status = 'PUBLISHED', updated_at = NOW() WHERE tenant_id = :tenantId AND id = :formId AND deleted = FALSE")
                .param("tenantId", tenantId).param("formId", formId).update();
        jdbc.sql("""
                INSERT INTO form_versions (tenant_id, form_id, version_number, definition, created_by)
                VALUES (:tenantId, :formId, :version, CAST(:definition AS JSONB), :createdBy)
                """).param("tenantId", tenantId).param("formId", formId).param("version", draft.version() + 1)
                .param("definition", draft.definition().toString()).param("createdBy", membershipId).update();
    }

    private FormSummary summary(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new FormSummary(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("description"),
                rs.getString("status"), (Integer) rs.getObject("published_version"),
                (Integer) rs.getObject("draft_version"), rs.getTimestamp("updated_at").toInstant());
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("JSON persistido inválido", exception);
        }
    }

    public record FormVersion(UUID id, int version, JsonNode definition) {
    }
}
