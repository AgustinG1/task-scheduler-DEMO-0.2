package com.taskapp.task_scheduler.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;

import com.taskapp.task_scheduler.model.Employee;
import com.taskapp.task_scheduler.repository.EmployeeRepository;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    // 1. Traer todos los empleados
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    // 2. Buscar empleado por ID
    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: No se encontró el empleado con ID " + id));
    }

    // 3. Traer empleados de un área específica (Usando el método que creaste en el Repositorio)
    public List<Employee> getEmployeesByArea(Long areaId) {
        return employeeRepository.findByAreaId(areaId);
    }

    // 4. Crear un nuevo empleado
    public Employee createEmployee(Employee employee) {
        // Por defecto, al crear a alguien, debería estar activo
        employee.setActive(true);
        return employeeRepository.save(employee);
    }

    // 5. Actualizar datos del empleado
    public Employee updateEmployee(Long id, Employee empleadoActualizado) {
        Employee empleadoExistente = getEmployeeById(id);
        
        // Actualizamos los campos (nombre, apellido y área)
        empleadoExistente.setName(empleadoActualizado.getName());
        empleadoExistente.setApellido(empleadoActualizado.getApellido());
        empleadoExistente.setArea(empleadoActualizado.getArea());
        
        return employeeRepository.save(empleadoExistente);
    }

    // 6. Desactivar empleado (Borrado Lógico)
    // NO usamos employeeRepository.delete() para no romper las planillas históricas
    public void deactivateEmployee(Long id) {
        Employee empleado = getEmployeeById(id);
        empleado.setActive(false); // Simplemente le quitamos el estado activo
        employeeRepository.save(empleado); // Guardamos el cambio
    }
    
    public void deleteEmployee(Long id) {
    Employee employee = getEmployeeById(id);
    employeeRepository.delete(employee);
}
}