package com.energy.detection.security;

import com.energy.detection.model.AppUser;
import com.energy.detection.model.AccountSecurityEvent;
import com.energy.detection.repository.AppUserRepository;
import com.energy.detection.repository.AccountSecurityEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PasswordChangeService {
    private final AppUserRepository users;
    private final AccountSecurityEventRepository events;
    private final PasswordEncoder encoder;
    private final Clock clock;

    @Transactional
    public void change(AppUser principal, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 12
                || newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password inválida");
        }
        AppUser account = users.findByUsernameForUpdate(principal.getUsername())
            .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));
        if (!account.isActive() || account.getLockedUntil() != null && account.getLockedUntil().isAfter(clock.instant())
                || !Objects.equals(account.getId(), principal.getId())
                || account.getAuthVersion() != principal.getAuthVersion()
                || currentPassword == null || currentPassword.getBytes(StandardCharsets.UTF_8).length > 72
                || !encoder.matches(currentPassword, account.getPassword())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }
        if (encoder.matches(newPassword, account.getPassword())) {
            throw new IllegalArgumentException("Escolha uma password diferente");
        }
        long nextVersion = Math.addExact(account.getAuthVersion(), 1L);
        account.setPasswordHash(encoder.encode(newPassword));
        account.setAuthVersion(nextVersion);
        account.setFailedAttempts(0);
        account.setLockedUntil(null);
        events.save(AccountSecurityEvent.builder().accountId(account.getId()).occurredAt(clock.instant())
            .action("PASSWORD_CHANGED").actorId(account.getId()).authVersion(nextVersion).build());
    }
}
