package com.energy.detection.model;

import jakarta.persistence.*;
import lombok.*;

/** Uma linha estável serializa transições administrativas entre contas. */
@Entity
@Table(name = "account_management_guard")
@Getter @NoArgsConstructor @AllArgsConstructor
public class AccountManagementGuard {
    @Id private Long id;
}
