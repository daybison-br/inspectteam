ALTER TABLE forms
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deleted_at TIMESTAMPTZ,
    ADD COLUMN deleted_by UUID;

ALTER TABLE forms
    ADD CONSTRAINT fk_forms_deleted_by FOREIGN KEY (tenant_id, deleted_by)
        REFERENCES tenant_memberships(tenant_id, id),
    ADD CONSTRAINT ck_forms_deleted_state CHECK (
        (deleted = FALSE AND deleted_at IS NULL) OR
        (deleted = TRUE AND deleted_at IS NOT NULL)
    );

CREATE INDEX ix_forms_active_tenant_status
    ON forms(tenant_id, status, updated_at DESC)
    WHERE deleted = FALSE;
