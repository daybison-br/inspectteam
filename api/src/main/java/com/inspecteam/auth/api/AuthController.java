package com.inspecteam.auth.api;

import com.inspecteam.auth.application.AuthService;
import com.inspecteam.auth.application.AuthService.AuthResult;
import com.inspecteam.auth.application.AuthService.TokenPair;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register-tenant")
    @ResponseStatus(HttpStatus.CREATED)
    AuthResult registerTenant(@Valid @RequestBody RegisterTenantRequest request) {
        return service.registerTenant(new AuthService.RegisterTenantCommand(
                request.tenantName(), request.tenantSlug(), request.displayName(),
                request.email(), request.password(), request.deviceName()));
    }

    @PostMapping("/login")
    AuthResult login(@Valid @RequestBody LoginRequest request) {
        return service.login(new AuthService.LoginCommand(request.email(), request.password(), request.deviceName()));
    }

    @PostMapping("/refresh")
    TokenPair refresh(@Valid @RequestBody RefreshRequest request) {
        return service.refresh(request.refreshToken(), request.deviceName());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody LogoutRequest request) {
        service.logout(request.refreshToken());
    }

    record RegisterTenantRequest(
            @NotBlank @Size(max = 160) String tenantName,
            @NotBlank @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max = 80) String tenantSlug,
            @NotBlank @Size(max = 160) String displayName,
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 10, max = 128) String password,
            @Size(max = 200) String deviceName) {
    }

    record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            @Size(max = 200) String deviceName) {
    }

    record RefreshRequest(@NotBlank String refreshToken, @Size(max = 200) String deviceName) {
    }

    record LogoutRequest(@NotBlank String refreshToken) {
    }
}
