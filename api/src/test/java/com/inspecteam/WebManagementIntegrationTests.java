package com.inspecteam;

import static org.assertj.core.api.Assertions.assertThat;

import com.inspecteam.account.application.AccountService;
import com.inspecteam.admin.application.AdminService;
import com.inspecteam.auth.application.AuthService;
import com.inspecteam.management.application.ManagementService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
class WebManagementIntegrationTests {
    @Container @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");
    @Autowired AuthService auth;
    @Autowired AccountService accounts;
    @Autowired ManagementService management;
    @Autowired AdminService admins;
    @Autowired JdbcClient jdbc;

    @Test
    void profileTenantContextAndPlatformAdministrationWork() {
        String suffix=UUID.randomUUID().toString().substring(0,8);
        var owner=auth.registerTenant(new AuthService.RegisterTenantCommand("Empresa A","empresa-a-"+suffix,"Ana",
                "ana-"+suffix+"@example.com","strong-password-123","test"));
        var other=auth.registerTenant(new AuthService.RegisterTenantCommand("Empresa B","empresa-b-"+suffix,"Bruno",
                "bruno-"+suffix+"@example.com","strong-password-123","test"));
        assertThat(accounts.get(owner.userId()).displayName()).isEqualTo("Ana");
        accounts.update(owner.userId(),"Ana Gestora");
        accounts.changePassword(owner.userId(),"strong-password-123","new-strong-password-456");
        assertThat(auth.login(new AuthService.LoginCommand("ana-"+suffix+"@example.com","new-strong-password-456","test")).userId()).isEqualTo(owner.userId());
        assertThat(management.context(owner.tenantId(),owner.userId(),false).membershipType()).isEqualTo("OWNER");
        assertThat(management.dashboard(owner.tenantId(),owner.userId(),false).activeMembers()).isEqualTo(1);
        jdbc.sql("UPDATE users SET platform_admin=TRUE WHERE id=:id").param("id",owner.userId()).update();
        admins.require(true);
        assertThat(admins.listTenants("Empresa","",0,20).total()).isEqualTo(2);
        assertThat(admins.enter(owner.userId(),other.tenantId())).isNotNull();
        assertThat(management.context(other.tenantId(),owner.userId(),true).platformAdmin()).isTrue();
    }
}
