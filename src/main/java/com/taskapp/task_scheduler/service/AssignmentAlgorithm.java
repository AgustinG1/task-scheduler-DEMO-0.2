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

        payrollRepository.findByStatus(PayrollStatus.ACTIVE).ifPresent(p -> {
            p.setStatus(PayrollStatus.ARCHIVED);
            payrollRepository.save(p);
        });

        Payroll payroll = new Payroll();
        payroll.setTeam(team);
        payroll.setStatus(PayrollStatus.ACTIVE);
        payroll.setStartDate(LocalDate.now());
        payroll.setEndDate(LocalDate.now().plusWeeks(totalWeeks));
        payroll = payrollRepository.save(payroll);

        // 2. Intercalar áreas (Round-Robin) CON ALEATORIEDAD
        Map<Long, List<Employee>> porArea = empleadosRaw.stream().collect(Collectors.groupingBy(e -> e.getArea().getId()));
        List<Employee> empleados = new ArrayList<>();
        
        // SOLUCIÓN AL "SIEMPRE DA LO MISMO": Barajamos las listas de cada área antes de ordenarlas.
        for (List<Employee> lista : porArea.values()) {
            Collections.shuffle(lista); 
        }

        boolean quedanEmpleados;
        do {
            quedanEmpleados = false;
            for (List<Employee> listaArea : porArea.values()) {
                if (!listaArea.isEmpty()) {
                    empleados.add(listaArea.remove(0));
                    quedanEmpleados = true;
                }
            }
        } while (quedanEmpleados);

        Map<Long, Long> ultimaTareaPorEmpleado = new HashMap<>();
        Map<Long, Set<Long>> tareasRealizadasPorEmpleado = new HashMap<>();
        
        int trabajadoresPorSemana = Math.min(empleados.size(), tareas.size());
        int descansanCount = Math.max(1, empleados.size() - trabajadoresPorSemana);

        for (int semana = 1; semana <= totalWeeks; semana++) {
            
            int indiceInicio = ((semana - 1) * descansanCount) % empleados.size();
            List<Employee> ordenSemana = new ArrayList<>();
            for (int i = 0; i < empleados.size(); i++) {
                ordenSemana.add(empleados.get((indiceInicio + i) % empleados.size()));
            }

            List<Employee> disponiblesParaAsignar = new ArrayList<>(ordenSemana.subList(0, trabajadoresPorSemana));
            List<Employee> descansan = new ArrayList<>(ordenSemana.subList(trabajadoresPorSemana, ordenSemana.size()));

            List<Task> tareasPendientes = new ArrayList<>(tareas);
            // SOLUCIÓN AL "SIEMPRE DA LO MISMO": Barajamos las tareas antes de evaluarlas
            Collections.shuffle(tareasPendientes);

            while (!tareasPendientes.isEmpty() && !disponiblesParaAsignar.isEmpty()) {
                
                // 🔥 NUEVA INTELIGENCIA DINÁMICA: Ordenar priorizando la protección del Ciclo
                tareasPendientes.sort((t1, t2) -> {
                    // Contamos cuántos candidatos "Ideales" (que NO la han hecho en su ciclo) le quedan a cada tarea
                    long ideales1 = disponiblesParaAsignar.stream()
                        .filter(emp -> isAuthorized(emp, t1))
                        .filter(emp -> !ultimaTareaPorEmpleado.getOrDefault(emp.getId(), -1L).equals(t1.getId()))
                        .filter(emp -> !tareasRealizadasPorEmpleado.getOrDefault(emp.getId(), new HashSet<>()).contains(t1.getId()))
                        .count();
                        
                    long ideales2 = disponiblesParaAsignar.stream()
                        .filter(emp -> isAuthorized(emp, t2))
                        .filter(emp -> !ultimaTareaPorEmpleado.getOrDefault(emp.getId(), -1L).equals(t2.getId()))
                        .filter(emp -> !tareasRealizadasPorEmpleado.getOrDefault(emp.getId(), new HashSet<>()).contains(t2.getId()))
                        .count();
                    
                    // Si no hay empate de ideales, asignamos PRIMERO la tarea que se esté quedando sin gente nueva
                    if (ideales1 != ideales2) {
                        return Long.compare(ideales1, ideales2);
                    }
                    
                    // DESEMPATE DE SEGURIDAD (Para evitar huecos): Si empatan, miramos los autorizados totales
                    long validos1 = disponiblesParaAsignar.stream().filter(emp -> isAuthorized(emp, t1)).count();
                    long validos2 = disponiblesParaAsignar.stream().filter(emp -> isAuthorized(emp, t2)).count();
                    if (validos1 != validos2) {
                        return Long.compare(validos1, validos2);
                    }
                    
                    if (t1.getType() == TaskType.SPECIFIC && t2.getType() != TaskType.SPECIFIC) return -1;
                    if (t1.getType() != TaskType.SPECIFIC && t2.getType() == TaskType.SPECIFIC) return 1;
                    return 0;
                });

                Task tarea = tareasPendientes.remove(0);
                
                List<Employee> autorizados = disponiblesParaAsignar.stream()
                        .filter(emp -> isAuthorized(emp, tarea))
                        .collect(Collectors.toList());

                if (autorizados.isEmpty()) continue;

                // SOLUCIÓN ANTI-CONSECUTIVOS Y CICLO DE TAREAS (Filtro de 3 capas)
                
                // Capa 1: Los ideales (No la hizo la semana pasada Y no la ha hecho en su ciclo actual)
                List<Employee> ideales = autorizados.stream()
                        .filter(emp -> !ultimaTareaPorEmpleado.getOrDefault(emp.getId(), -1L).equals(tarea.getId()))
                        .filter(emp -> !tareasRealizadasPorEmpleado.getOrDefault(emp.getId(), new HashSet<>()).contains(tarea.getId()))
                        .collect(Collectors.toList());

                List<Employee> candidatosFinales;
                if (!ideales.isEmpty()) {
                    candidatosFinales = ideales;
                } else {
                    // Capa 2: Si todos ya la hicieron en el ciclo, buscamos al menos a los que NO la hicieron la semana pasada.
                    List<Employee> sinConsecutivos = autorizados.stream()
                            .filter(emp -> !ultimaTareaPorEmpleado.getOrDefault(emp.getId(), -1L).equals(tarea.getId()))
                            .collect(Collectors.toList());
                    
                    // Capa 3: Fallback de emergencia (Solo ocurre si la matemática obliga a repetir para no dejar el hueco)
                    candidatosFinales = sinConsecutivos.isEmpty() ? autorizados : sinConsecutivos;
                }

                // Para evitar predictibilidad si hay empates
                Collections.shuffle(candidatosFinales);

                // LÓGICA DE LA LISTA DE CHEQUEO ORIGINAL: El que tenga la lista más vacía elige primero
                candidatosFinales.sort((e1, e2) -> {
                    Set<Long> hist1 = tareasRealizadasPorEmpleado.getOrDefault(e1.getId(), new HashSet<>());
                    Set<Long> hist2 = tareasRealizadasPorEmpleado.getOrDefault(e2.getId(), new HashSet<>());
                    return Integer.compare(hist1.size(), hist2.size());
                });

                Employee elegido = candidatosFinales.get(0);

                Assignment asignacion = new Assignment();
                asignacion.setEmployee(elegido);
                asignacion.setPayroll(payroll);
                asignacion.setWeekNumber(semana);
                asignacion.setTask(tarea);
                asignacion.setStatus(AssignmentStatus.ASSIGNED);
                assignmentRepository.save(asignacion);

                // --- ACTUALIZAR EL CICLO DE TAREAS ORIGINAL ---
                ultimaTareaPorEmpleado.put(elegido.getId(), tarea.getId());
                
                Set<Long> historial = tareasRealizadasPorEmpleado.getOrDefault(elegido.getId(), new HashSet<>());
                historial.add(tarea.getId());
                
                long totalAutorizadas = tareas.stream().filter(t -> isAuthorized(elegido, t)).count();
                if (historial.size() >= totalAutorizadas) {
                    historial.clear();
                }
                
                tareasRealizadasPorEmpleado.put(elegido.getId(), historial);
                disponiblesParaAsignar.remove(elegido);
            }

            for (Employee emp : disponiblesParaAsignar) {
                assignmentRepository.save(asignarDescanso(emp, payroll, semana));
            }
            for (Employee emp : descansan) {
                assignmentRepository.save(asignarDescanso(emp, payroll, semana));
            }
        }

        return payroll;
    }

    private boolean isAuthorized(Employee emp, Task task) {
        if (task.getType() == TaskType.GENERAL) return true;
        if (task.getType() == TaskType.SPECIFIC) {
            return task.getAuthorizedAreas().stream()
                    .anyMatch(ta -> ta.getArea().getId().equals(emp.getArea().getId()));
        }
        return false;
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