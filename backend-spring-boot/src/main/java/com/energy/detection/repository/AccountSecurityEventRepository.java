package com.energy.detection.repository;

import com.energy.detection.model.AccountSecurityEvent;
import org.springframework.data.repository.Repository;

/** Sem operações de edição/remoção na aplicação. */
public interface AccountSecurityEventRepository extends Repository<AccountSecurityEvent, Long> {
    AccountSecurityEvent save(AccountSecurityEvent event);
}
