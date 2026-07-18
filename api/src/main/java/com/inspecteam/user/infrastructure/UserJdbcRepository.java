package com.inspecteam.user.infrastructure;

import com.inspecteam.user.domain.TenantUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class UserJdbcRepository {

    private final JdbcClient jdbc;

    public UserJdbcRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<TenantUser> list(UUID tenantId) {
        return jdbc.sql("""
                SELECT m.id membership_id, u.id user_id, u.email, u.display_name,
                       m.membership_type, m.status, m.created_at
                  FROM tenant_memberships m JOIN users u ON u.id = m.user_id
                 WHERE m.tenant_id = :tenantId ORDER BY u.display_name
                """).param("tenantId", tenantId)
                .query((rs, rowNum) -> new TenantUser(
                        rs.getObject("membership_id", UUID.class), rs.getObject("user_id", UUID.class),
                        rs.getString("email"), rs.getString("display_name"), rs.getString("membership_type"),
                        rs.getString("status"), rs.getTimestamp("created_at").toInstant())).list();
    }

    public Optional<UUID> findUserId(String email) {
        return jdbc.sql("SELECT id FROM users WHERE LOWER(email) = LOWER(:email)")
                .param("email", email).query(UUID.class).optional();
    }

    public UUID createUser(String email, String displayName, String passwordHash) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO users (id, email, display_name, password_hash, status, must_change_password)
                VALUES (:id, :email, :displayName, :passwordHash, 'ACTIVE', TRUE)
                """).param("id", id).param("email", email.toLowerCase()).param("displayName", displayName)
                .param("passwordHash", passwordHash).update();
        return id;
    }

    public UUID createMembership(UUID tenantId, UUID userId, UUID invitedBy) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO tenant_memberships (id, tenant_id, user_id, membership_type, status, invited_by)
                VALUES (:id, :tenantId, :userId, 'MEMBER', 'ACTIVE', :invitedBy)
                """).param("id", id).param("tenantId", tenantId).param("userId", userId)
                .param("invitedBy", invitedBy).update();
        return id;
    }

    public boolean suspend(UUID tenantId, UUID membershipId) {
        return jdbc.sql("""
                UPDATE tenant_memberships SET status = 'SUSPENDED', updated_at = NOW()
                 WHERE tenant_id = :tenantId AND id = :membershipId AND membership_type <> 'OWNER'
                """).param("tenantId", tenantId).param("membershipId", membershipId).update() == 1;
    }
}
