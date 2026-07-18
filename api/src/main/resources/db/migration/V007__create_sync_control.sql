CREATE TABLE devices (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    membership_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    platform VARCHAR(30) NOT NULL,
    last_seen_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_devices_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_devices_member FOREIGN KEY (tenant_id, membership_id)
        REFERENCES tenant_memberships(tenant_id, id) ON DELETE CASCADE
);

CREATE TABLE sync_mutations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    device_id UUID NOT NULL,
    mutation_id UUID NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    entity_id UUID NOT NULL,
    operation VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'APPLIED',
    result JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_sync_mutation UNIQUE (tenant_id, device_id, mutation_id),
    CONSTRAINT fk_sync_device FOREIGN KEY (tenant_id, device_id)
        REFERENCES devices(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_sync_operation CHECK (operation IN ('CREATE', 'UPDATE', 'COMPLETE', 'DELETE')),
    CONSTRAINT ck_sync_status CHECK (status IN ('APPLIED', 'REJECTED', 'CONFLICT'))
);

CREATE TABLE sync_tombstones (
    sequence_id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    entity_type VARCHAR(60) NOT NULL,
    entity_id UUID NOT NULL,
    deleted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_sync_tombstone UNIQUE (tenant_id, entity_type, entity_id)
);

CREATE INDEX ix_sync_mutations_device ON sync_mutations(tenant_id, device_id, created_at);
CREATE INDEX ix_sync_tombstones_cursor ON sync_tombstones(tenant_id, sequence_id);
