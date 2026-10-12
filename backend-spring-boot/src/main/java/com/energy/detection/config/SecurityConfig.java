package com.energy.detection.config;

import com.energy.detection.security.JwtAuthFilter;
import com.energy.detection.security.LoginRateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuração de Segurança com Spring Security + JWT:
 * - Autenticação baseada em papéis (RBAC): ADMIN e OPERATOR
 * - Sessão STATELESS: cada pedido leva o token JWT no cabeçalho Authorization
 * - Administradores (Genserico, Oxidia): acesso total, incluindo alertas críticos e gestão de dados
 * - Operadores (Leysa, Helen): acesso de consulta geral aos transformadores
 * - Passwords provenientes do ambiente e protegidas com BCrypt
 * - CORS habilitado para comunicação com o frontend React
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    @org.springframework.beans.factory.annotation.Value("${EDM_CORS_ORIGIN:http://localhost:3000}")
    private String corsOrigin;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Cadastro de utilizadores em memória (adequado para a demo com H2;
     * numa evolução para produção, isto passaria a vir da tabela `utilizador` via JPA).
     *
     * Administradores: Genserico, Oxidia
     * Operadores: Leysa, Helen
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails genserico = User.builder()
                .username("genserico")
                .password(passwordEncoder.encode(requiredPassword("EDM_ADMIN_PASSWORD")))
                .authorities("ROLE_ADMIN", "VIEW_ALERTS", "MANAGE_ENERGY_DATA", "AUDIT_SECTORS")
                .build();

        UserDetails oxidia = User.builder()
                .username("oxidia")
                .password(passwordEncoder.encode(requiredPassword("EDM_ADMIN_PASSWORD")))
                .authorities("ROLE_ADMIN", "VIEW_ALERTS", "MANAGE_ENERGY_DATA", "AUDIT_SECTORS")
                .build();

        UserDetails leysa = User.builder()
                .username("leysa")
                .password(passwordEncoder.encode(requiredPassword("EDM_OPERATOR_PASSWORD")))
                .authorities("ROLE_OPERATOR", "VIEW_ENERGY_DATA")
                .build();

        UserDetails helen = User.builder()
                .username("helen")
                .password(passwordEncoder.encode(requiredPassword("EDM_OPERATOR_PASSWORD")))
                .authorities("ROLE_OPERATOR", "VIEW_ENERGY_DATA")
                .build();

        return new InMemoryUserDetailsManager(genserico, oxidia, leysa, helen);
    }

    // As passwords são definidas fora do repositório para evitar credenciais públicas.
    private static String requiredPassword(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável de ambiente obrigatória: " + key);
        }
        return value;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'; base-uri 'none'")))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> response.sendError(401))
                .accessDeniedHandler((request, response, exception) -> response.sendError(403)))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Rotas públicas (login e H2 Console, útil para inspecionar dados durante a demo)
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers("/error").permitAll()

                // Regra do Administrador: Apenas ADMIN pode visualizar alertas críticos e gerenciar dados
                .requestMatchers("/api/energy/alerts").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/energy/transformers/*/simulate-theft").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/energy/transformers/*/normalize").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/energy/transformers/**").hasRole("ADMIN")

                // Rotas de leitura acessíveis por ADMIN e OPERADOR
                .requestMatchers("/api/energy/**").hasAnyRole("ADMIN", "OPERATOR")

                .anyRequest().authenticated()
            )
            .addFilterBefore(new LoginRateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(corsOrigin));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
