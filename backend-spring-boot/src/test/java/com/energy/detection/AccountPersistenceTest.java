package com.energy.detection;

import com.energy.detection.model.AppUser;
import com.energy.detection.security.AccountAuthenticationService;
import com.energy.detection.security.DatabaseUserDetailsService;
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

/** Transacções reais, sem rollback do teste e sem substituição por H2. */
@Testcontainers
@ActiveProfiles("postgres")
@SpringBootTest(properties = {
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

    @BeforeEach void resetSyntheticAccount() {
        jdbc.update("UPDATE app_user SET failed_attempts=0, locked_until=NULL, active=true WHERE username='helen'");
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
}
