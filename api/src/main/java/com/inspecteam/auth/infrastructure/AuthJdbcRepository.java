package com.inspecteam.auth.infrastructure;

import com.inspecteam.auth.domain.UserAccount;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AuthJdbcRepository {

    private final JdbcClient jdbc;

    public AuthJdbcRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserAccount> findByEmail(String email) {
        return jdbc.sql("""
                SELECT id, email, password_hash, display_name, status, platform_admin
                  FROM users
                 WHERE LOWER(email) = LOWER(:email)
                """)
                .param("email", email)
                .query((rs, rowNum) -> new UserAccount(
                        rs.getObject("id", UUID.class),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("display_name"),
                        rs.getString("status"),
                        rs.getBoolean("platform_admin")))
                .optional();
    }

    public UUID insertUser(String email, String passwordHash, String displayName) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO users (id, email, password_hash, display_name, status)
                VALUES (:id, :email, :passwordHash, :displayName, 'ACTIVE')
                """)
                .param("id", id)
                .param("email", email.toLowerCase())
                .param("passwordHash", passwordHash)
                .param("displayName", displayName)
                .update();
        return id;
    }

    public UUID insertTenant(String name, String slug) {
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO tenants (id, name, slug) VALUES (:id, :name, :slug)")
                .param("id", id)
                .param("name", name)
                .param("slug", slug.toLowerCase())
                .update();
        return id;
    }

    public UUID insertOwnerMembership(UUID tenantId, UUID userId) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO tenant_memberships (id, tenant_id, user_id, membership_type, status)
                VALUES (:id, :tenantId, :userId, 'OWNER', 'ACTIVE')
                """)
                .param("id", id)
                .param("tenantId", tenantId)
                .param("userId", userId)
                .update();
        return id;
    }

    public void storeRefreshToken(UUID userId, String tokenHash, Instant expiresAt, String deviceName) {
        jdbc.sql("""
                INSERT INTO refresh_tokens (user_id, token_hash, expires_at, device_name)
                VALUES (:userId, :tokenHash, :expiresAt, :deviceName)
                """)
                .param("userId", userId)
                .param("tokenHash", tokenHash)
                .param("expiresAt", expiresAt)
                .param("deviceName", deviceName)
                .update();
    }

    public Optional<RefreshSession> findRefreshSession(String tokenHash) {
        return jdbc.sql("""
                SELECT rt.id, rt.user_id, u.platform_admin
                  FROM refresh_tokens rt
                  JOIN users u ON u.id = rt.user_id
                 WHERE rt.token_hash = :tokenHash
                   AND rt.revoked_at IS NULL
                   AND rt.expires_at > NOW()
                   AND u.status = 'ACTIVE'
                """)
                .param("tokenHash", tokenHash)
                .query((rs, rowNum) -> new RefreshSession(
                        rs.getObject("id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getBoolean("platform_admin")))
                .optional();
    }

    public void revokeRefreshToken(UUID id) {
        jdbc.sql("UPDATE refresh_tokens SET revoked_at = NOW() WHERE id = :id AND revoked_at IS NULL")
                .param("id", id)
                .update();
    }

    public record RefreshSession(UUID id, UUID userId, boolean platformAdmin) {
    }
}
