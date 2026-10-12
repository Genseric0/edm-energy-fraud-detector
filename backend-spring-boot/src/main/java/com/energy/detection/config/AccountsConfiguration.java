package com.energy.detection.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

/** Contas transitórias do protótipo. Cada conta exige uma credencial distinta.
 * A persistência e administração de utilizadores em BD continuam pendentes.
 */
@Configuration
public class AccountsConfiguration {
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public UserDetailsService userDetailsService(Environment environment, PasswordEncoder encoder) {
        Map<String, String> passwords = new LinkedHashMap<>();
        for (String username : new String[]{"genserico", "oxidia", "leysa", "helen"}) {
            String key = "EDM_" + username.toUpperCase(java.util.Locale.ROOT) + "_PASSWORD";
            String password = environment.getProperty(key);
            // BCrypt só processa até 72 bytes; recusar truncagem silenciosa.
            if (password == null || password.isBlank() || password.length() < 12
                    || password.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalStateException(key + " deve conter pelo menos 12 caracteres e no máximo 72 bytes UTF-8");
            }
            passwords.put(username, password);
        }
        if (new HashSet<>(passwords.values()).size() != passwords.size()) {
            throw new IllegalStateException("Cada conta deve ter uma password diferente");
        }
        var manager = new InMemoryUserDetailsManager();
        passwords.forEach((username, password) -> {
            boolean admin = username.equals("genserico") || username.equals("oxidia");
            String[] authorities = admin
                ? new String[]{"ROLE_ADMIN", "VIEW_ALERTS", "MANAGE_ENERGY_DATA", "AUDIT_SECTORS"}
                : new String[]{"ROLE_OPERATOR", "VIEW_ENERGY_DATA"};
            manager.createUser(User.withUsername(username).password(encoder.encode(password))
                .authorities(authorities).build());
        });
        return manager;
    }
}
