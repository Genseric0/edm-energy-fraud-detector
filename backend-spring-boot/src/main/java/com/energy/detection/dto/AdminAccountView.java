package com.energy.detection.dto;

import com.energy.detection.model.AppUser;
import com.energy.detection.model.AccountRole;
import java.time.Instant;
import java.util.List;

/** Projecção explícita: nunca serializar a entidade UserDetails/password_hash. */
public record AdminAccountView(Long id, String username, String displayName, AccountRole role,
                               boolean active, int failedAttempts, Instant lockedUntil, long version) {
    public static AdminAccountView from(AppUser account) {
        return new AdminAccountView(account.getId(), account.getUsername(), account.getDisplayName(),
            account.getRole(), account.isActive(), account.getFailedAttempts(), account.getLockedUntil(), account.getAuthVersion());
    }
    public record Page(List<AdminAccountView> accounts, int page, int totalPages, long totalElements) {}
}
