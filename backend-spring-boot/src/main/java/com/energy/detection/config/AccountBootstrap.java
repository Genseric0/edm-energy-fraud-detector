package com.energy.detection.config;

import com.energy.detection.model.AccountRole;
import com.energy.detection.model.AppUser;
import com.energy.detection.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Cria as quatro contas iniciais apenas quando a tabela ainda está vazia. */
@Component
@RequiredArgsConstructor
public class AccountBootstrap implements ApplicationRunner {
    private static final List<String> USERNAMES = List.of("genserico", "oxidia", "leysa", "helen");
    private static final Map<String, String> DISPLAY_NAMES = Map.of(
        "genserico", "Genserico", "oxidia", "Oxidia", "leysa", "Leysa", "helen", "Helen");

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final Environment environment;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) return;

        Map<String, String> passwords = readPasswords(environment);
        users.saveAll(USERNAMES.stream().map(username -> AppUser.builder()
            .username(username)
            .displayName(DISPLAY_NAMES.get(username))
            .passwordHash(encoder.encode(passwords.get(username)))
            .role(username.equals("genserico") || username.equals("oxidia")
                ? AccountRole.ADMIN : AccountRole.OPERATOR)
            .active(true)
            .build()).toList());
    }

    static Map<String, String> readPasswords(Environment environment) {
        Map<String, String> passwords = new LinkedHashMap<>();
        for (String username : USERNAMES) {
            String key = "EDM_" + username.toUpperCase(Locale.ROOT) + "_PASSWORD";
            String password = environment.getProperty(key);
            if (password == null || password.isBlank() || password.length() < 12
                    || password.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalStateException(key + " deve conter pelo menos 12 caracteres e no máximo 72 bytes UTF-8");
            }
            passwords.put(username, password);
        }
        if (new HashSet<>(passwords.values()).size() != passwords.size()) {
            throw new IllegalStateException("Cada conta deve ter uma password diferente");
        }
        return passwords;
    }
}
