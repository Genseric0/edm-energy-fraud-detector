package com.energy.detection.controller;

import com.energy.detection.dto.PasswordChangeRequest;
import com.energy.detection.model.AppUser;
import com.energy.detection.security.AccountAuthenticationService;
import com.energy.detection.security.PasswordChangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
public class PasswordChangeController {
    private final PasswordChangeService passwords;
    private final AccountAuthenticationService attempts;

    @PostMapping
    public ResponseEntity<?> change(@Valid @RequestBody PasswordChangeRequest request, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser account)) {
            return ResponseEntity.status(401).build();
        }
        try {
            // A identidade vem da sessão; nenhum ID/username do corpo pode escolher outra conta.
            passwords.change(account, request.currentPassword(), request.newPassword());
            return ResponseEntity.noContent().build();
        } catch (AuthenticationException exception) {
            // Fora da transacção revertida, para que falhas sejam persistidas.
            attempts.recordFailure(account.getUsername());
            return ResponseEntity.badRequest().body(Map.of("message", "Não foi possível alterar a password. Verifique a password actual."));
        }
    }
}
