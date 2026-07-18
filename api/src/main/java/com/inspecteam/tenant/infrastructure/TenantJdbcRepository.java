package com.inspecteam.tenant.infrastructure;

import com.inspecteam.tenant.domain.TenantSummary;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TenantJdbcRepository {
    private final JdbcClient jdbc;
    public TenantJdbcRepository(JdbcClient jdbc){this.jdbc=jdbc;}

    public List<TenantSummary> findForUser(UUID userId){return jdbc.sql("""
        SELECT t.id tenant_id,t.name,t.slug,m.id membership_id,m.membership_type,m.status membership_status
        FROM tenant_memberships m JOIN tenants t ON t.id=m.tenant_id
        WHERE m.user_id=:userId AND t.status<>'ARCHIVED' ORDER BY t.name
        """).param("userId",userId).query((rs,row)->new TenantSummary(rs.getObject("tenant_id",UUID.class),rs.getString("name"),
        rs.getString("slug"),rs.getObject("membership_id",UUID.class),rs.getString("membership_type"),rs.getString("membership_status"))).list();}

    public Optional<Membership> findActiveMembership(UUID tenantId,UUID userId){return jdbc.sql("""
        SELECT id,tenant_id,user_id,membership_type FROM tenant_memberships
        WHERE tenant_id=:tenantId AND user_id=:userId AND status='ACTIVE'
        """).param("tenantId",tenantId).param("userId",userId).query((rs,row)->new Membership(rs.getObject("id",UUID.class),
        rs.getObject("tenant_id",UUID.class),rs.getObject("user_id",UUID.class),rs.getString("membership_type"))).optional();}

    public Optional<TenantDetails> findDetails(UUID tenantId){return jdbc.sql("""
        SELECT id,name,slug,status,timezone,created_at,updated_at FROM tenants WHERE id=:tenantId
        """).param("tenantId",tenantId).query((rs,row)->new TenantDetails(rs.getObject("id",UUID.class),rs.getString("name"),
        rs.getString("slug"),rs.getString("status"),rs.getString("timezone"),rs.getTimestamp("created_at").toInstant(),
        rs.getTimestamp("updated_at").toInstant())).optional();}

    public void update(UUID tenantId,String name,String timezone){jdbc.sql("UPDATE tenants SET name=:name,timezone=:timezone,updated_at=NOW() WHERE id=:id")
        .param("name",name).param("timezone",timezone).param("id",tenantId).update();}

    public List<String> effectivePermissions(UUID tenantId,UUID membershipId,boolean owner){
        if(owner)return jdbc.sql("SELECT code FROM permissions ORDER BY code").query(String.class).list();
        return jdbc.sql("""
SELECT DISTINCT rp.permission_code FROM membership_roles mr JOIN role_permissions rp
            ON rp.tenant_id=mr.tenant_id AND rp.role_id=mr.role_id WHERE mr.tenant_id=:tenantId
            AND mr.membership_id=:membershipId ORDER BY rp.permission_code""").param("tenantId",tenantId)
            .param("membershipId",membershipId).query(String.class).list();}

    public Membership ensurePlatformMembership(UUID tenantId,UUID userId){return findActiveMembership(tenantId,userId).orElseGet(()->{
        UUID id=UUID.randomUUID(); jdbc.sql("""
INSERT INTO tenant_memberships(id,tenant_id,user_id,membership_type,status,invited_by)
            VALUES(:id,:tenantId,:userId,'PLATFORM_ADMIN','ACTIVE',:userId) ON CONFLICT(tenant_id,user_id) DO UPDATE
            SET membership_type='PLATFORM_ADMIN',status='ACTIVE',updated_at=NOW()""").param("id",id).param("tenantId",tenantId)
            .param("userId",userId).update(); return findActiveMembership(tenantId,userId).orElseThrow();});}

    public void activateRls(UUID tenantId){jdbc.sql("SELECT set_config('app.current_tenant_id',:tenantId,TRUE)").param("tenantId",tenantId.toString()).query(String.class).single();}
    public boolean hasPermission(UUID tenantId,UUID membershipId,String permission){return jdbc.sql("""
SELECT EXISTS(SELECT 1 FROM membership_roles mr
        JOIN role_permissions rp ON rp.tenant_id=mr.tenant_id AND rp.role_id=mr.role_id WHERE mr.tenant_id=:tenantId
        AND mr.membership_id=:membershipId AND rp.permission_code=:permission)""").param("tenantId",tenantId).param("membershipId",membershipId)
        .param("permission",permission).query(Boolean.class).single();}
    public boolean hasFormPermission(UUID tenantId,UUID membershipId,UUID formId,String permission){return jdbc.sql("""
        SELECT EXISTS(SELECT 1 FROM form_grants fg WHERE fg.tenant_id=:tenantId AND fg.form_id=:formId AND fg.permission_code=:permission
        AND fg.membership_id=:membershipId UNION ALL SELECT 1 FROM form_grants fg JOIN membership_roles mr ON mr.tenant_id=fg.tenant_id
        AND mr.role_id=fg.role_id WHERE fg.tenant_id=:tenantId AND fg.form_id=:formId AND fg.permission_code=:permission
        AND mr.membership_id=:membershipId)""").param("tenantId",tenantId).param("membershipId",membershipId).param("formId",formId)
        .param("permission",permission).query(Boolean.class).single();}

    public record Membership(UUID id,UUID tenantId,UUID userId,String type){public boolean owner(){return "OWNER".equals(type);}}
    public record TenantDetails(UUID id,String name,String slug,String status,String timezone,java.time.Instant createdAt,java.time.Instant updatedAt){}
}
