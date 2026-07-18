CREATE TABLE files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    submission_id UUID,
    field_id VARCHAR(120),
    object_key VARCHAR(500) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(160) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT uk_files_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_files_object_key UNIQUE (object_key),
    CONSTRAINT fk_files_submission FOREIGN KEY (tenant_id, submission_id)
        REFERENCES submissions(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_files_creator FOREIGN KEY (tenant_id, created_by)
        REFERENCES tenant_memberships(tenant_id, id),
    CONSTRAINT ck_files_status CHECK (status IN ('PENDING', 'AVAILABLE', 'FAILED', 'DELETED')),
    CONSTRAINT ck_files_size CHECK (size_bytes >= 0)
);

CREATE INDEX ix_files_submission ON files(tenant_id, submission_id);
