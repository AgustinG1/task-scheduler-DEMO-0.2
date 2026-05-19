package com.taskapp.task_scheduler.repository;

import com.taskapp.task_scheduler.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    // Spring Data JPA nos regala el save(), findAll(), deleteById(), etc.
}