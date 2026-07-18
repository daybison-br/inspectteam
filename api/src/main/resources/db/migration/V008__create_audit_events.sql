CREATE TABLE audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    actor_user_id UUID REFERENCES users(id),
    actor_membership_id UUID,
    action VARCHAR(120) NOT NULL,
    resource_type VARCHAR(80) NOT NULL,
    resource_id UUID,
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    ip_address INET,
    user_agent VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_audit_membership FOREIGN KEY (tenant_id, actor_membership_id)
        REFERENCES tenant_memberships(tenant_id, id)
);

CREATE INDEX ix_audit_tenant_time ON audit_events(tenant_id, occurred_at DESC);
CREATE INDEX ix_audit_resource ON audit_events(tenant_id, resource_type, resource_id);
