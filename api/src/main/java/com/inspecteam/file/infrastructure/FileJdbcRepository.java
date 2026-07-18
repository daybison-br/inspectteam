package com.inspecteam.file.infrastructure;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class FileJdbcRepository {

    private final JdbcClient jdbc;

    public FileJdbcRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public UUID create(UUID tenantId, UUID submissionId, String fieldId, String objectKey,
            String originalName, String contentType, long size, String checksum, UUID membershipId) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO files (id, tenant_id, submission_id, field_id, object_key, original_name,
                    content_type, size_bytes, checksum_sha256, created_by)
                VALUES (:id, :tenantId, :submissionId, :fieldId, :objectKey, :originalName,
                    :contentType, :size, :checksum, :createdBy)
                """).param("id", id).param("tenantId", tenantId).param("submissionId", submissionId)
                .param("fieldId", fieldId).param("objectKey", objectKey).param("originalName", originalName)
                .param("contentType", contentType).param("size", size).param("checksum", checksum)
                .param("createdBy", membershipId).update();
        return id;
    }

    public FileRecord find(UUID tenantId, UUID fileId) {
        return jdbc.sql("""
                SELECT id, object_key, size_bytes, status FROM files
                 WHERE tenant_id = :tenantId AND id = :fileId
                """).param("tenantId", tenantId).param("fileId", fileId)
                .query((rs, rowNum) -> new FileRecord(rs.getObject("id", UUID.class),
                        rs.getString("object_key"), rs.getLong("size_bytes"), rs.getString("status"))).single();
    }

    public void complete(UUID tenantId, UUID fileId) {
        jdbc.sql("""
                UPDATE files SET status = 'AVAILABLE', completed_at = NOW()
                 WHERE tenant_id = :tenantId AND id = :fileId AND status = 'PENDING'
                """).param("tenantId", tenantId).param("fileId", fileId).update();
    }

    public record FileRecord(UUID id, String objectKey, long expectedSize, String status) { }
}
