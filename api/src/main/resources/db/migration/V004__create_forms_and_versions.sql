CREATE TABLE forms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(180) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_forms_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_forms_creator FOREIGN KEY (tenant_id, created_by)
        REFERENCES tenant_memberships(tenant_id, id),
    CONSTRAINT ck_forms_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE TABLE form_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    form_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    definition JSONB NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ,
    CONSTRAINT uk_form_versions_number UNIQUE (form_id, version_number),
    CONSTRAINT uk_form_versions_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_form_versions_form FOREIGN KEY (tenant_id, form_id)
        REFERENCES forms(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_form_versions_creator FOREIGN KEY (tenant_id, created_by)
        REFERENCES tenant_memberships(tenant_id, id),
    CONSTRAINT ck_form_versions_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'RETIRED')),
    CONSTRAINT ck_form_definition_object CHECK (jsonb_typeof(definition) = 'object')
);

CREATE UNIQUE INDEX uk_one_draft_per_form ON form_versions(form_id) WHERE status = 'DRAFT';
CREATE INDEX ix_forms_tenant_status ON forms(tenant_id, status);
CREATE INDEX ix_form_versions_tenant_form ON form_versions(tenant_id, form_id);

CREATE TABLE form_grants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    form_id UUID NOT NULL,
    membership_id UUID,
    role_id UUID,
    permission_code VARCHAR(80) NOT NULL REFERENCES permissions(code),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_form_grants_form FOREIGN KEY (tenant_id, form_id)
        REFERENCES forms(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_form_grants_membership FOREIGN KEY (tenant_id, membership_id)
        REFERENCES tenant_memberships(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_form_grants_role FOREIGN KEY (tenant_id, role_id)
        REFERENCES roles(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_form_grant_principal CHECK ((membership_id IS NULL) <> (role_id IS NULL))
);

CREATE UNIQUE INDEX uk_form_grant_membership ON form_grants(form_id, membership_id, permission_code)
    WHERE membership_id IS NOT NULL;
CREATE UNIQUE INDEX uk_form_grant_role ON form_grants(form_id, role_id, permission_code)
    WHERE role_id IS NOT NULL;
