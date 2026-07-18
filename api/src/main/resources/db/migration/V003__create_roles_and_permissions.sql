CREATE TABLE permissions (
    code VARCHAR(80) PRIMARY KEY,
    description VARCHAR(240) NOT NULL
);

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(240),
    system_role BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_roles_tenant_name UNIQUE (tenant_id, name),
    CONSTRAINT uk_roles_tenant_id UNIQUE (tenant_id, id)
);

CREATE TABLE role_permissions (
    tenant_id UUID NOT NULL,
    role_id UUID NOT NULL,
    permission_code VARCHAR(80) NOT NULL REFERENCES permissions(code),
    PRIMARY KEY (role_id, permission_code),
    FOREIGN KEY (tenant_id, role_id) REFERENCES roles(tenant_id, id) ON DELETE CASCADE
);

CREATE TABLE membership_roles (
    tenant_id UUID NOT NULL,
    membership_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (membership_id, role_id),
    FOREIGN KEY (tenant_id, membership_id) REFERENCES tenant_memberships(tenant_id, id) ON DELETE CASCADE,
    FOREIGN KEY (tenant_id, role_id) REFERENCES roles(tenant_id, id) ON DELETE CASCADE
);

INSERT INTO permissions (code, description) VALUES
    ('TENANT_MANAGE', 'Gerenciar configurações do tenant'),
    ('USER_VIEW', 'Visualizar usuários'),
    ('USER_CREATE', 'Convidar e criar usuários'),
    ('USER_UPDATE', 'Atualizar usuários'),
    ('USER_SUSPEND', 'Suspender usuários'),
    ('FORM_VIEW', 'Visualizar formulários'),
    ('FORM_CREATE', 'Criar formulários'),
    ('FORM_EDIT', 'Editar rascunhos de formulários'),
    ('FORM_PUBLISH', 'Publicar formulários'),
    ('FORM_ARCHIVE', 'Arquivar formulários'),
    ('FORM_USE', 'Preencher formulários'),
    ('FORM_MANAGE_ACCESS', 'Gerenciar acesso a formulários'),
    ('SUBMISSION_VIEW', 'Visualizar submissões'),
    ('SUBMISSION_CREATE', 'Criar submissões'),
    ('SUBMISSION_EXPORT', 'Exportar submissões');

CREATE INDEX ix_roles_tenant ON roles(tenant_id);
CREATE INDEX ix_membership_roles_tenant ON membership_roles(tenant_id);
