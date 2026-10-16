package com.taskapp.task_scheduler.repository;

import java.util.Optional;
import com.taskapp.task_scheduler.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByIdentityKey(String identityKey);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from AppUser u where u.id = :id")
    Optional<AppUser> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}
