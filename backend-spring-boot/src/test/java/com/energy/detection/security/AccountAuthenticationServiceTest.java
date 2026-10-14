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
        new AccountAuthenticationService(users, Clock.fixed(NOW, ZoneOffset.UTC)).recordSuccess(account());
        assertEquals(0, account.getFailedAttempts());
        assertNull(account.getLockedUntil());
    }

    @Test void unknownUsernameDoesNotExposeOrCreateAccount() {
        AppUserRepository users = mock(AppUserRepository.class);
        when(users.findByUsernameForUpdate("unknown")).thenReturn(Optional.empty());
        new AccountAuthenticationService(users, Clock.fixed(NOW, ZoneOffset.UTC)).recordFailure("unknown");
        verify(users, never()).save(any());
    }

    @Test void expiredLockStartsNewFailureWindowAtExactBoundary() {
        AppUserRepository users = mock(AppUserRepository.class);
        AppUser account = account();
        account.setFailedAttempts(5);
        account.setLockedUntil(NOW);
        when(users.findByUsernameForUpdate("operator")).thenReturn(Optional.of(account));
        new AccountAuthenticationService(users, Clock.fixed(NOW, ZoneOffset.UTC)).recordFailure("operator");
        assertEquals(1, account.getFailedAttempts());
        assertNull(account.getLockedUntil());
    }

    @Test void failedRequestsDoNotExtendActiveLock() {
        AppUserRepository users = mock(AppUserRepository.class);
        AppUser account = account();
        account.setFailedAttempts(5);
        account.setLockedUntil(NOW.plusSeconds(20));
        when(users.findByUsernameForUpdate("operator")).thenReturn(Optional.of(account));
        new AccountAuthenticationService(users, Clock.fixed(NOW, ZoneOffset.UTC)).recordFailure("operator");
        assertEquals(5, account.getFailedAttempts());
        assertEquals(NOW.plusSeconds(20), account.getLockedUntil());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"locked", "disabled", "password", "version", "recreated", "deleted"})
    void staleAuthenticationCannotClearNewSecurityState(String change) {
        AppUserRepository users = mock(AppUserRepository.class);
        AppUser current = account();
        current.setFailedAttempts(4);
        switch (change) {
            case "locked" -> current.setLockedUntil(NOW.plusSeconds(900));
            case "disabled" -> current.setActive(false);
            case "password" -> current.setPasswordHash("new-hash");
            case "version" -> current.setAuthVersion(1);
            case "recreated" -> current.setId(99L);
        }
        when(users.findByUsernameForUpdate("operator"))
            .thenReturn(change.equals("deleted") ? Optional.empty() : Optional.of(current));
        var service = new AccountAuthenticationService(users, Clock.fixed(NOW, ZoneOffset.UTC));
        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
            () -> service.recordSuccess(account()));
        assertEquals(4, current.getFailedAttempts());
    }

    @Test void successReturnsCurrentRoleRatherThanStalePrincipal() {
        AppUserRepository users = mock(AppUserRepository.class);
        AppUser current = account();
        AppUser old = account();
        old.setRole(AccountRole.ADMIN);
        when(users.findByUsernameForUpdate("operator")).thenReturn(Optional.of(current));
        var result = new AccountAuthenticationService(users, Clock.fixed(NOW, ZoneOffset.UTC)).recordSuccess(old);
        assertSame(current, result);
        assertEquals(AccountRole.OPERATOR, result.getRole());
    }
}
