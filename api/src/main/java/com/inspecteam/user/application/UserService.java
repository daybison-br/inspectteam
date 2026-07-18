package com.inspecteam.user.application;

import com.inspecteam.audit.application.AuditService;
import com.inspecteam.permission.application.TenantAuthorizationService;
import com.inspecteam.shared.exception.ApiException;
import com.inspecteam.tenant.infrastructure.TenantJdbcRepository.Membership;
import com.inspecteam.user.domain.TenantUser;
import com.inspecteam.user.infrastructure.UserJdbcRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserJdbcRepository users;
    private final TenantAuthorizationService authorization;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public UserService(UserJdbcRepository users, TenantAuthorizationService authorization,
            PasswordEncoder passwordEncoder, AuditService audit) {
        this.users = users;
        this.authorization = authorization;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<TenantUser> list(UUID tenantId, UUID actorId, boolean platformAdmin) {
        authorization.require(tenantId, actorId, platformAdmin, "USER_VIEW");
        return users.list(tenantId);
    }

    @Transactional
    public UUID create(UUID tenantId, UUID actorId, boolean platformAdmin,
            String email, String displayName, String temporaryPassword) {
        Membership actor = authorization.require(tenantId, actorId, platformAdmin, "USER_CREATE");
        if (actor == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Administrador global precisa de membership para criar usuários");
        }
        try {
            UUID userId = users.findUserId(email).orElseGet(() -> users.createUser(
                    email.trim(), displayName.trim(), passwordEncoder.encode(temporaryPassword)));
            UUID membershipId = users.createMembership(tenantId, userId, actorId);
            audit.record(tenantId, actorId, actor.id(), "USER_ADDED", "MEMBERSHIP", membershipId,
                    java.util.Map.of("email", email.toLowerCase()));
            return membershipId;
        } catch (DuplicateKeyException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "Usuário já pertence ao tenant");
        }
    }

    @Transactional
    public void suspend(UUID tenantId, UUID actorId, boolean platformAdmin, UUID membershipId) {
        authorization.require(tenantId, actorId, platformAdmin, "USER_SUSPEND");
        if (!users.suspend(tenantId, membershipId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Membership não encontrado ou proprietário não pode ser suspenso");
        }
        audit.record(tenantId, actorId, null, "USER_SUSPENDED", "MEMBERSHIP", membershipId, java.util.Map.of());
    }
}
