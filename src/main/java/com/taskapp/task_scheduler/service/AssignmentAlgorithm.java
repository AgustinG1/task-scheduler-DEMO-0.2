package com.taskapp.task_scheduler.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import com.taskapp.task_scheduler.model.*;
import com.taskapp.task_scheduler.repository.*;

@Service
@RequiredArgsConstructor
public class AssignmentAlgorithm {

    private final FeasibilityValidator validator;
    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final PayrollRepository payrollRepository;
    private final AssignmentRepository assignmentRepository;
    private final TeamRepository teamRepository; // Agregado para el Equipo

    @Transactional
    public Payroll generatePayroll(int totalWeeks, Long teamId) {
        if (totalWeeks < 1 || totalWeeks > 52) {
            throw new IllegalArgumentException("La cantidad de semanas debe estar entre 1 y 52.");
        }

        // 1. Filtrar por Equipo
        Team team = teamRepository.findById(teamId).orElseThrow();
        List<Employee> empleadosRaw = employeeRepository.findByActiveTrue().stream()
                .filter(emp -> team.getAreas() != null && team.getAreas().contains(emp.getArea()))
                .collect(Collectors.toList());

        List<Task> tareas = new ArrayList<>();
        if (team.getTaskGroups() != null) {
            for (TaskGroup tg : team.getTaskGroups()) {
                tareas.addAll(tg.getTasks());
            }
        }
        tareas = tareas.stream().distinct().collect(Collectors.toList());

        validator.validar(empleadosRaw, tareas);

        payrollRepository.findByStatus(PayrollStatus.ACTIVE).forEach(p -> {
            p.setStatus(PayrollStatus.ARCHIVED);
            payrollRepository.save(p);
        });

        Payroll payroll = new Payroll();
        payroll.setTeam(team);
        payroll.setStatus(PayrollStatus.ACTIVE);
        payroll.setStartDate(LocalDate.now());
        payroll.setEndDate(LocalDate.now().plusWeeks(totalWeeks));
        payroll = payrollRepository.save(payroll);

        List<Map<Long, Task>> plan = new RotationPlanner().plan(empleadosRaw, tareas, totalWeeks, new Random());
        for (int semana = 1; semana <= totalWeeks; semana++) {
            Map<Long, Task> tareasSemana = plan.get(semana - 1);
            for (Employee empleado : empleadosRaw) {
                Task tarea = tareasSemana.get(empleado.getId());
                if (tarea == null) {
                    assignmentRepository.save(asignarDescanso(empleado, payroll, semana));
                    continue;
                }
                Assignment asignacion = new Assignment();
                asignacion.setEmployee(empleado);
                asignacion.setPayroll(payroll);
                asignacion.setWeekNumber(semana);
                asignacion.setTask(tarea);
                asignacion.setStatus(AssignmentStatus.ASSIGNED);
                assignmentRepository.save(asignacion);
            }
        }
        return payroll;
    }

    private Assignment asignarDescanso(Employee employee, Payroll payroll, int semana) {
        Assignment asignacionDescanso = new Assignment();
        asignacionDescanso.setEmployee(employee);
        asignacionDescanso.setPayroll(payroll);
        asignacionDescanso.setWeekNumber(semana);
        asignacionDescanso.setTask(null);
        asignacionDescanso.setStatus(AssignmentStatus.REST);
        return asignacionDescanso;
    }
}
