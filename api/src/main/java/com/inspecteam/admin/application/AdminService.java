package com.inspecteam.admin.application;

import com.inspecteam.shared.exception.ApiException;
import com.inspecteam.tenant.infrastructure.TenantJdbcRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final JdbcClient jdbc; private final TenantJdbcRepository tenants;
    public AdminService(JdbcClient jdbc,TenantJdbcRepository tenants){this.jdbc=jdbc;this.tenants=tenants;}
    public void require(boolean admin){if(!admin)throw new ApiException(HttpStatus.FORBIDDEN,"Acesso exclusivo do administrador global");}
    @Transactional(readOnly=true) public Overview overview(){return jdbc.sql("""
SELECT
        (SELECT COUNT(*) FROM tenants) tenants,(SELECT COUNT(*) FROM tenants WHERE status='ACTIVE') active_tenants,
        (SELECT COUNT(*) FROM users) users,(SELECT COUNT(*) FROM submissions WHERE status='COMPLETED') submissions
        """).query((rs,row)->new Overview(rs.getLong("tenants"),rs.getLong("active_tenants"),rs.getLong("users"),rs.getLong("submissions"))).single();}
    @Transactional(readOnly=true) public Page<TenantView> listTenants(String query,String status,int page,int size){int safe=Math.min(Math.max(size,1),100),offset=Math.max(page,0)*safe;
        String q="%"+(query==null?"":query.trim().toLowerCase())+"%",s=status==null?"":status;
        long total=jdbc.sql("SELECT COUNT(*) FROM tenants WHERE (LOWER(name) LIKE :q OR LOWER(slug) LIKE :q) AND (:s='' OR status=:s)").param("q",q).param("s",s).query(Long.class).single();
        var items=jdbc.sql("""
SELECT t.id,t.name,t.slug,t.status,t.timezone,t.created_at,
            (SELECT COUNT(*) FROM tenant_memberships m WHERE m.tenant_id=t.id AND m.membership_type<>'PLATFORM_ADMIN') members,
            (SELECT COUNT(*) FROM forms f WHERE f.tenant_id=t.id) forms FROM tenants t
            WHERE (LOWER(t.name) LIKE :q OR LOWER(t.slug) LIKE :q) AND (:s='' OR t.status=:s)
            ORDER BY t.created_at DESC LIMIT :limit OFFSET :offset""").param("q",q).param("s",s).param("limit",safe).param("offset",offset)
            .query((rs,row)->new TenantView(rs.getObject("id",UUID.class),rs.getString("name"),rs.getString("slug"),rs.getString("status"),
                rs.getString("timezone"),rs.getLong("members"),rs.getLong("forms"),rs.getTimestamp("created_at").toInstant())).list(); return new Page<>(items,total,page,safe);}
    @Transactional(readOnly=true) public Page<UserView> listUsers(String query,String status,int page,int size){int safe=Math.min(Math.max(size,1),100),offset=Math.max(page,0)*safe;
        String q="%"+(query==null?"":query.trim().toLowerCase())+"%",s=status==null?"":status;
        long total=jdbc.sql("SELECT COUNT(*) FROM users WHERE (LOWER(display_name) LIKE :q OR LOWER(email) LIKE :q) AND (:s='' OR status=:s)").param("q",q).param("s",s).query(Long.class).single();
        var items=jdbc.sql("""
SELECT id,email,display_name,status,platform_admin,created_at FROM users
            WHERE (LOWER(display_name) LIKE :q OR LOWER(email) LIKE :q) AND (:s='' OR status=:s)
            ORDER BY created_at DESC LIMIT :limit OFFSET :offset""").param("q",q).param("s",s).param("limit",safe).param("offset",offset)
            .query((rs,row)->new UserView(rs.getObject("id",UUID.class),rs.getString("email"),rs.getString("display_name"),rs.getString("status"),
                rs.getBoolean("platform_admin"),rs.getTimestamp("created_at").toInstant())).list(); return new Page<>(items,total,page,safe);}
    @Transactional public void tenantStatus(UUID actor,UUID tenantId,String status){jdbc.sql("UPDATE tenants SET status=:s,updated_at=NOW() WHERE id=:id").param("s",status).param("id",tenantId).update();audit(actor,"TENANT_STATUS_CHANGED","TENANT",tenantId,"{\"status\":\""+status+"\"}");}
    @Transactional public void userStatus(UUID actor,UUID userId,String status){jdbc.sql("UPDATE users SET status=:s,updated_at=NOW() WHERE id=:id").param("s",status).param("id",userId).update();audit(actor,"USER_STATUS_CHANGED","USER",userId,"{\"status\":\""+status+"\"}");}
    @Transactional public void platformAdmin(UUID actor,UUID userId,boolean enabled){if(!enabled){long active=jdbc.sql("SELECT COUNT(*) FROM users WHERE platform_admin AND status='ACTIVE'").query(Long.class).single();boolean target=jdbc.sql("SELECT platform_admin FROM users WHERE id=:id").param("id",userId).query(Boolean.class).optional().orElse(false);if(target&&active<=1)throw new ApiException(HttpStatus.CONFLICT,"O Ãºltimo administrador ativo nÃ£o pode ser removido");}
        jdbc.sql("UPDATE users SET platform_admin=:enabled,updated_at=NOW() WHERE id=:id").param("enabled",enabled).param("id",userId).update();audit(actor,"PLATFORM_ADMIN_CHANGED","USER",userId,"{\"enabled\":"+enabled+"}");}
    @Transactional public UUID enter(UUID actor,UUID tenantId){if(tenants.findDetails(tenantId).isEmpty())throw new ApiException(HttpStatus.NOT_FOUND,"Tenant nÃ£o encontrado");tenants.activateRls(tenantId);UUID membership=tenants.ensurePlatformMembership(tenantId,actor).id();audit(actor,"PLATFORM_ADMIN_ENTERED_TENANT","TENANT",tenantId,"{}");return membership;}
    private void audit(UUID actor,String action,String type,UUID id,String details){jdbc.sql("INSERT INTO platform_audit_events(actor_user_id,action,resource_type,resource_id,details) VALUES(:actor,:action,:type,:id,CAST(:details AS JSONB))").param("actor",actor).param("action",action).param("type",type).param("id",id).param("details",details).update();}
    public record Overview(long tenants,long activeTenants,long users,long completedSubmissions){}
    public record TenantView(UUID id,String name,String slug,String status,String timezone,long members,long forms,Instant createdAt){}
    public record UserView(UUID id,String email,String displayName,String status,boolean platformAdmin,Instant createdAt){}
    public record Page<T>(List<T> items,long total,int page,int size){}
}
