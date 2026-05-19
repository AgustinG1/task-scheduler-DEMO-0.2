package com.taskapp.task_scheduler.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.taskapp.task_scheduler.model.Payroll;
import com.taskapp.task_scheduler.model.PayrollStatus;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {
// Devuelve la planilla con el estado indicado, si existe.

    Optional<Payroll> findByStatus(PayrollStatus status);
}
