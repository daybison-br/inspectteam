package com.inspecteam.admin.api;
import com.inspecteam.admin.application.AdminService;import com.inspecteam.shared.security.CurrentUser;import jakarta.validation.Valid;import jakarta.validation.constraints.Pattern;import java.util.Map;import java.util.UUID;import org.springframework.http.HttpStatus;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin") public class AdminController{
 private final AdminService service; public AdminController(AdminService service){this.service=service;}
 private void check(Authentication a){service.require(CurrentUser.isPlatformAdmin(a));}
 @GetMapping("/overview") AdminService.Overview overview(Authentication a){check(a);return service.overview();}
 @GetMapping("/tenants") AdminService.Page<AdminService.TenantView> tenants(@RequestParam(defaultValue="")String query,@RequestParam(defaultValue="")String status,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size,Authentication a){check(a);return service.listTenants(query,status,page,size);}
 @GetMapping("/users") AdminService.Page<AdminService.UserView> users(@RequestParam(defaultValue="")String query,@RequestParam(defaultValue="")String status,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size,Authentication a){check(a);return service.listUsers(query,status,page,size);}
 @PatchMapping("/tenants/{id}/status") @ResponseStatus(HttpStatus.NO_CONTENT) void tenantStatus(@PathVariable UUID id,@Valid @RequestBody Status r,Authentication a){check(a);service.tenantStatus(CurrentUser.id(a),id,r.status());}
 @PatchMapping("/users/{id}/status") @ResponseStatus(HttpStatus.NO_CONTENT) void userStatus(@PathVariable UUID id,@Valid @RequestBody UserStatus r,Authentication a){check(a);service.userStatus(CurrentUser.id(a),id,r.status());}
 @PutMapping("/users/{id}/platform-admin") @ResponseStatus(HttpStatus.NO_CONTENT) void admin(@PathVariable UUID id,@RequestBody AdminFlag r,Authentication a){check(a);service.platformAdmin(CurrentUser.id(a),id,r.enabled());}
 @PostMapping("/tenants/{id}/enter") Map<String,UUID> enter(@PathVariable UUID id,Authentication a){check(a);return Map.of("membershipId",service.enter(CurrentUser.id(a),id));}
 record Status(@Pattern(regexp="ACTIVE|SUSPENDED|ARCHIVED")String status){} record UserStatus(@Pattern(regexp="ACTIVE|SUSPENDED")String status){} record AdminFlag(boolean enabled){}
}
