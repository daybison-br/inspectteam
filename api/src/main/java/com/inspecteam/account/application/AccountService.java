package com.inspecteam.account.application;

import com.inspecteam.shared.exception.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final JdbcClient jdbc;
    private final PasswordEncoder passwordEncoder;

    public AccountService(JdbcClient jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AccountView get(UUID userId) {
        return jdbc.sql("""
                SELECT id, email, display_name, status, platform_admin, must_change_password
                  FROM users WHERE id = :id
                """).param("id", userId)
                .query((rs, row) -> new AccountView(rs.getObject("id", UUID.class), rs.getString("email"),
                        rs.getString("display_name"), rs.getString("status"), rs.getBoolean("platform_admin"),
                        rs.getBoolean("must_change_password"))).optional()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conta não encontrada"));
    }

    @Transactional
    public AccountView update(UUID userId, String displayName) {
        jdbc.sql("UPDATE users SET display_name = :name, updated_at = NOW() WHERE id = :id")
                .param("name", displayName.trim()).param("id", userId).update();
        return get(userId);
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        String hash = jdbc.sql("SELECT password_hash FROM users WHERE id = :id")
                .param("id", userId).query(String.class).optional()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conta não encontrada"));
        if (!passwordEncoder.matches(currentPassword, hash)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Senha atual inválida");
        }
        jdbc.sql("""
                UPDATE users SET password_hash = :hash, must_change_password = FALSE, updated_at = NOW()
                 WHERE id = :id
                """).param("hash", passwordEncoder.encode(newPassword)).param("id", userId).update();
    }

    public record AccountView(UUID id, String email, String displayName, String status,
            boolean platformAdmin, boolean mustChangePassword) { }
}
