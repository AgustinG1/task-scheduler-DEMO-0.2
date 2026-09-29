package com.taskapp.task_scheduler.service;

import com.taskapp.task_scheduler.model.Employee;
import com.taskapp.task_scheduler.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTests {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void listsOnlyActiveEmployees() {
        Employee active = new Employee();
        active.setId(1L);
        active.setActive(true);
        when(employeeRepository.findByActiveTrue()).thenReturn(List.of(active));

        assertEquals(List.of(active), employeeService.getAllEmployees());

        verify(employeeRepository).findByActiveTrue();
        verify(employeeRepository, never()).findAll();
    }

    @Test
    void deactivatesEmployeeWithoutDeletingHistoricalReferences() {
        Employee employee = new Employee();
        employee.setId(5L);
        employee.setActive(true);
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));

        employeeService.deactivateEmployee(5L);

        assertFalse(employee.isActive());
        verify(employeeRepository).save(employee);
        verify(employeeRepository, never()).delete(employee);
        verify(employeeRepository, never()).deleteById(5L);
    }
}
