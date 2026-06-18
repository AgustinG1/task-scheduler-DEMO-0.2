package com.taskapp.task_scheduler.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.taskapp.task_scheduler.model.Payroll;
import com.taskapp.task_scheduler.model.PayrollStatus;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {
    // Devuelve todas las planillas con el estado indicado.
    List<Payroll> findByStatus(PayrollStatus status);
}
