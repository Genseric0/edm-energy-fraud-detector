package com.energy.detection.security;

import com.energy.detection.repository.AppUserRepository;
import com.energy.detection.model.AppUser;
import org.springframework.security.authentication.BadCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/** Estado persistente de tentativas por conta, complementar ao limite por IP. */
@Service
@RequiredArgsConstructor
public class AccountAuthenticationService {
    static final int MAX_FAILURES = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final AppUserRepository users;
    private final Clock clock;

    @Transactional
    public void recordFailure(String username) {
        users.findByUsernameForUpdate(normalize(username)).ifPresent(account -> {
            Instant now = clock.instant();
            if (!account.isActive() || account.getLockedUntil() != null && account.getLockedUntil().isAfter(now)) return;
            // Depois de cumprir a penalização, começa uma nova janela de tentativas.
            if (account.getLockedUntil() != null) {
                account.setFailedAttempts(0);
                account.setLockedUntil(null);
            }
            int attempts = account.getFailedAttempts() + 1;
            account.setFailedAttempts(attempts);
            if (attempts >= MAX_FAILURES) account.setLockedUntil(now.plus(LOCK_DURATION));
        });
    }

    @Transactional
    public AppUser recordSuccess(AppUser authenticated) {
        AppUser account = users.findByUsernameForUpdate(normalize(authenticated.getUsername()))
            .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));
        // A verificação BCrypt ocorre antes deste lock. Revalidar o estado para não
        // desfazer um bloqueio ou aceitar credenciais revogadas durante essa verificação.
        if (!account.isActive()
                || account.getLockedUntil() != null && account.getLockedUntil().isAfter(clock.instant())
                || !Objects.equals(account.getId(), authenticated.getId())
                || !Objects.equals(account.getPassword(), authenticated.getPassword())
                || account.getAuthVersion() != authenticated.getAuthVersion()) {
            throw new BadCredentialsException("Credenciais inválidas");
        }
        account.setFailedAttempts(0);
        account.setLockedUntil(null);
        return account;
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
