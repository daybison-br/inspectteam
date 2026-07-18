package com.inspecteam.account.api;

import com.inspecteam.account.application.AccountService;
import com.inspecteam.account.application.AccountService.AccountView;
import com.inspecteam.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping
    AccountView get(Authentication authentication) {
        return service.get(CurrentUser.id(authentication));
    }

    @PatchMapping
    AccountView update(@Valid @RequestBody UpdateAccountRequest request, Authentication authentication) {
        return service.update(CurrentUser.id(authentication), request.displayName());
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        service.changePassword(CurrentUser.id(authentication), request.currentPassword(), request.newPassword());
    }

    record UpdateAccountRequest(@NotBlank @Size(max = 160) String displayName) { }
    record ChangePasswordRequest(@NotBlank String currentPassword,
            @NotBlank @Size(min = 10, max = 128) String newPassword) { }
}
