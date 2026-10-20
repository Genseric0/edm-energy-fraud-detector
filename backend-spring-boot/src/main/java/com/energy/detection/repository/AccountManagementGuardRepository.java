package com.energy.detection.repository;

import com.energy.detection.model.AccountManagementGuard;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface AccountManagementGuardRepository extends JpaRepository<AccountManagementGuard, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select guard from AccountManagementGuard guard where guard.id = 1")
    Optional<AccountManagementGuard> lockAdministration();
}
