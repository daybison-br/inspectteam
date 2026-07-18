ALTER TABLE tenant_memberships DROP CONSTRAINT ck_membership_type;
ALTER TABLE tenant_memberships ADD CONSTRAINT ck_membership_type
    CHECK (membership_type IN ('OWNER', 'MEMBER', 'PLATFORM_ADMIN'));

ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX ix_users_status ON users(status);
CREATE INDEX ix_tenants_status ON tenants(status);
