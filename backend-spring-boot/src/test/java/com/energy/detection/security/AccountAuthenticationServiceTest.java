package com.energy.detection.security;

import com.energy.detection.model.AccountRole;
import com.energy.detection.model.AppUser;
import com.energy.detection.repository.AppUserRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountAuthenticationServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-13T18:00:00Z");

    private AppUser account() {
        return AppUser.builder().username("operator").displayName("Operator").passwordHash("hash")
            .role(AccountRole.OPERATOR).active(true).build();
    }

    @Test void fifthFailureLocksAccountForFifteenMinutes() {
        AppUserRepository users = mock(AppUserRepository.class);
        AppUser account = account();
        when(users.findByUsernameForUpdate("operator")).thenReturn(Optional.of(account));
        var service = new AccountAuthenticationService(users, Clock.fixed(NOW, ZoneOffset.UTC));
        for (int i = 0; i < 5; i++) service.recordFailure(" Operator ");
        assertEquals(5, account.getFailedAttempts());
        assertEquals(NOW.plusSeconds(900), account.getLockedUntil());
    }

    @Test void successClearsOldFailureState() {
        AppUserRepository users = mock(AppUserRepository.class);
        AppUser account = account();
        account.setFailedAttempts(4);
        account.setLockedUntil(NOW.minusSeconds(1));
        when(users.findByUsernameForUpdate("operator")).thenReturn(Optional.of(account));
        new AccountAuthenticationService(users, Clock.fixed(NOW, ZoneOffset.UTC)).recordSuccess("operator");
        assertEquals(0, account.getFailedAttempts());
        assertNull(account.getLockedUntil());
    }

    @Test void unknownUsernameDoesNotExposeOrCreateAccount() {
        AppUserRepository users = mock(AppUserRepository.class);
        when(users.findByUsernameForUpdate("unknown")).thenReturn(Optional.empty());
        new AccountAuthenticationService(users, Clock.fixed(NOW, ZoneOffset.UTC)).recordFailure("unknown");
        verify(users, never()).save(any());
    }
}
