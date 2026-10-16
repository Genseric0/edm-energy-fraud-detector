package com.energy.detection.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import java.time.Instant;

/** Metadados mínimos da rotação; nunca passwords, hashes, tokens ou endereços IP. */
@Entity
@Table(name = "account_security_event")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AccountSecurityEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    @Column(nullable = false, length = 32)
    private String action;
    @Column(name = "auth_version", nullable = false)
    private long authVersion;
}
