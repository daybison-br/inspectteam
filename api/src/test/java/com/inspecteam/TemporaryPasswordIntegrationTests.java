package com.inspecteam;

import static org.assertj.core.api.Assertions.assertThat;

import com.inspecteam.account.application.AccountService;
import com.inspecteam.auth.application.AuthService;
import com.inspecteam.user.application.UserService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
class TemporaryPasswordIntegrationTests {
    @Container @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");
    @Autowired AuthService auth;
    @Autowired UserService users;
    @Autowired AccountService accounts;

    @Test
    void invitedUserMustChangeTemporaryPassword() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var owner = auth.registerTenant(new AuthService.RegisterTenantCommand(
                "Empresa Senha", "empresa-senha-" + suffix, "Proprietário",
                "owner-password-" + suffix + "@example.com", "strong-password-123", "test"));
        users.create(owner.tenantId(), owner.userId(), false, "member-" + suffix + "@example.com",
                "Pessoa Convidada", "temporary-password-123");
        var member = users.list(owner.tenantId(), owner.userId(), false).stream()
                .filter(user -> user.email().startsWith("member-"))
                .findFirst().orElseThrow();
        assertThat(accounts.get(member.userId()).mustChangePassword()).isTrue();
        accounts.changePassword(member.userId(), "temporary-password-123", "permanent-password-456");
        assertThat(accounts.get(member.userId()).mustChangePassword()).isFalse();
    }
}
