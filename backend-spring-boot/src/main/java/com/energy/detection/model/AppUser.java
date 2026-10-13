package com.energy.detection.model;

import com.energy.detection.security.VersionedUserDetails;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser implements UserDetails, VersionedUserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AccountRole role;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Builder.Default
    @Column(name = "auth_version", nullable = false)
    private long authVersion = 0;

    @Override public String getPassword() { return passwordHash; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == AccountRole.ADMIN) {
            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("VIEW_ALERTS"),
                new SimpleGrantedAuthority("MANAGE_ENERGY_DATA"),
                new SimpleGrantedAuthority("AUDIT_SECTORS"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_OPERATOR"),
            new SimpleGrantedAuthority("VIEW_ENERGY_DATA"));
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return active; }
    @Override public boolean isAccountNonLocked() {
        return lockedUntil == null || !lockedUntil.isAfter(Instant.now());
    }
}
