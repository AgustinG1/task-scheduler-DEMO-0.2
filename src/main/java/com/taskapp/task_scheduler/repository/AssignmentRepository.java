package com.taskapp.task_scheduler.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 

import com.taskapp.task_scheduler.model.Assignment;

 
@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    
    List<Assignment> findByPayrollId(Long payrollId);
    
    List<Assignment> findByEmployeeId(Long employeeId);
    
    // Método que agregamos para poder buscar turnos y evitar el Error 500 al borrar tareas
    List<Assignment> findByTaskId(Long taskId);
}