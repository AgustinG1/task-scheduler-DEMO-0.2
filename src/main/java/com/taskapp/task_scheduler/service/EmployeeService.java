package com.taskapp.task_scheduler.service;

import java.util.List;
import com.taskapp.task_scheduler.model.Employee;
import com.taskapp.task_scheduler.repository.AreaRepository;
import com.taskapp.task_scheduler.repository.EmployeeRepository;
import com.taskapp.task_scheduler.security.OwnerAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final AreaRepository areaRepository;
    private final OwnerAccess owners;

    public List<Employee> getAllEmployees() {
        Long ownerId = owners.id();
        var result = employeeRepository.findByOwner_IdAndActiveTrue(ownerId);
        result.forEach(e -> owners.employee(e, ownerId));
        return result;
    }

    public Employee getEmployeeById(Long id) {
        Long ownerId = owners.id();
        Employee employee = owners.require(employeeRepository.findByIdAndOwner_Id(id, ownerId));
        owners.employee(employee, ownerId);
        return employee;
    }

    public List<Employee> getEmployeesByArea(Long areaId) {
        Long ownerId = owners.id();
        owners.require(areaRepository.findByIdAndOwner_Id(areaId, ownerId));
        var result = employeeRepository.findByOwner_IdAndArea_Id(ownerId, areaId);
        result.forEach(e -> owners.employee(e, ownerId));
        return result;
    }

    @Transactional
    public Employee createEmployee(Employee input) {
        Long ownerId = owners.id();
        var owner = owners.lock(ownerId);
        var area = owners.require(areaRepository.findByIdAndOwner_Id(
                input.getArea() == null ? null : input.getArea().getId(), ownerId));
        Employee employee = new Employee();
        employee.setName(input.getName());
        employee.setApellido(input.getApellido());
        employee.setArea(area);
        employee.setActive(true);
        employee.setOwner(owner);
        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee updateEmployee(Long id, Employee input) {
        Long ownerId = owners.id();
        owners.lock(ownerId);
        Employee employee = owners.require(employeeRepository.findByIdAndOwner_Id(id, ownerId));
        var area = owners.require(areaRepository.findByIdAndOwner_Id(
                input.getArea() == null ? null : input.getArea().getId(), ownerId));
        employee.setName(input.getName());
        employee.setApellido(input.getApellido());
        employee.setArea(area);
        return employeeRepository.save(employee);
    }

    @Transactional
    public void deactivateEmployee(Long id) {
        Long ownerId = owners.id();
        owners.lock(ownerId);
        Employee employee = owners.require(employeeRepository.findByIdAndOwner_Id(id, ownerId));
        employee.setActive(false);
        employeeRepository.save(employee);
    }
}