CREATE TABLE submissions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    form_id UUID NOT NULL,
    form_version_id UUID NOT NULL,
    submitted_by UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    answers JSONB NOT NULL DEFAULT '{}'::jsonb,
    revision INTEGER NOT NULL DEFAULT 0,
    client_created_at TIMESTAMPTZ,
    submitted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_submissions_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_submissions_form FOREIGN KEY (tenant_id, form_id)
        REFERENCES forms(tenant_id, id),
    CONSTRAINT fk_submissions_version FOREIGN KEY (tenant_id, form_version_id)
        REFERENCES form_versions(tenant_id, id),
    CONSTRAINT fk_submissions_member FOREIGN KEY (tenant_id, submitted_by)
        REFERENCES tenant_memberships(tenant_id, id),
    CONSTRAINT ck_submissions_status CHECK (status IN ('DRAFT', 'COMPLETED', 'VOIDED')),
    CONSTRAINT ck_submission_answers_object CHECK (jsonb_typeof(answers) = 'object')
);

CREATE INDEX ix_submissions_tenant_form ON submissions(tenant_id, form_id, created_at DESC);
CREATE INDEX ix_submissions_member ON submissions(tenant_id, submitted_by, created_at DESC);
CREATE INDEX ix_submissions_answers_gin ON submissions USING GIN(answers);

CREATE TABLE submission_revisions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    submission_id UUID NOT NULL,
    revision INTEGER NOT NULL,
    answers JSONB NOT NULL,
    changed_by UUID NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_submission_revision UNIQUE (submission_id, revision),
    CONSTRAINT fk_revision_submission FOREIGN KEY (tenant_id, submission_id)
        REFERENCES submissions(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_revision_member FOREIGN KEY (tenant_id, changed_by)
        REFERENCES tenant_memberships(tenant_id, id)
);
