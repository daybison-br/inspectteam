package com.inspecteam.tenant.api;

import com.inspecteam.shared.security.CurrentUser;
import com.inspecteam.tenant.application.TenantQueryService;
import com.inspecteam.tenant.domain.TenantSummary;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantQueryService service;

    public TenantController(TenantQueryService service) {
        this.service = service;
    }

    @GetMapping
    List<TenantSummary> list(Authentication authentication) {
        return service.listForUser(CurrentUser.id(authentication));
    }
}
