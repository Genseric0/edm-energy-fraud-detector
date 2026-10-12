package com.energy.detection.controller;

import com.energy.detection.dto.LoginRequestDTO;
import com.energy.detection.dto.LoginResponseDTO;
import com.energy.detection.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller REST para autenticação de utilizadores.
 * Emite um token JWT no login, que o frontend deve reenviar em
 * "Authorization: Bearer {token}" em todos os pedidos subsequentes.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    /**
     * Nomes de exibição amigáveis para os utilizadores cadastrados.
     * Mantido aqui (e não no SecurityConfig) para não misturar dados de autenticação
     * com dados de apresentação.
     */
    private static final Map<String, String> NOMES_EXIBICAO = Map.of(
            "genserico", "Genserico",
            "oxidia", "Oxidia",
            "leysa", "Leysa",
            "helen", "Helen"
    );

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            List<String> authorities = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            boolean isAdmin = authorities.contains("ROLE_ADMIN");
            String role = isAdmin ? "ROLE_ADMIN" : "ROLE_OPERATOR";
            String username = authentication.getName();
            String displayName = NOMES_EXIBICAO.getOrDefault(username, username);

            UserDetails userDetails = User.builder()
                    .username(username)
                    .password("")
                    .authorities(authentication.getAuthorities())
                    .build();
            String token = jwtUtil.gerarToken(userDetails);

            LoginResponseDTO response = LoginResponseDTO.builder()
                    .authenticated(true)
                    .token(token)
                    .username(username)
                    .name(displayName)
                    .role(role)
                    .authorities(authorities)
                    .message("Autenticação realizada com sucesso!")
                    .build();

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    LoginResponseDTO.builder()
                            .authenticated(false)
                            .message("Credenciais inválidas. Verifique usuário e senha.")
                            .build()
            );
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuário não autenticado");
        }

        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        boolean isAdmin = authorities.contains("ROLE_ADMIN");
        String username = authentication.getName();
        String displayName = NOMES_EXIBICAO.getOrDefault(username, username);

        return ResponseEntity.ok(
                LoginResponseDTO.builder()
                        .authenticated(true)
                        .username(username)
                        .name(displayName)
                        .role(isAdmin ? "ROLE_ADMIN" : "ROLE_OPERATOR")
                        .authorities(authorities)
                        .message("Sessão ativa")
                        .build()
        );
    }
}
