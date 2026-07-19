package com.inspecteam;

import static org.assertj.core.api.Assertions.assertThat;

import com.inspecteam.auth.application.AuthService;
import com.inspecteam.form.application.FormService;
import com.inspecteam.management.application.ManagementService;
import com.inspecteam.submission.application.SubmissionService;
import com.inspecteam.sync.application.SyncService;
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
class InspecTeamApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    AuthService authService;

    @Autowired
    FormService formService;

    @Autowired
    SyncService syncService;

    @Autowired
    SubmissionService submissionService;

    @Autowired
    ManagementService managementService;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    tools.jackson.databind.ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void ownerCanPublishSyncSubmitAndSoftDeleteForm() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var registration = authService.registerTenant(new AuthService.RegisterTenantCommand(
                "Tenant Teste", "tenant-" + suffix, "Proprietário",
                "owner-" + suffix + "@example.com", "strong-password-123", "integration-test"));

        var definition = objectMapper.readTree("""
                {"sections":[{"id":"vehicle","fields":[
                  {"id":"plate","type":"text","label":"Placa","required":true}
                ]}]}
                """);
        UUID formId = formService.create(registration.tenantId(), registration.userId(), false,
                "Checklist", "Teste integrado", definition);
        formService.publish(registration.tenantId(), formId, registration.userId(), false);

        assertThat(formService.available(registration.tenantId(), registration.userId(), false))
                .singleElement().extracting(item -> item.id()).isEqualTo(formId);
        assertThat(formService.published(registration.tenantId(), formId, registration.userId(), false).name())
                .isEqualTo("Checklist");

        UUID deviceId = UUID.randomUUID();
        syncService.registerDevice(registration.tenantId(), registration.userId(), false,
                deviceId, "Test device", "TEST");
        var pull = syncService.pull(registration.tenantId(), registration.userId(), false, 0);
        assertThat(pull.forms()).hasSize(1);

        UUID submissionId = UUID.randomUUID();
        UUID versionId = pull.forms().getFirst().versionId();
        submissionService.create(registration.tenantId(), registration.userId(), false,
                submissionId, formId, versionId, objectMapper.createObjectNode().put("plate", "ABC1D23"), null);
        submissionService.complete(registration.tenantId(), registration.userId(), false,
                formId, submissionId, 0, objectMapper.createObjectNode().put("plate", "ABC1D23"));

        assertThat(submissionService.list(registration.tenantId(), registration.userId(), false, formId))
                .singleElement().extracting(item -> item.status()).isEqualTo("COMPLETED");

        managementService.softDeleteForm(registration.tenantId(), formId, registration.userId(), false);

        assertThat(formService.list(registration.tenantId(), registration.userId(), false)).isEmpty();
        assertThat(formService.available(registration.tenantId(), registration.userId(), false)).isEmpty();
        assertThat(jdbc.sql("SELECT deleted FROM forms WHERE tenant_id=:tenantId AND id=:formId")
                .param("tenantId", registration.tenantId()).param("formId", formId)
                .query(Boolean.class).single()).isTrue();
        assertThat(jdbc.sql("""
                SELECT COUNT(*) FROM sync_tombstones
                 WHERE tenant_id=:tenantId AND entity_type='FORM' AND entity_id=:formId
                """).param("tenantId", registration.tenantId()).param("formId", formId)
                .query(Integer.class).single()).isEqualTo(1);
        assertThat(submissionService.list(registration.tenantId(), registration.userId(), false, formId))
                .singleElement().extracting(item -> item.status()).isEqualTo("COMPLETED");
    }
}
