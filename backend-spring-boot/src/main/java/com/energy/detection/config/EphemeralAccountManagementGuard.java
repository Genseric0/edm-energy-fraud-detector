package com.energy.detection.config;

import com.energy.detection.model.AccountManagementGuard;
import com.energy.detection.repository.AccountManagementGuardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** H2 create-drop não executa Flyway. No PostgreSQL a linha vem exclusivamente de V5. */
@Component
@Profile("!postgres")
@RequiredArgsConstructor
public class EphemeralAccountManagementGuard implements ApplicationRunner {
    private final AccountManagementGuardRepository guards;
    @Override @Transactional
    public void run(ApplicationArguments arguments) {
        if (!guards.existsById(1L)) guards.save(new AccountManagementGuard(1L));
    }
}
