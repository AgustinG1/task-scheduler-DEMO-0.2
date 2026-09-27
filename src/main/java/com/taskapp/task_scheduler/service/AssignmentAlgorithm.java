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
        int pasoRotacion = Math.max(1, empleados.size() - trabajadoresPorSemana);

        for (int semana = 1; semana <= totalWeeks; semana++) {
            int indiceInicio = ((semana - 1) * pasoRotacion) % empleados.size();
            List<Employee> ordenSemana = new ArrayList<>();
            for (int i = 0; i < empleados.size(); i++) {
                ordenSemana.add(empleados.get((indiceInicio + i) % empleados.size()));
            }

            List<Task> tareasPendientes = new ArrayList<>(tareas);
            Collections.shuffle(tareasPendientes);
            // Priorizar tareas con pocos candidatos ideales, sin adelantar una tarea
            // que obligaría a repetir cuando todavía existe una alternativa de ciclo.
            tareasPendientes.sort(Comparator
                    .comparingInt((Task tarea) -> prioridadTarea(tarea, ordenSemana,
                            ultimaTareaPorEmpleado, tareasRealizadasPorEmpleado))
                    .thenComparingLong(tarea -> cantidadCandidatosIdeales(tarea, ordenSemana,
                            ultimaTareaPorEmpleado, tareasRealizadasPorEmpleado))
                    .thenComparingLong(tarea -> cantidadCandidatosAutorizados(tarea, ordenSemana))
                    .thenComparing(tarea -> tarea.getType() == TaskType.SPECIFIC ? 0 : 1));

            // Elegir descansos después de comprobar todas las personas autorizadas.
            // Si una tarea ocupa a la única persona de otra, reubicarla antes de dejar un hueco.
            Map<Long, Task> tareaPorEmpleado = new HashMap<>();
            for (Task tarea : tareasPendientes) {
                buscarAsignacion(tarea, ordenSemana, tareaPorEmpleado, new HashSet<>(),
                        ultimaTareaPorEmpleado, tareasRealizadasPorEmpleado);
            }

            for (Employee elegido : ordenSemana) {
                Task tarea = tareaPorEmpleado.get(elegido.getId());
                if (tarea == null) {
                    assignmentRepository.save(asignarDescanso(elegido, payroll, semana));
                    continue;
                }
                Assignment asignacion = new Assignment();
                asignacion.setEmployee(elegido);
                asignacion.setPayroll(payroll);
                asignacion.setWeekNumber(semana);
                asignacion.setTask(tarea);
                asignacion.setStatus(AssignmentStatus.ASSIGNED);
                assignmentRepository.save(asignacion);

                ultimaTareaPorEmpleado.put(elegido.getId(), tarea.getId());
                Set<Long> historial = tareasRealizadasPorEmpleado.getOrDefault(elegido.getId(), new HashSet<>());
                historial.add(tarea.getId());
                long totalAutorizadas = tareas.stream().filter(t -> isAuthorized(elegido, t)).count();
                if (historial.size() >= totalAutorizadas) {
                    historial.clear();
                }
                tareasRealizadasPorEmpleado.put(elegido.getId(), historial);
            }
        }

        return payroll;
    }

    private int prioridadTarea(Task tarea, List<Employee> empleados,
            Map<Long, Long> ultimaTareaPorEmpleado,
            Map<Long, Set<Long>> tareasRealizadasPorEmpleado) {
        long autorizados = cantidadCandidatosAutorizados(tarea, empleados);
        if (autorizados == 0) return 2;

        long ideales = cantidadCandidatosIdeales(tarea, empleados, ultimaTareaPorEmpleado,
                tareasRealizadasPorEmpleado);
        return ideales > 0 ? 0 : 1;
    }

    private long cantidadCandidatosIdeales(Task tarea, List<Employee> empleados,
            Map<Long, Long> ultimaTareaPorEmpleado,
            Map<Long, Set<Long>> tareasRealizadasPorEmpleado) {
        return empleados.stream()
                .filter(emp -> prioridadCandidato(emp, tarea, ultimaTareaPorEmpleado,
                        tareasRealizadasPorEmpleado) == 0)
                .count();
    }

    private long cantidadCandidatosAutorizados(Task tarea, List<Employee> empleados) {
        return empleados.stream().filter(emp -> isAuthorized(emp, tarea)).count();
    }

    private boolean buscarAsignacion(Task tarea, List<Employee> ordenSemana, Map<Long, Task> tareaPorEmpleado,
            Set<Long> visitados, Map<Long, Long> ultimaTareaPorEmpleado,
            Map<Long, Set<Long>> tareasRealizadasPorEmpleado) {
        List<Employee> candidatos = ordenSemana.stream().filter(emp -> isAuthorized(emp, tarea))
                .collect(Collectors.toCollection(ArrayList::new));
        candidatos.sort(Comparator
                .comparingInt((Employee emp) -> prioridadCandidato(emp, tarea, ultimaTareaPorEmpleado,
                        tareasRealizadasPorEmpleado))
                .thenComparingInt(emp -> tareasRealizadasPorEmpleado
                        .getOrDefault(emp.getId(), Collections.emptySet()).size())
                .thenComparingInt(ordenSemana::indexOf));

        for (Employee candidato : candidatos) {
            if (!visitados.add(candidato.getId())) continue;
            Task anterior = tareaPorEmpleado.get(candidato.getId());
            if (anterior == null || buscarAsignacion(anterior, ordenSemana, tareaPorEmpleado, visitados,
                    ultimaTareaPorEmpleado, tareasRealizadasPorEmpleado)) {
                tareaPorEmpleado.put(candidato.getId(), tarea);
                return true;
            }
        }
        return false;
    }

    private int prioridadCandidato(Employee emp, Task tarea, Map<Long, Long> ultimaTareaPorEmpleado,
            Map<Long, Set<Long>> tareasRealizadasPorEmpleado) {
        if (!isAuthorized(emp, tarea)) return 3;
        if (ultimaTareaPorEmpleado.getOrDefault(emp.getId(), -1L).equals(tarea.getId())) return 2;
        if (tareasRealizadasPorEmpleado.getOrDefault(emp.getId(), Collections.emptySet()).contains(tarea.getId())) {
            return 1;
        }
        return 0;
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
