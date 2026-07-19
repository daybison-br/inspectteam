package com.inspecteam.management.application;

import com.inspecteam.audit.application.AuditService;
import com.inspecteam.permission.application.TenantAuthorizationService;
import com.inspecteam.shared.exception.ApiException;
import com.inspecteam.tenant.infrastructure.TenantJdbcRepository;
import com.inspecteam.tenant.infrastructure.TenantJdbcRepository.Membership;
import com.inspecteam.tenant.infrastructure.TenantJdbcRepository.TenantDetails;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class ManagementService {
    private final JdbcClient jdbc;
    private final ObjectMapper mapper;
    private final TenantAuthorizationService authorization;
    private final TenantJdbcRepository tenants;
    private final AuditService audit;

    public ManagementService(JdbcClient jdbc, ObjectMapper mapper, TenantAuthorizationService authorization,
            TenantJdbcRepository tenants, AuditService audit) {
        this.jdbc = jdbc; this.mapper = mapper; this.authorization = authorization; this.tenants = tenants; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public TenantContext context(UUID tenantId, UUID userId, boolean admin) {
        Membership membership = authorization.activate(tenantId, userId, admin);
        TenantDetails tenant = tenants.findDetails(tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tenant não encontrado"));
        List<String> permissions = admin ? jdbc.sql("SELECT code FROM permissions ORDER BY code").query(String.class).list()
                : tenants.effectivePermissions(tenantId, membership.id(), membership.owner());
        return new TenantContext(tenant, membership == null ? null : membership.id(),
                membership == null ? "PLATFORM_ADMIN" : membership.type(), admin, permissions);
    }

    @Transactional(readOnly = true)
    public Dashboard dashboard(UUID tenantId, UUID userId, boolean admin) {
        authorization.activate(tenantId, userId, admin);
        return jdbc.sql("""
                SELECT (SELECT COUNT(*) FROM forms WHERE tenant_id=:id AND deleted=FALSE AND status<>'ARCHIVED') forms,
                  (SELECT COUNT(*) FROM submissions WHERE tenant_id=:id AND status='COMPLETED') submissions,
                  (SELECT COUNT(*) FROM tenant_memberships WHERE tenant_id=:id AND status='ACTIVE' AND membership_type<>'PLATFORM_ADMIN') members,
                  (SELECT COUNT(*) FROM forms WHERE tenant_id=:id AND deleted=FALSE AND status='DRAFT') drafts
                """).param("id", tenantId).query((rs,row)->new Dashboard(rs.getLong("forms"),rs.getLong("submissions"),
                        rs.getLong("members"),rs.getLong("drafts"))).single();
    }

    @Transactional
    public TenantDetails updateTenant(UUID tenantId, UUID userId, boolean admin, String name, String timezone) {
        Membership m = authorization.require(tenantId,userId,admin,"TENANT_MANAGE");
        tenants.update(tenantId,name.trim(),timezone.trim());
        audit.record(tenantId,userId,m==null?null:m.id(),"TENANT_UPDATED","TENANT",tenantId,Map.of("name",name.trim()));
        return tenants.findDetails(tenantId).orElseThrow();
    }

    @Transactional(readOnly = true)
    public FormDetails form(UUID tenantId, UUID formId, UUID userId, boolean admin) {
        authorization.requireForm(tenantId,userId,admin,formId,"FORM_VIEW");
        return jdbc.sql("""
                SELECT f.id,f.name,f.description,f.status,f.updated_at,v.id version_id,v.version_number,v.definition::text
                  FROM forms f JOIN form_versions v ON v.tenant_id=f.tenant_id AND v.form_id=f.id AND v.status='DRAFT'
                 WHERE f.tenant_id=:tenantId AND f.id=:formId AND f.deleted=FALSE
                """).param("tenantId",tenantId).param("formId",formId)
                .query((rs,row)->new FormDetails(rs.getObject("id",UUID.class),rs.getString("name"),rs.getString("description"),
                        rs.getString("status"),rs.getObject("version_id",UUID.class),rs.getInt("version_number"),
                        json(rs.getString("definition")),rs.getTimestamp("updated_at").toInstant())).optional()
                .orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Formulário não encontrado"));
    }

    @Transactional
    public void updateForm(UUID tenantId, UUID formId, UUID userId, boolean admin, String name, String description) {
        authorization.requireForm(tenantId,userId,admin,formId,"FORM_EDIT");
        int changed=jdbc.sql("UPDATE forms SET name=:name,description=:description,updated_at=NOW() WHERE tenant_id=:tenantId AND id=:formId AND deleted=FALSE")
                .param("name",name.trim()).param("description",description).param("tenantId",tenantId).param("formId",formId).update();
        if(changed==0) throw new ApiException(HttpStatus.NOT_FOUND,"Formulário não encontrado");
        audit.record(tenantId,userId,null,"FORM_METADATA_UPDATED","FORM",formId,Map.of("name",name.trim()));
    }

    @Transactional
    public void setFormArchived(UUID tenantId, UUID formId, UUID userId, boolean admin, boolean archived) {
        authorization.requireForm(tenantId,userId,admin,formId,"FORM_ARCHIVE");
        int changed=jdbc.sql("UPDATE forms SET status=:status,updated_at=NOW() WHERE tenant_id=:tenantId AND id=:formId AND deleted=FALSE")
                .param("status",archived?"ARCHIVED":"DRAFT").param("tenantId",tenantId).param("formId",formId).update();
        if(changed==0) throw new ApiException(HttpStatus.NOT_FOUND,"Formulário não encontrado");
        audit.record(tenantId,userId,null,archived?"FORM_ARCHIVED":"FORM_RESTORED","FORM",formId,Map.of());
    }

    @Transactional
    public void softDeleteForm(UUID tenantId, UUID formId, UUID userId, boolean admin) {
        Membership membership = authorization.requireForm(tenantId, userId, admin, formId, "FORM_ARCHIVE");
        if (membership == null) {
            membership = tenants.ensurePlatformMembership(tenantId, userId);
        }
        int changed = jdbc.sql("""
                UPDATE forms SET deleted=TRUE,deleted_at=NOW(),deleted_by=:deletedBy,
                       status='ARCHIVED',updated_at=NOW()
                 WHERE tenant_id=:tenantId AND id=:formId AND deleted=FALSE
                """).param("deletedBy", membership.id()).param("tenantId", tenantId).param("formId", formId).update();
        if (changed == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Formulário não encontrado");
        }
        jdbc.sql("""
                INSERT INTO sync_tombstones(tenant_id,entity_type,entity_id)
                VALUES(:tenantId,'FORM',:formId)
                ON CONFLICT(tenant_id,entity_type,entity_id) DO UPDATE SET deleted_at=NOW()
                """).param("tenantId", tenantId).param("formId", formId).update();
        audit.record(tenantId, userId, membership.id(), "FORM_DELETED", "FORM", formId,
                Map.of("softDelete", true));
    }
    @Transactional(readOnly = true)
    public SubmissionDetails submission(UUID tenantId, UUID id, UUID formId, UUID userId, boolean admin) {
        authorization.requireForm(tenantId,userId,admin,formId,"SUBMISSION_VIEW");
        return jdbc.sql("""
                SELECT s.id,s.form_id,s.form_version_id,s.submitted_by,u.display_name,u.email,s.status,s.revision,
                       s.answers::text,v.definition::text,s.client_created_at,s.submitted_at,s.created_at,s.updated_at
                  FROM submissions s JOIN form_versions v ON v.tenant_id=s.tenant_id AND v.id=s.form_version_id
                  JOIN tenant_memberships m ON m.tenant_id=s.tenant_id AND m.id=s.submitted_by JOIN users u ON u.id=m.user_id
                 WHERE s.tenant_id=:tenantId AND s.id=:id AND s.form_id=:formId
                """).param("tenantId",tenantId).param("id",id).param("formId",formId)
                .query((rs,row)->new SubmissionDetails(rs.getObject("id",UUID.class),rs.getObject("form_id",UUID.class),
                        rs.getObject("form_version_id",UUID.class),rs.getObject("submitted_by",UUID.class),rs.getString("display_name"),
                        rs.getString("email"),rs.getString("status"),rs.getInt("revision"),json(rs.getString("answers")),
                        json(rs.getString("definition")),instant(rs,"client_created_at"),instant(rs,"submitted_at"),
                        instant(rs,"created_at"),instant(rs,"updated_at"))).optional()
                .orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Resposta não encontrada"));
    }

    @Transactional(readOnly = true)
    public List<PermissionView> permissionCatalog(UUID tenantId,UUID userId,boolean admin){
        authorization.require(tenantId,userId,admin,"TENANT_MANAGE");
        return jdbc.sql("SELECT code,description FROM permissions ORDER BY code")
                .query((rs,row)->new PermissionView(rs.getString("code"),rs.getString("description"))).list();
    }

    @Transactional(readOnly = true)
    public AccessView access(UUID tenantId,UUID userId,boolean admin){
        authorization.require(tenantId,userId,admin,"TENANT_MANAGE");
        List<RoleAssignment> assignments=jdbc.sql("""
                SELECT mr.membership_id,mr.role_id FROM membership_roles mr WHERE mr.tenant_id=:tenantId
                """).param("tenantId",tenantId).query((rs,row)->new RoleAssignment(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class))).list();
        List<FormGrant> grants=jdbc.sql("""
                SELECT id,form_id,membership_id,role_id,permission_code FROM form_grants WHERE tenant_id=:tenantId ORDER BY created_at
                """).param("tenantId",tenantId).query((rs,row)->new FormGrant(rs.getObject("id",UUID.class),rs.getObject("form_id",UUID.class),
                        rs.getObject("membership_id",UUID.class),rs.getObject("role_id",UUID.class),rs.getString("permission_code"))).list();
        return new AccessView(assignments,grants);
    }

    @Transactional
    public void removeRole(UUID tenantId,UUID membershipId,UUID roleId,UUID userId,boolean admin){
        authorization.require(tenantId,userId,admin,"TENANT_MANAGE");
        jdbc.sql("DELETE FROM membership_roles WHERE tenant_id=:tenantId AND membership_id=:membershipId AND role_id=:roleId")
                .param("tenantId",tenantId).param("membershipId",membershipId).param("roleId",roleId).update();
        audit.record(tenantId,userId,null,"ROLE_UNASSIGNED","MEMBERSHIP",membershipId,Map.of("roleId",roleId));
    }

    @Transactional
    public void removeGrant(UUID tenantId,UUID grantId,UUID userId,boolean admin){
        authorization.require(tenantId,userId,admin,"FORM_MANAGE_ACCESS");
        jdbc.sql("DELETE FROM form_grants WHERE tenant_id=:tenantId AND id=:id").param("tenantId",tenantId).param("id",grantId).update();
        audit.record(tenantId,userId,null,"FORM_ACCESS_REVOKED","FORM_GRANT",grantId,Map.of());
    }

    private JsonNode json(String value){try{return mapper.readTree(value);}catch(JacksonException e){throw new IllegalStateException(e);}}
    private Instant instant(java.sql.ResultSet rs,String column){try{var t=rs.getTimestamp(column);return t==null?null:t.toInstant();}catch(java.sql.SQLException e){throw new IllegalStateException(e);}}
    public record TenantContext(TenantDetails tenant,UUID membershipId,String membershipType,boolean platformAdmin,List<String> permissions){}
    public record Dashboard(long forms,long completedSubmissions,long activeMembers,long drafts){}
    public record FormDetails(UUID id,String name,String description,String status,UUID draftVersionId,int draftVersion,JsonNode definition,Instant updatedAt){}
    public record SubmissionDetails(UUID id,UUID formId,UUID formVersionId,UUID submittedBy,String submittedByName,String submittedByEmail,String status,int revision,JsonNode answers,JsonNode definition,Instant clientCreatedAt,Instant submittedAt,Instant createdAt,Instant updatedAt){}
    public record PermissionView(String code,String description){}
    public record RoleAssignment(UUID membershipId,UUID roleId){}
    public record FormGrant(UUID id,UUID formId,UUID membershipId,UUID roleId,String permission){}
    public record AccessView(List<RoleAssignment> assignments,List<FormGrant> grants){}
}
