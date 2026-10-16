package com.energy.detection;

import com.energy.detection.model.AppUser;
import com.energy.detection.security.AccountAuthenticationService;
import com.energy.detection.security.DatabaseUserDetailsService;
import com.energy.detection.security.PasswordChangeService;
import com.energy.detection.security.JwtUtil;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Transacções reais, sem rollback do teste e sem substituição por H2. */
@Testcontainers
@ActiveProfiles("postgres")
@AutoConfigureMockMvc
@SpringBootTest(properties = {
    "app.demo.seed-enabled=false",
    "JWT_SECRET=test-only-persistence-key-at-least-32-bytes",
    "EDM_GENSERICO_PASSWORD=test-genserico-password",
    "EDM_OXIDIA_PASSWORD=test-oxidia-password",
    "EDM_LEYSA_PASSWORD=test-leysa-password",
    "EDM_HELEN_PASSWORD=test-helen-password"
})
class AccountPersistenceTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired AccountAuthenticationService attempts;
    @Autowired DatabaseUserDetailsService users;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired PasswordChangeService passwords;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtUtil jwt;
    @Autowired ApplicationContext context;

    @BeforeEach void resetSyntheticAccount() {
        jdbc.update("UPDATE app_user SET failed_attempts=0, locked_until=NULL, active=true, password_hash=?, auth_version=auth_version+1 WHERE username='helen'",
            encoder.encode("test-helen-password"));
    }

    @Test void concurrentFailuresAreCommittedWithoutLostUpdates() throws Exception {
        var executor = Executors.newFixedThreadPool(5);
        var start = new CountDownLatch(1);
        var jobs = new ArrayList<Future<?>>();
        try {
            for (int i = 0; i < 5; i++) jobs.add(executor.submit(() -> {
                assertTrue(start.await(10, TimeUnit.SECONDS));
                attempts.recordFailure("helen");
                return null;
            }));
            start.countDown();
            for (var job : jobs) job.get(20, TimeUnit.SECONDS);
            AppUser account = (AppUser) users.loadUserByUsername("helen");
            assertEquals(5, account.getFailedAttempts());
            assertFalse(account.isAccountNonLocked());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test void loginSnapshotCannotUndoSubsequentCommittedLock() {
        AppUser snapshot = (AppUser) users.loadUserByUsername("helen");
        for (int i = 0; i < 5; i++) attempts.recordFailure("helen");
        assertThrows(BadCredentialsException.class, () -> attempts.recordSuccess(snapshot));
        AppUser current = (AppUser) users.loadUserByUsername("helen");
        assertEquals(5, current.getFailedAttempts());
        assertFalse(current.isAccountNonLocked());
    }

    @Test void expiredLockRecoversAndSuccessfulLoginPersistsReset() {
        jdbc.update("UPDATE app_user SET failed_attempts=5, locked_until=CURRENT_TIMESTAMP - INTERVAL '1 second' WHERE username='helen'");
        attempts.recordFailure("helen");
        AppUser snapshot = (AppUser) users.loadUserByUsername("helen");
        assertEquals(1, snapshot.getFailedAttempts());
        assertNull(snapshot.getLockedUntil());
        attempts.recordSuccess(snapshot);
        AppUser reloaded = (AppUser) users.loadUserByUsername("helen");
        assertEquals(0, reloaded.getFailedAttempts());
        assertNull(reloaded.getLockedUntil());
    }

    private AppUser helen() { return (AppUser) users.loadUserByUsername("helen"); }
    private long eventCount() {
        return jdbc.queryForObject("SELECT count(*) FROM account_security_event WHERE account_id=?", Long.class, helen().getId());
    }
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder changeRequest(String token, String body) {
        return post("/api/auth/password").header("Authorization", "Bearer " + token)
            .with(request -> { request.setRemoteAddr(java.util.UUID.randomUUID().toString()); return request; })
            .contentType("application/json").content(body);
    }

    @Test void syntheticGridIsAbsentUnlessExplicitlyEnabled() {
        assertFalse(context.containsBean("dataSeeder"));
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM transformers", Long.class));
    }

    @Test void operatorRotatesOwnPasswordAndRevokesOldTokensWithoutAffectingOtherAccounts() throws Exception {
        AppUser previous = helen();
        String oldToken = jwt.gerarToken(previous);
        String otherToken = jwt.gerarToken((AppUser) users.loadUserByUsername("genserico"));
        long count = eventCount();
        mvc.perform(changeRequest(oldToken, "{\"currentPassword\":\"test-helen-password\",\"newPassword\":\"new-private-password\",\"username\":\"genserico\"}"))
            .andExpect(status().isNoContent()).andExpect(content().string(""));
        AppUser changed = helen();
        assertEquals(previous.getAuthVersion() + 1, changed.getAuthVersion());
        assertTrue(encoder.matches("new-private-password", changed.getPassword()));
        assertFalse(encoder.matches("test-helen-password", changed.getPassword()));
        assertEquals(count + 1, eventCount());
        assertEquals("PASSWORD_CHANGED", jdbc.queryForObject("SELECT action FROM account_security_event WHERE account_id=? AND auth_version=?",
            String.class, changed.getId(), changed.getAuthVersion()));
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + oldToken)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + otherToken)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType("application/json")
            .content("{\"username\":\"helen\",\"password\":\"new-private-password\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.token").isString());
        mvc.perform(post("/api/auth/login").contentType("application/json")
            .content("{\"username\":\"helen\",\"password\":\"test-helen-password\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test void anonymousCannotRotatePasswords() throws Exception {
        mvc.perform(post("/api/auth/password").contentType("application/json")
            .content("{\"currentPassword\":\"test-helen-password\",\"newPassword\":\"new-private-password\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test void wrongCurrentPasswordPersistsFailuresAndLocksTheAccount() throws Exception {
        AppUser previous = helen();
        String token = jwt.gerarToken(previous);
        long count = eventCount();
        for (int i = 0; i < 5; i++) {
            mvc.perform(changeRequest(token, "{\"currentPassword\":\"wrong-password\",\"newPassword\":\"new-private-password\"}"))
                .andExpect(status().isBadRequest());
        }
        assertEquals(5, helen().getFailedAttempts());
        assertFalse(helen().isAccountNonLocked());
        assertEquals(previous.getPassword(), helen().getPassword());
        assertEquals(previous.getAuthVersion(), helen().getAuthVersion());
        assertEquals(count, eventCount());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test void weakUnchangedAndOversizedUtf8PasswordsDoNotChangeAccountOrAudit() throws Exception {
        AppUser previous = helen();
        String token = jwt.gerarToken(previous);
        long count = eventCount();
        for (String invalid : java.util.List.of("short", "test-helen-password", "é".repeat(37))) {
            mvc.perform(changeRequest(token, "{\"currentPassword\":\"test-helen-password\",\"newPassword\":\"" + invalid + "\"}"))
                .andExpect(status().isBadRequest());
        }
        assertEquals(previous.getPassword(), helen().getPassword());
        assertEquals(previous.getAuthVersion(), helen().getAuthVersion());
        assertEquals(count, eventCount());
        assertEquals(0, helen().getFailedAttempts());
    }

    @Test void concurrentRotationAllowsExactlyOneSnapshotToCommit() throws Exception {
        AppUser first = helen();
        AppUser second = helen();
        long count = eventCount();
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var results = new ArrayList<Future<Boolean>>();
            for (AppUser snapshot : java.util.List.of(first, second)) results.add(executor.submit(() -> {
                assertTrue(start.await(10, TimeUnit.SECONDS));
                try {
                    passwords.change(snapshot, "test-helen-password", "new-private-password");
                    return true;
                } catch (BadCredentialsException expected) { return false; }
            }));
            start.countDown();
            int successes = 0;
            for (var result : results) if (result.get(20, TimeUnit.SECONDS)) successes++;
            assertEquals(1, successes);
            assertEquals(first.getAuthVersion() + 1, helen().getAuthVersion());
            assertEquals(count + 1, eventCount());
        } finally { executor.shutdownNow(); }
    }

    @Test void auditFailureRollsBackPasswordAndVersionInSameTransaction() {
        AppUser previous = helen();
        // Força conflito de auditoria sem substituir o repositório/BD por mocks.
        jdbc.update("INSERT INTO account_security_event(account_id,occurred_at,action,auth_version) VALUES (?,CURRENT_TIMESTAMP,'PASSWORD_CHANGED',?)",
            previous.getId(), previous.getAuthVersion() + 1);
        assertThrows(DataIntegrityViolationException.class, () -> passwords.change(previous, "test-helen-password", "new-private-password"));
        assertEquals(previous.getPassword(), helen().getPassword());
        assertEquals(previous.getAuthVersion(), helen().getAuthVersion());
    }

    @Test void auditRowsRejectUpdateAndDeleteOnPostgres() {
        AppUser previous = helen();
        passwords.change(previous, "test-helen-password", "new-private-password");
        assertThrows(DataAccessException.class, () -> jdbc.update("UPDATE account_security_event SET action='PASSWORD_CHANGED' WHERE account_id=?", previous.getId()));
        assertThrows(DataAccessException.class, () -> jdbc.update("DELETE FROM account_security_event WHERE account_id=?", previous.getId()));
    }
}
