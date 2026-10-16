package com.taskapp.task_scheduler.repository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.taskapp.task_scheduler.model.Employee;

import java.util.List;


@Repository
public interface EmployeeRepository extends OwnedRepository<Employee> {
    
    // Método 1: Traer todos los empleados de un área específica.
    // Spring genera: SELECT * FROM empleados WHERE area_id = ?
    List<Employee> findByAreaId(Long areaId);
    
    // Método 2: Traer solo los empleados activos de un área.
    // Agregamos "And" para unir condiciones, y "True" porque la variable active es un boolean.
    // Spring genera: SELECT * FROM empleados WHERE area_id = ? AND active = true
    List<Employee> findByAreaIdAndActiveTrue(Long areaId);

    // Traer a todos los empleados activos sin importar el área
    List<Employee> findByActiveTrue();

    long countByActiveTrue();

    List<Employee> findByOwner_IdAndActiveTrue(Long ownerId);
    List<Employee> findByOwner_IdAndArea_Id(Long ownerId, Long areaId);
    long countByOwner_IdAndActiveTrue(Long ownerId);

}
