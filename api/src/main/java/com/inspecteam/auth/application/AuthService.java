package com.inspecteam.auth.application;

import com.inspecteam.auth.domain.UserAccount;
import com.inspecteam.auth.infrastructure.AuthJdbcRepository;
import com.inspecteam.shared.exception.ApiException;
import com.inspecteam.shared.security.JwtService;
import com.inspecteam.shared.security.SecurityProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthJdbcRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecurityProperties properties;

    public AuthService(AuthJdbcRepository repository, PasswordEncoder passwordEncoder,
            JwtService jwtService, SecurityProperties properties) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
    }

    @Transactional
    public AuthResult registerTenant(RegisterTenantCommand command) {
        try {
            UUID userId = repository.insertUser(
                    command.email().trim(), passwordEncoder.encode(command.password()), command.displayName().trim());
            UUID tenantId = repository.insertTenant(command.tenantName().trim(), command.tenantSlug().trim());
            UUID membershipId = repository.insertOwnerMembership(tenantId, userId);
            TokenPair tokens = createTokenPair(userId, false, command.deviceName());
            return new AuthResult(userId, tenantId, membershipId, tokens);
        } catch (DuplicateKeyException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "E-mail ou identificador do tenant já está em uso");
        }
    }

    @Transactional
    public AuthResult login(LoginCommand command) {
        UserAccount user = repository.findByEmail(command.email().trim())
                .filter(account -> "ACTIVE".equals(account.status()))
                .filter(account -> passwordEncoder.matches(command.password(), account.passwordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas"));
        TokenPair tokens = createTokenPair(user.id(), user.platformAdmin(), command.deviceName());
        return new AuthResult(user.id(), null, null, tokens);
    }

    @Transactional
    public TokenPair refresh(String refreshToken, String deviceName) {
        var session = repository.findRefreshSession(hash(refreshToken))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token inválido ou expirado"));
        repository.revokeRefreshToken(session.id());
        return createTokenPair(session.userId(), session.platformAdmin(), deviceName);
    }

    @Transactional
    public void logout(String refreshToken) {
        repository.findRefreshSession(hash(refreshToken)).ifPresent(session -> repository.revokeRefreshToken(session.id()));
    }

    private TokenPair createTokenPair(UUID userId, boolean platformAdmin, String deviceName) {
        JwtService.AccessToken accessToken = jwtService.issue(userId, platformAdmin);
        String refreshToken = randomToken();
        Instant refreshExpiresAt = Instant.now().plus(properties.refreshTokenTtl());
        repository.storeRefreshToken(userId, hash(refreshToken), refreshExpiresAt, deviceName);
        return new TokenPair(accessToken.value(), accessToken.expiresAt(), refreshToken, refreshExpiresAt);
    }

    private static String randomToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record RegisterTenantCommand(String tenantName, String tenantSlug, String displayName,
            String email, String password, String deviceName) {
    }

    public record LoginCommand(String email, String password, String deviceName) {
    }

    public record AuthResult(UUID userId, UUID tenantId, UUID membershipId, TokenPair tokens) {
    }

    public record TokenPair(String accessToken, Instant accessTokenExpiresAt,
            String refreshToken, Instant refreshTokenExpiresAt) {
    }
}
