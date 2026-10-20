package com.energy.detection.controller;

import com.energy.detection.dto.*;
import com.energy.detection.model.AppUser;
import com.energy.detection.security.AccountAuthenticationService;
import com.energy.detection.service.AccountAdministrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/admin/accounts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AccountAdministrationController {
    private final AccountAdministrationService accounts;
    private final AccountAuthenticationService attempts;

    @GetMapping
    public AdminAccountView.Page list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return accounts.list(page, size);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable long id, @Valid @RequestBody AdminAccountUpdate input, Authentication auth) {
        AppUser actor = (AppUser) auth.getPrincipal();
        return mutate(actor, () -> accounts.update(actor, id, input.expectedVersion(), input.role(), input.active(), input.reason(), input.currentPassword()));
    }

    @PostMapping("/{id}/revoke-sessions")
    public ResponseEntity<?> revoke(@PathVariable long id, @Valid @RequestBody AdminAccountAction input, Authentication auth) {
        AppUser actor = (AppUser) auth.getPrincipal();
        return mutate(actor, () -> accounts.act(actor, id, input.expectedVersion(), input.reason(), input.currentPassword(), false));
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<?> unlock(@PathVariable long id, @Valid @RequestBody AdminAccountAction input, Authentication auth) {
        AppUser actor = (AppUser) auth.getPrincipal();
        return mutate(actor, () -> accounts.act(actor, id, input.expectedVersion(), input.reason(), input.currentPassword(), true));
    }

    private ResponseEntity<?> mutate(AppUser actor, Supplier<AdminAccountView> operation) {
        try {
            return ResponseEntity.ok(operation.get());
        } catch (CredentialsExpiredException ex) {
            return ResponseEntity.status(401).body(Map.of("message", "Sessão inválida. Entre novamente."));
        } catch (BadCredentialsException ex) {
            attempts.recordFailure(actor.getUsername());
            return ResponseEntity.badRequest().body(Map.of("message", "Não foi possível confirmar a password actual."));
        }
    }
}
