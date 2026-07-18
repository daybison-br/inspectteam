CREATE OR REPLACE FUNCTION app_current_tenant_id()
RETURNS UUID
LANGUAGE SQL
STABLE
AS $$
    SELECT NULLIF(current_setting('app.current_tenant_id', TRUE), '')::UUID
$$;

DO $$
DECLARE
    secured_table TEXT;
BEGIN
    FOREACH secured_table IN ARRAY ARRAY[
        'roles',
        'role_permissions',
        'membership_roles',
        'forms',
        'form_versions',
        'form_grants',
        'submissions',
        'submission_revisions',
        'files',
        'devices',
        'sync_mutations',
        'sync_tombstones',
        'audit_events'
    ] LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', secured_table);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', secured_table);
        EXECUTE format(
            'CREATE POLICY tenant_isolation ON %I USING (tenant_id = app_current_tenant_id()) WITH CHECK (tenant_id = app_current_tenant_id())',
            secured_table
        );
    END LOOP;
END
$$;
