package com.energy.detection;

import com.energy.detection.config.AccountsConfiguration;
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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class AccountSecurityTest {
    private final JwtUtil jwt = new JwtUtil("unit-test-only-key-of-at-least-32-bytes", 900000);
    private final AccountsConfiguration accounts = new AccountsConfiguration();

    private MockEnvironment environment() {
        return new MockEnvironment()
            .withProperty("EDM_GENSERICO_PASSWORD", "test-genserico-password")
            .withProperty("EDM_OXIDIA_PASSWORD", "test-oxidia-password")
            .withProperty("EDM_LEYSA_PASSWORD", "test-leysa-password")
            .withProperty("EDM_HELEN_PASSWORD", "test-helen-password");
    }

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void independentPasswordsAreHashedAndRolesPreserved() {
        var encoder = accounts.passwordEncoder();
        var env = environment();
        var users = accounts.userDetailsService(env, encoder);
        for (String name : new String[]{"genserico", "oxidia", "leysa", "helen"}) {
            var user = users.loadUserByUsername(name);
            String password = env.getProperty("EDM_" + name.toUpperCase(java.util.Locale.ROOT) + "_PASSWORD");
            assertNotEquals(password, user.getPassword());
            assertTrue(encoder.matches(password, user.getPassword()));
            assertEquals(name.equals("genserico") || name.equals("oxidia"),
                user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        }
        assertFalse(encoder.matches(env.getProperty("EDM_GENSERICO_PASSWORD"),
            users.loadUserByUsername("oxidia").getPassword()));
    }

    @Test void sharedLegacyPasswordsDoNotStartApplication() {
        var legacy = new MockEnvironment().withProperty("EDM_ADMIN_PASSWORD", "legacy-admin-password")
            .withProperty("EDM_OPERATOR_PASSWORD", "legacy-operator-password");
        assertThrows(IllegalStateException.class, () -> accounts.userDetailsService(legacy, accounts.passwordEncoder()));
    }

    @Test void duplicatePasswordsAreRejected() {
        var env = environment().withProperty("EDM_OXIDIA_PASSWORD", "test-genserico-password");
        assertThrows(IllegalStateException.class, () -> accounts.userDetailsService(env, accounts.passwordEncoder()));
    }

    @ParameterizedTest @ValueSource(strings = {"", "short", "            "})
    void invalidPasswordsAreRejectedWithoutDisclosingValue(String value) {
        var env = environment().withProperty("EDM_HELEN_PASSWORD", value);
        var error = assertThrows(IllegalStateException.class,
            () -> accounts.userDetailsService(env, accounts.passwordEncoder()));
        assertTrue(error.getMessage().contains("EDM_HELEN_PASSWORD"));
    }

    @Test void bcryptByteLimitIncludesMultibyteCharacters() {
        var env = environment().withProperty("EDM_HELEN_PASSWORD", "é".repeat(37));
        assertThrows(IllegalStateException.class, () -> accounts.userDetailsService(env, accounts.passwordEncoder()));
    }

    private void authenticate(InMemoryUserDetailsManager users, String token) throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/energy/alerts");
        request.addHeader("Authorization", "Bearer " + token);
        var continued = new AtomicBoolean();
        new JwtAuthFilter(jwt, users).doFilter(request, new MockHttpServletResponse(),
            (req, res) -> continued.set(true));
        assertTrue(continued.get());
    }

    @Test void tokenCannotRetainOldAdminRole() throws Exception {
        var admin = User.withUsername("account").password("hash").roles("ADMIN").build();
        String token = jwt.gerarToken(admin);
        var users = new InMemoryUserDetailsManager(admin);
        users.updateUser(User.withUsername("account").password("hash").roles("OPERATOR").build());
        authenticate(users, token);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("account", auth.getName());
        assertEquals(java.util.List.of("ROLE_OPERATOR"),
            auth.getAuthorities().stream().map(a -> a.getAuthority()).toList());
    }

    @ParameterizedTest @ValueSource(strings = {"disabled", "locked", "expired", "credentialsExpired", "deleted"})
    void unavailableAccountsCannotReuseValidToken(String state) throws Exception {
        var original = User.withUsername("account").password("hash").roles("ADMIN").build();
        String token = jwt.gerarToken(original);
        var users = new InMemoryUserDetailsManager(original);
        if (state.equals("deleted")) users.deleteUser("account");
        else users.updateUser(User.withUsername("account").password("hash").roles("ADMIN")
            .disabled(state.equals("disabled")).accountLocked(state.equals("locked"))
            .accountExpired(state.equals("expired")).credentialsExpired(state.equals("credentialsExpired")).build());
        authenticate(users, token);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test void invalidSignatureCannotAuthenticateAnExistingAccount() throws Exception {
        var account = User.withUsername("account").password("hash").roles("ADMIN").build();
        var foreignJwt = new JwtUtil("another-test-key-with-at-least-32-bytes", 900000);
        authenticate(new InMemoryUserDetailsManager(account), foreignJwt.gerarToken(account));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
