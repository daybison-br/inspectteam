package com.inspecteam.management.api;

import com.inspecteam.management.application.ManagementService;
import com.inspecteam.shared.security.CurrentUser;
import com.inspecteam.tenant.infrastructure.TenantJdbcRepository.TenantDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}")
public class ManagementController {
    private final ManagementService service;
    public ManagementController(ManagementService service){this.service=service;}

    @GetMapping("/context")
    ManagementService.TenantContext context(@PathVariable UUID tenantId,Authentication auth){return service.context(tenantId,id(auth),admin(auth));}
    @GetMapping("/dashboard")
    ManagementService.Dashboard dashboard(@PathVariable UUID tenantId,Authentication auth){return service.dashboard(tenantId,id(auth),admin(auth));}
    @PatchMapping
    TenantDetails updateTenant(@PathVariable UUID tenantId,@Valid @RequestBody TenantRequest r,Authentication auth){return service.updateTenant(tenantId,id(auth),admin(auth),r.name(),r.timezone());}

    @GetMapping("/forms/{formId}")
    ManagementService.FormDetails form(@PathVariable UUID tenantId,@PathVariable UUID formId,Authentication auth){return service.form(tenantId,formId,id(auth),admin(auth));}
    @PatchMapping("/forms/{formId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateForm(@PathVariable UUID tenantId,@PathVariable UUID formId,@Valid @RequestBody FormRequest r,Authentication auth){service.updateForm(tenantId,formId,id(auth),admin(auth),r.name(),r.description());}
    @PostMapping("/forms/{formId}/archive") @ResponseStatus(HttpStatus.NO_CONTENT)
    void archive(@PathVariable UUID tenantId,@PathVariable UUID formId,Authentication auth){service.setFormArchived(tenantId,formId,id(auth),admin(auth),true);}
    @PostMapping("/forms/{formId}/restore") @ResponseStatus(HttpStatus.NO_CONTENT)
    void restore(@PathVariable UUID tenantId,@PathVariable UUID formId,Authentication auth){service.setFormArchived(tenantId,formId,id(auth),admin(auth),false);}

    @GetMapping("/submissions/{submissionId}")
    ManagementService.SubmissionDetails submission(@PathVariable UUID tenantId,@PathVariable UUID submissionId,@RequestParam UUID formId,Authentication auth){return service.submission(tenantId,submissionId,formId,id(auth),admin(auth));}
    @GetMapping("/permissions/catalog")
    List<ManagementService.PermissionView> catalog(@PathVariable UUID tenantId,Authentication auth){return service.permissionCatalog(tenantId,id(auth),admin(auth));}
    @GetMapping("/permissions/access")
    ManagementService.AccessView access(@PathVariable UUID tenantId,Authentication auth){return service.access(tenantId,id(auth),admin(auth));}
    @DeleteMapping("/permissions/roles/{roleId}/members/{membershipId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeRole(@PathVariable UUID tenantId,@PathVariable UUID roleId,@PathVariable UUID membershipId,Authentication auth){service.removeRole(tenantId,membershipId,roleId,id(auth),admin(auth));}
    @DeleteMapping("/permissions/grants/{grantId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeGrant(@PathVariable UUID tenantId,@PathVariable UUID grantId,Authentication auth){service.removeGrant(tenantId,grantId,id(auth),admin(auth));}

    private UUID id(Authentication auth){return CurrentUser.id(auth);} private boolean admin(Authentication auth){return CurrentUser.isPlatformAdmin(auth);}
    record TenantRequest(@NotBlank @Size(max=160) String name,@NotBlank @Size(max=80) String timezone){}
    record FormRequest(@NotBlank @Size(max=180) String name,@Size(max=4000) String description){}
}
