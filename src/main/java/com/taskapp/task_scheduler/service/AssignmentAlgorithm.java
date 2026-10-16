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
    private final com.taskapp.task_scheduler.security.OwnerAccess owners;

    @Transactional
    public Payroll generatePayroll(int totalWeeks, Long teamId) {
        if (totalWeeks < 1 || totalWeeks > 52) {
            throw new IllegalArgumentException("La cantidad de semanas debe estar entre 1 y 52.");
        }

        // 1. Filtrar por Equipo
        Long ownerId = owners.id();
        var owner = owners.lock(ownerId);
        Team team = owners.require(teamRepository.findByIdAndOwner_Id(teamId, ownerId));
        owners.team(team, ownerId);
        List<Employee> empleadosRaw = employeeRepository.findByOwner_IdAndActiveTrue(ownerId).stream()
                .filter(emp -> team.getAreas() != null && team.getAreas().contains(emp.getArea()))
                .collect(Collectors.toList());
        empleadosRaw.forEach(employee -> owners.employee(employee, ownerId));

        List<Task> tareas = new ArrayList<>();
        if (team.getTaskGroups() != null) {
            for (TaskGroup tg : team.getTaskGroups()) {
                tareas.addAll(tg.getTasks());
            }
        }
        tareas = tareas.stream().distinct().collect(Collectors.toList());

        validator.validar(empleadosRaw, tareas);

        payrollRepository.findByOwner_IdAndStatus(ownerId, PayrollStatus.ACTIVE).forEach(p -> {
            p.setStatus(PayrollStatus.ARCHIVED);
            payrollRepository.save(p);
        });

        Payroll payroll = new Payroll();
        payroll.setOwner(owner);
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
