package com.inspecteam.permission.application;

import com.inspecteam.shared.exception.ApiException;
import com.inspecteam.tenant.infrastructure.TenantJdbcRepository;
import com.inspecteam.tenant.infrastructure.TenantJdbcRepository.Membership;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TenantAuthorizationService {

    private final TenantJdbcRepository tenants;

    public TenantAuthorizationService(TenantJdbcRepository tenants) {
        this.tenants = tenants;
    }

    public Membership activate(UUID tenantId, UUID userId, boolean platformAdmin) {
        Membership membership = tenants.findActiveMembership(tenantId, userId).orElse(null);
        if (membership == null && !platformAdmin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Usuário não pertence ao tenant informado");
        }
        tenants.activateRls(tenantId);
        return membership;
    }

    public Membership require(UUID tenantId, UUID userId, boolean platformAdmin, String permission) {
        Membership membership = activate(tenantId, userId, platformAdmin);
        if (platformAdmin || membership.owner()) {
            return membership;
        }
        if (!tenants.hasPermission(tenantId, membership.id(), permission)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Permissão insuficiente: " + permission);
        }
        return membership;
    }
}
