package com.taskapp.task_scheduler.service;

import com.taskapp.task_scheduler.model.Area;
import com.taskapp.task_scheduler.model.Assignment;
import com.taskapp.task_scheduler.model.AssignmentStatus;
import com.taskapp.task_scheduler.model.Employee;
import com.taskapp.task_scheduler.model.Payroll;
import com.taskapp.task_scheduler.model.PayrollStatus;
import com.taskapp.task_scheduler.model.Task;
import com.taskapp.task_scheduler.model.TaskArea;
import com.taskapp.task_scheduler.model.TaskGroup;
import com.taskapp.task_scheduler.model.TaskType;
import com.taskapp.task_scheduler.model.Team;
import com.taskapp.task_scheduler.repository.AssignmentRepository;
import com.taskapp.task_scheduler.repository.EmployeeRepository;
import com.taskapp.task_scheduler.repository.PayrollRepository;
import com.taskapp.task_scheduler.repository.TaskRepository;
import com.taskapp.task_scheduler.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentAlgorithmUnitTests {

    @Mock private FeasibilityValidator validator;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private PayrollRepository payrollRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private TeamRepository teamRepository;

    @InjectMocks private AssignmentAlgorithm algorithm;

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 53})
    void rejectsInvalidWeeksBeforeUsingAnyDependency(int weeks) {
        assertThrows(IllegalArgumentException.class, () -> algorithm.generatePayroll(weeks, 10L));

        verifyNoInteractions(teamRepository, employeeRepository, taskRepository, payrollRepository,
                assignmentRepository, validator);
    }

    @Test
    void stopsBeforeArchivingWhenFeasibilityValidationFails() {
        Area area = area(1L, "Área");
        Employee employee = employee(1L, area);
        Task task = generalTask(1L, "Tarea");
        Team team = team(1L, List.of(area), List.of(task));
        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        when(employeeRepository.findByActiveTrue()).thenReturn(List.of(employee));
        doThrow(new IllegalStateException("Configuración inviable"))
                .when(validator).validar(any(), any());

        assertThrows(IllegalStateException.class, () -> algorithm.generatePayroll(1, team.getId()));

        verifyNoInteractions(payrollRepository, assignmentRepository);
    }

    @Test
    void validatesOnlyTeamEmployeesAndDistinctCatalogTasks() {
        Area includedArea = area(1L, "Incluida");
        Area outsideArea = area(2L, "Externa");
        Employee included = employee(1L, includedArea);
        Employee outside = employee(2L, outsideArea);
        Task repeated = generalTask(1L, "Repetida");

        TaskGroup firstCatalog = taskGroup(1L, List.of(repeated, repeated));
        TaskGroup secondCatalog = taskGroup(2L, List.of(repeated));
        Team team = new Team();
        team.setId(1L);
        team.setAreas(new ArrayList<>(List.of(includedArea)));
        team.setTaskGroups(new ArrayList<>(List.of(firstCatalog, secondCatalog)));

        prepareSuccessfulGeneration(team, List.of(included, outside), List.of());

        algorithm.generatePayroll(1, team.getId());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Employee>> employeeCaptor = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Task>> taskCaptor = ArgumentCaptor.forClass(List.class);
        verify(validator).validar(employeeCaptor.capture(), taskCaptor.capture());
        assertEquals(List.of(included), employeeCaptor.getValue());
        assertEquals(List.of(repeated), taskCaptor.getValue());
    }

    @Test
    void archivesActivePayrollBeforeSavingTheNewOne() {
        Area area = area(1L, "Área");
        Employee employee = employee(1L, area);
        Task task = generalTask(1L, "Tarea");
        Team team = team(1L, List.of(area), List.of(task));
        Payroll previous = new Payroll();
        previous.setId(50L);
        previous.setStatus(PayrollStatus.ACTIVE);
        previous.setTeam(team);
        previous.setStartDate(LocalDate.now().minusWeeks(1));
        previous.setEndDate(LocalDate.now());
        prepareSuccessfulGeneration(team, List.of(employee), List.of(previous));

        Payroll generated = algorithm.generatePayroll(2, team.getId());

        var order = inOrder(payrollRepository, assignmentRepository);
        order.verify(payrollRepository).findByStatus(PayrollStatus.ACTIVE);
        order.verify(payrollRepository).save(previous);
        order.verify(payrollRepository).save(generated);
        order.verify(assignmentRepository, times(2)).save(any(Assignment.class));
        assertEquals(PayrollStatus.ARCHIVED, previous.getStatus());
        assertEquals(PayrollStatus.ACTIVE, generated.getStatus());
        assertSame(team, generated.getTeam());
        assertEquals(generated.getStartDate().plusWeeks(2), generated.getEndDate());
    }

    @Test
    void coversAllCompatibleTasksByReassigningEmployees() {
        Area areaA = area(1L, "A");
        Area areaB = area(2L, "B");
        Area areaC = area(3L, "C");
        Employee employeeA = employee(1L, areaA);
        Employee employeeB = employee(2L, areaB);
        Employee employeeC = employee(3L, areaC);
        Task onlyA = specificTask(1L, "Solo A", areaA);
        Task aOrB = specificTask(2L, "A o B", areaA, areaB);
        Task bOrC = specificTask(3L, "B o C", areaB, areaC);
        Team team = team(1L, List.of(areaA, areaB, areaC), List.of(aOrB, bOrC, onlyA));
        prepareSuccessfulGeneration(team, List.of(employeeA, employeeB, employeeC), List.of());

        algorithm.generatePayroll(1, team.getId());

        List<Assignment> assignments = capturedAssignments(3);
        assertEquals(3, assignments.stream()
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.ASSIGNED).count());
        assertEquals(Set.of(onlyA.getId(), aOrB.getId(), bOrC.getId()), assignments.stream()
                .map(assignment -> assignment.getTask().getId()).collect(Collectors.toSet()));
        Assignment restricted = assignments.stream()
                .filter(assignment -> assignment.getTask().getId().equals(onlyA.getId()))
                .findFirst().orElseThrow();
        assertSame(employeeA, restricted.getEmployee());
    }

    @Test
    void rotatesWorkAndRestWhenThereAreMoreEmployeesThanTasks() {
        Area area = area(1L, "Área");
        List<Employee> employees = List.of(
                employee(1L, area), employee(2L, area), employee(3L, area));
        Team team = team(1L, List.of(area), List.of(generalTask(1L, "Única")));
        prepareSuccessfulGeneration(team, employees, List.of());

        algorithm.generatePayroll(3, team.getId());

        List<Assignment> assignments = capturedAssignments(9);
        for (int week = 1; week <= 3; week++) {
            int currentWeek = week;
            List<Assignment> weekly = assignments.stream()
                    .filter(assignment -> assignment.getWeekNumber() == currentWeek).toList();
            assertEquals(3, weekly.size());
            assertEquals(1, weekly.stream()
                    .filter(assignment -> assignment.getStatus() == AssignmentStatus.ASSIGNED).count());
            assertEquals(2, weekly.stream()
                    .filter(assignment -> assignment.getStatus() == AssignmentStatus.REST
                            && assignment.getTask() == null).count());
        }
        for (Employee employee : employees) {
            assertEquals(1, assignments.stream()
                    .filter(assignment -> assignment.getEmployee() == employee
                            && assignment.getStatus() == AssignmentStatus.ASSIGNED).count());
        }
    }

    @Test
    void avoidsRepeatingATaskWhenTheEmployeeHasAnAlternative() {
        Area area = area(1L, "Área");
        Employee employee = employee(1L, area);
        Task first = generalTask(1L, "Primera");
        Task second = generalTask(2L, "Segunda");
        Team team = team(1L, List.of(area), List.of(first, second));
        prepareSuccessfulGeneration(team, List.of(employee), List.of());

        algorithm.generatePayroll(2, team.getId());

        List<Assignment> assignments = capturedAssignments(2).stream()
                .sorted(java.util.Comparator.comparingInt(Assignment::getWeekNumber)).toList();
        assertTrue(assignments.stream().allMatch(assignment -> assignment.getStatus() == AssignmentStatus.ASSIGNED));
        assertNotEquals(assignments.get(0).getTask().getId(), assignments.get(1).getTask().getId(),
                "Debe elegir la otra tarea antes de repetir la última");
    }

    @Test
    void repeatsTheOnlyTaskWhenThereIsNoAlternative() {
        Area area = area(1L, "Área");
        Employee employee = employee(1L, area);
        Task onlyTask = generalTask(1L, "Única");
        Team team = team(1L, List.of(area), List.of(onlyTask));
        prepareSuccessfulGeneration(team, List.of(employee), List.of());

        algorithm.generatePayroll(2, team.getId());

        List<Assignment> assignments = capturedAssignments(2);
        assertTrue(assignments.stream()
                .allMatch(assignment -> assignment.getStatus() == AssignmentStatus.ASSIGNED
                        && assignment.getTask() == onlyTask));
    }

    private void prepareSuccessfulGeneration(Team team, List<Employee> employees, List<Payroll> activePayrolls) {
        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        when(employeeRepository.findByActiveTrue()).thenReturn(employees);
        when(payrollRepository.findByStatus(PayrollStatus.ACTIVE)).thenReturn(activePayrolls);
        AtomicLong ids = new AtomicLong(100L);
        when(payrollRepository.save(any(Payroll.class))).thenAnswer(invocation -> {
            Payroll payroll = invocation.getArgument(0);
            if (payroll.getId() == null) payroll.setId(ids.getAndIncrement());
            return payroll;
        });
        when(assignmentRepository.save(any(Assignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private List<Assignment> capturedAssignments(int expectedCount) {
        ArgumentCaptor<Assignment> captor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentRepository, times(expectedCount)).save(captor.capture());
        return captor.getAllValues();
    }

    private Area area(long id, String name) {
        Area area = new Area();
        area.setId(id);
        area.setNombre(name);
        return area;
    }

    private Employee employee(long id, Area area) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setName("Empleado " + id);
        employee.setArea(area);
        employee.setActive(true);
        return employee;
    }

    private Task generalTask(long id, String name) {
        Task task = new Task();
        task.setId(id);
        task.setName(name);
        task.setType(TaskType.GENERAL);
        return task;
    }

    private Task specificTask(long id, String name, Area... areas) {
        Task task = new Task();
        task.setId(id);
        task.setName(name);
        task.setType(TaskType.SPECIFIC);
        List<TaskArea> links = new ArrayList<>();
        for (Area area : areas) {
            TaskArea link = new TaskArea();
            link.setArea(area);
            links.add(link);
        }
        task.setAuthorizedAreas(links);
        return task;
    }

    private TaskGroup taskGroup(long id, List<Task> tasks) {
        TaskGroup group = new TaskGroup();
        group.setId(id);
        group.setName("Catálogo " + id);
        group.setTasks(new ArrayList<>(tasks));
        return group;
    }

    private Team team(long id, List<Area> areas, List<Task> tasks) {
        Team team = new Team();
        team.setId(id);
        team.setName("Equipo " + id);
        team.setAreas(new ArrayList<>(areas));
        team.setTaskGroups(new ArrayList<>(List.of(taskGroup(id, tasks))));
        return team;
    }
}
