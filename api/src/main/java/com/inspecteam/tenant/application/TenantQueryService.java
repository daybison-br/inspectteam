package com.inspecteam.tenant.application;

import com.inspecteam.tenant.domain.TenantSummary;
import com.inspecteam.tenant.infrastructure.TenantJdbcRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantQueryService {

    private final TenantJdbcRepository repository;

    public TenantQueryService(TenantJdbcRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TenantSummary> listForUser(UUID userId) {
        return repository.findForUser(userId);
    }
}
