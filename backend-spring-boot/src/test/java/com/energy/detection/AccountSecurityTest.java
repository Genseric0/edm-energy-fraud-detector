package com.energy.detection;

import com.energy.detection.config.AccountBootstrap;
import com.energy.detection.config.AccountsConfiguration;
import com.energy.detection.model.AccountRole;
import com.energy.detection.model.AppUser;
import com.energy.detection.repository.AppUserRepository;
import com.energy.detection.security.JwtAuthFilter;
import com.energy.detection.security.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountSecurityTest {
    private final JwtUtil jwt = new JwtUtil("unit-test-only-key-of-at-least-32-bytes", 900000);
    private final PasswordEncoder encoder = new AccountsConfiguration().passwordEncoder();

    private MockEnvironment environment() {
        return new MockEnvironment()
            .withProperty("EDM_GENSERICO_PASSWORD", "test-genserico-password")
            .withProperty("EDM_OXIDIA_PASSWORD", "test-oxidia-password")
            .withProperty("EDM_LEYSA_PASSWORD", "test-leysa-password")
            .withProperty("EDM_HELEN_PASSWORD", "test-helen-password");
    }

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void initialAccountsArePersistedWithIndependentHashesAndRoles() throws Exception {
        AppUserRepository users = mock(AppUserRepository.class);
        when(users.count()).thenReturn(0L);
        new AccountBootstrap(users, encoder, environment()).run(null);
        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(Iterable.class);
        verify(users).saveAll(captor.capture());
        List<AppUser> saved = new java.util.ArrayList<>();
        captor.getValue().forEach(item -> saved.add((AppUser) item));
        assertEquals(4, saved.size());
        for (AppUser account : saved) {
            String clear = environment().getProperty("EDM_" + account.getUsername().toUpperCase(java.util.Locale.ROOT) + "_PASSWORD");
            assertNotEquals(clear, account.getPassword());
            assertTrue(encoder.matches(clear, account.getPassword()));
            assertEquals(account.getUsername().equals("genserico") || account.getUsername().equals("oxidia"), account.getRole() == AccountRole.ADMIN);
        }
        assertNotEquals(saved.get(0).getPassword(), saved.get(1).getPassword());
    }

    @Test void existingDatabaseAccountsAreNeverOverwrittenByBootstrap() throws Exception {
        AppUserRepository users = mock(AppUserRepository.class);
        when(users.count()).thenReturn(1L);
        new AccountBootstrap(users, encoder, new MockEnvironment()).run(null);
        verify(users, never()).saveAll(any());
    }

    @Test void sharedLegacyPasswordsDoNotStartEmptyDatabase() {
        var legacy = new MockEnvironment().withProperty("EDM_ADMIN_PASSWORD", "legacy-admin-password")
            .withProperty("EDM_OPERATOR_PASSWORD", "legacy-operator-password");
        assertBootstrapFails(legacy);
    }

    @Test void duplicatePasswordsAreRejected() {
        assertBootstrapFails(environment().withProperty("EDM_OXIDIA_PASSWORD", "test-genserico-password"));
    }

    @ParameterizedTest @ValueSource(strings = {"", "short", "            "})
    void invalidPasswordsAreRejectedWithoutDisclosingValue(String value) {
        var error = assertThrows(IllegalStateException.class,
            () -> bootstrap(environment().withProperty("EDM_HELEN_PASSWORD", value)));
        assertTrue(error.getMessage().contains("EDM_HELEN_PASSWORD"));
    }

    @Test void bcryptByteLimitIncludesMultibyteCharacters() {
        assertBootstrapFails(environment().withProperty("EDM_HELEN_PASSWORD", "é".repeat(37)));
    }

    private void bootstrap(MockEnvironment environment) throws Exception {
        AppUserRepository users = mock(AppUserRepository.class);
        when(users.count()).thenReturn(0L);
        new AccountBootstrap(users, encoder, environment).run(null);
    }

    private void assertBootstrapFails(MockEnvironment environment) {
        assertThrows(IllegalStateException.class, () -> bootstrap(environment));
    }

    private AppUser account(AccountRole role) {
        return AppUser.builder().username("account").displayName("Account").passwordHash("hash")
            .role(role).active(true).build();
    }

    private void authenticate(UserDetailsService users, String token) throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/energy/alerts");
        request.addHeader("Authorization", "Bearer " + token);
        var continued = new AtomicBoolean();
        new JwtAuthFilter(jwt, users).doFilter(request, new MockHttpServletResponse(),
            (req, res) -> continued.set(true));
        assertTrue(continued.get());
    }

    @Test void tokenCannotRetainOldAdminRole() throws Exception {
        AppUser account = account(AccountRole.ADMIN);
        String token = jwt.gerarToken(account);
        account.setRole(AccountRole.OPERATOR);
        authenticate(username -> account, token);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(List.of("ROLE_OPERATOR", "VIEW_ENERGY_DATA"),
            auth.getAuthorities().stream().map(a -> a.getAuthority()).toList());
    }

    @Test void authenticationVersionInvalidatesPreviouslyIssuedToken() throws Exception {
        AppUser account = account(AccountRole.ADMIN);
        String token = jwt.gerarToken(account);
        account.setAuthVersion(1);
        authenticate(username -> account, token);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @ParameterizedTest @ValueSource(strings = {"disabled", "locked", "deleted"})
    void unavailableAccountsCannotReuseValidToken(String state) throws Exception {
        AppUser account = account(AccountRole.ADMIN);
        String token = jwt.gerarToken(account);
        UserDetailsService users = username -> account;
        if (state.equals("disabled")) account.setActive(false);
        if (state.equals("locked")) account.setLockedUntil(Instant.now().plusSeconds(600));
        if (state.equals("deleted")) users = username -> { throw new UsernameNotFoundException("deleted"); };
        authenticate(users, token);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test void invalidSignatureCannotAuthenticateAnExistingAccount() throws Exception {
        AppUser account = account(AccountRole.ADMIN);
        var foreignJwt = new JwtUtil("another-test-key-with-at-least-32-bytes", 900000);
        authenticate(username -> account, foreignJwt.gerarToken(account));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
