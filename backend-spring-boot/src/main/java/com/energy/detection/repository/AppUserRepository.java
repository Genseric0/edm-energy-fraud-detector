package com.energy.detection.repository;

import com.energy.detection.model.AppUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    long countByRoleAndActiveTrue(com.energy.detection.model.AccountRole role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from AppUser account where account.id = :id")
    Optional<AppUser> findByIdForUpdate(@Param("id") Long id);
    Optional<AppUser> findByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from AppUser account where account.username = :username")
    Optional<AppUser> findByUsernameForUpdate(@Param("username") String username);
}
