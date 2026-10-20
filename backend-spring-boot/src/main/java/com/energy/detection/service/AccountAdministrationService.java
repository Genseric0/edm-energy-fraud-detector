package com.energy.detection.service;

import com.energy.detection.controller.AccountConflictException;
import com.energy.detection.controller.ResourceNotFoundException;
import com.energy.detection.dto.AdminAccountView;
import com.energy.detection.model.*;
import com.energy.detection.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Objects;

@Service
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AccountAdministrationService {
    private final AppUserRepository users;
    private final AccountManagementGuardRepository guards;
    private final AccountSecurityEventRepository events;
    private final PasswordEncoder encoder;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AdminAccountView.Page list(int page, int size) {
        if (page < 0 || page > 100000 || size < 1 || size > 100) throw new IllegalArgumentException();
        var result = users.findAll(PageRequest.of(page, size, Sort.by("id")));
        return new AdminAccountView.Page(result.getContent().stream().map(AdminAccountView::from).toList(),
            page, result.getTotalPages(), result.getTotalElements());
    }

    @Transactional
    public AdminAccountView update(AppUser principal, long id, long expectedVersion, AccountRole role,
                                   boolean active, String reason, String currentPassword) {
        AppUser actor = authorizeMutation(principal, currentPassword, reason);
        AppUser target = target(id, expectedVersion);
        AccountRole oldRole = target.getRole();
        boolean oldActive = target.isActive();
        if (oldRole == role && oldActive == active) throw new AccountConflictException("Não há alterações para guardar.");
        if (oldRole == AccountRole.ADMIN && oldActive && (role != AccountRole.ADMIN || !active)
                && users.countByRoleAndActiveTrue(AccountRole.ADMIN) <= 1) {
            throw new AccountConflictException("É necessário manter pelo menos um administrador activo.");
        }
        target.setRole(role);
        target.setActive(active);
        audit(actor, target, "ACCOUNT_UPDATED", reason, oldRole, oldActive);
        return AdminAccountView.from(target);
    }

    @Transactional
    public AdminAccountView act(AppUser principal, long id, long expectedVersion, String reason,
                                String currentPassword, boolean unlock) {
        AppUser actor = authorizeMutation(principal, currentPassword, reason);
        AppUser target = target(id, expectedVersion);
        if (unlock) {
            if (target.getLockedUntil() == null && target.getFailedAttempts() == 0)
                throw new AccountConflictException("A conta não tem bloqueio ou tentativas para limpar.");
            target.setLockedUntil(null);
            target.setFailedAttempts(0);
        }
        audit(actor, target, unlock ? "ACCOUNT_UNLOCKED" : "SESSIONS_REVOKED", reason, target.getRole(), target.isActive());
        return AdminAccountView.from(target);
    }

    private AppUser authorizeMutation(AppUser principal, String password, String reason) {
        if (reason == null || reason.trim().length() < 10 || reason.length() > 500) throw new IllegalArgumentException();
        // Linha independente evita que duas contas administrativas se desactivem
        // mutuamente com snapshots antigas e garante a regra do último ADMIN.
        guards.lockAdministration().orElseThrow(() -> new AccountConflictException("Administração indisponível. Contacte o responsável."));
        AppUser actor = users.findByUsernameForUpdate(principal.getUsername())
            .orElseThrow(() -> new CredentialsExpiredException("Sessão inválida"));
        if (!Objects.equals(actor.getId(), principal.getId()) || actor.getAuthVersion() != principal.getAuthVersion()
                || !actor.isActive() || actor.getRole() != AccountRole.ADMIN
                || actor.getLockedUntil() != null && actor.getLockedUntil().isAfter(clock.instant()))
            throw new CredentialsExpiredException("Sessão inválida");
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length > 72 || !encoder.matches(password, actor.getPassword()))
            throw new BadCredentialsException("Credenciais inválidas");
        return actor;
    }

    private AppUser target(long id, long expectedVersion) {
        AppUser account = users.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));
        if (account.getAuthVersion() != expectedVersion)
            throw new AccountConflictException("A conta foi alterada. Actualize os dados antes de tentar novamente.");
        return account;
    }

    private void audit(AppUser actor, AppUser target, String action, String reason, AccountRole oldRole, boolean oldActive) {
        target.setAuthVersion(Math.addExact(target.getAuthVersion(), 1));
        events.save(AccountSecurityEvent.builder().accountId(target.getId()).actorId(actor.getId())
            .occurredAt(clock.instant()).action(action).authVersion(target.getAuthVersion()).reason(reason.trim())
            .oldRole(oldRole).newRole(target.getRole()).oldActive(oldActive).newActive(target.isActive()).build());
    }
}
