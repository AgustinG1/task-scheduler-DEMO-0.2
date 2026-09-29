package com.taskapp.task_scheduler.service;

import com.taskapp.task_scheduler.model.Area;
import com.taskapp.task_scheduler.model.Assignment;
import com.taskapp.task_scheduler.model.AssignmentStatus;
import com.taskapp.task_scheduler.model.Employee;
import com.taskapp.task_scheduler.model.Payroll;
import com.taskapp.task_scheduler.model.PayrollStatus;
import com.taskapp.task_scheduler.model.Task;
import com.taskapp.task_scheduler.model.TaskArea;
import com.taskapp.task_scheduler.model.TaskAreaId;
import com.taskapp.task_scheduler.model.TaskGroup;
import com.taskapp.task_scheduler.model.TaskType;
import com.taskapp.task_scheduler.model.Team;
import com.taskapp.task_scheduler.repository.AreaRepository;
import com.taskapp.task_scheduler.repository.AssignmentRepository;
import com.taskapp.task_scheduler.repository.EmployeeRepository;
import com.taskapp.task_scheduler.repository.PayrollRepository;
import com.taskapp.task_scheduler.repository.TaskAreaRepository;
import com.taskapp.task_scheduler.repository.TaskGroupRepository;
import com.taskapp.task_scheduler.repository.TaskRepository;
import com.taskapp.task_scheduler.repository.TeamRepository;
import jakarta.persistence.EntityManager;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.jpa.show-sql=false",
        "spring.datasource.url=jdbc:h2:mem:taskdb_foundations;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@ActiveProfiles("local")
@Transactional
class AssignmentAlgorithmIntegrationTests {

    @Autowired private AssignmentAlgorithm algorithm;
    @Autowired private EmployeeService employeeService;
    @Autowired private ExcelExportService excelExportService;
    @Autowired private AreaRepository areaRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private TaskAreaRepository taskAreaRepository;
    @Autowired private TaskGroupRepository taskGroupRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private PayrollRepository payrollRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void assignsEachEligibleEmployeeOncePerWeekAndRespectsSpecificAreas() {
        Area production = area("Producción");
        Area sales = area("Ventas");
        Employee specialist = employee("Especialista", production, true);
        Employee colleague = employee("Colega", sales, true);
        Employee inactive = employee("Inactivo", production, false);
        Employee outsider = employee("Externo", area("Exterior"), true);
        Task general = task("General", TaskType.GENERAL);
        Task specific = specificTask("Solo producción", production);
        Team team = team("Equipo principal", List.of(production, sales), List.of(general, specific));

        Payroll payroll = algorithm.generatePayroll(3, team.getId());
        List<Assignment> assignments = assignmentRepository.findByPayrollId(payroll.getId());

        assertEquals(6, assignments.size());
        for (int week = 1; week <= 3; week++) {
            final int currentWeek = week;
            List<Assignment> weekly = assignments.stream()
                    .filter(a -> a.getWeekNumber() == currentWeek).toList();
            assertEquals(2, weekly.size());
            Set<Long> employeeIds = weekly.stream().map(a -> a.getEmployee().getId())
                    .collect(java.util.stream.Collectors.toSet());
            assertEquals(Set.of(specialist.getId(), colleague.getId()), employeeIds);
            assertEquals(2, weekly.stream().filter(a -> a.getStatus() == AssignmentStatus.ASSIGNED).count());
            assertEquals(1, weekly.stream().filter(a -> a.getTask().getId().equals(general.getId())).count());
            assertEquals(1, weekly.stream().filter(a -> a.getTask().getId().equals(specific.getId())
                    && a.getEmployee().getId().equals(specialist.getId())).count());
            assertFalse(weekly.stream().anyMatch(a -> a.getEmployee().getId().equals(inactive.getId())
                    || a.getEmployee().getId().equals(outsider.getId())));
        }
    }

    @Test
    void rotatesPlannedRestWhenThereAreMoreEmployeesThanTasks() {
        Area area = area("Área");
        List<Employee> employees = List.of(
                employee("Uno", area, true),
                employee("Dos", area, true),
                employee("Tres", area, true));
        Team team = team("Equipo", List.of(area), List.of(task("Única", TaskType.GENERAL)));

        Payroll payroll = algorithm.generatePayroll(3, team.getId());
        List<Assignment> assignments = assignmentRepository.findByPayrollId(payroll.getId());

        assertEquals(9, assignments.size());
        for (Employee employee : employees) {
            List<Assignment> personal = assignments.stream()
                    .filter(a -> a.getEmployee().getId().equals(employee.getId())).toList();
            assertEquals(3, personal.size());
            assertEquals(1, personal.stream().filter(a -> a.getStatus() == AssignmentStatus.ASSIGNED).count());
            assertEquals(2, personal.stream().filter(a -> a.getStatus() == AssignmentStatus.REST
                    && a.getTask() == null).count());
        }
    }

    @Test
    void archivesThePreviousActivePayrollWhenGeneratingForAnotherTeam() {
        Area area = area("Área");
        employee("Persona", area, true);
        Task task = task("Tarea", TaskType.GENERAL);
        Team firstTeam = team("Primero", List.of(area), List.of(task));
        Team secondTeam = team("Segundo", List.of(area), List.of(task));

        Payroll first = algorithm.generatePayroll(1, firstTeam.getId());
        Payroll second = algorithm.generatePayroll(1, secondTeam.getId());

        assertEquals(PayrollStatus.ARCHIVED, payrollRepository.findById(first.getId()).orElseThrow().getStatus());
        assertEquals(PayrollStatus.ACTIVE, payrollRepository.findById(second.getId()).orElseThrow().getStatus());
        assertEquals(1, payrollRepository.findByStatus(PayrollStatus.ACTIVE).size());
        assertEquals(1, assignmentRepository.findByPayrollId(first.getId()).size());
    }

    @Test
    void deactivatesEmployeeWithoutBreakingHistoricalAssignments() {
        Area area = area("Área histórica");
        Employee employee = employee("Persona histórica", area, true);
        Task task = task("Tarea histórica", TaskType.GENERAL);
        Team team = team("Equipo histórico", List.of(area), List.of(task));
        Payroll payroll = algorithm.generatePayroll(1, team.getId());

        employeeService.deactivateEmployee(employee.getId());
        entityManager.flush();
        entityManager.clear();

        Employee stored = employeeRepository.findById(employee.getId()).orElseThrow();
        List<Assignment> historicalAssignments = assignmentRepository.findByPayrollId(payroll.getId());
        assertFalse(stored.isActive());
        assertEquals(1, historicalAssignments.size());
        assertEquals(employee.getId(), historicalAssignments.get(0).getEmployee().getId());
    }

    @Test
    void exportsTheGeneratedAssignmentsAsAnExcelWorkbook() throws IOException {
        Area area = area("Área");
        Employee employee = employee("Persona", area, true);
        Task task = task("Tarea", TaskType.GENERAL);
        Team team = team("Equipo", List.of(area), List.of(task));
        Payroll payroll = algorithm.generatePayroll(2, team.getId());

        byte[] excel = excelExportService.generateExcelForPayroll(payroll.getId());

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            var sheet = workbook.getSheet("Matriz de Turnos");
            assertNotNull(sheet);
            assertEquals(3, sheet.getPhysicalNumberOfRows());
            assertEquals("Semana", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals(task.getName(), sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("En Descanso (REST)", sheet.getRow(0).getCell(2).getStringCellValue());
            assertEquals(employee.getName(), sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals(employee.getName(), sheet.getRow(2).getCell(1).getStringCellValue());
        }
    }

    @Test
    void coversACompatibleSpecificTaskEveryWeek() {
        Area specialistArea = area("Especialistas");
        Area otherArea = area("Otros");
        employee("Especialista", specialistArea, true);
        employee("General uno", otherArea, true);
        employee("General dos", otherArea, true);
        Task specific = specificTask("Específica", specialistArea);
        Task general = task("General", TaskType.GENERAL);
        Team team = team("Equipo", List.of(specialistArea, otherArea), List.of(specific, general));

        Payroll payroll = algorithm.generatePayroll(3, team.getId());
        List<Assignment> assignments = assignmentRepository.findByPayrollId(payroll.getId());

        for (int week = 1; week <= 3; week++) {
            final int currentWeek = week;
            assertEquals(1, assignments.stream().filter(a -> a.getWeekNumber() == currentWeek
                    && a.getTask() != null && a.getTask().getId().equals(specific.getId())).count(),
                    "Hay una persona autorizada para la tarea específica en la semana " + week);
        }
    }

    @Test
    void rejectsZeroWeeksBeforeArchivingAnExistingPayroll() {
        Area area = area("Área");
        employee("Persona", area, true);
        Team team = team("Equipo", List.of(area), List.of(task("Tarea", TaskType.GENERAL)));
        Payroll previous = new Payroll();
        previous.setTeam(team);
        previous.setStatus(PayrollStatus.ACTIVE);
        previous.setStartDate(LocalDate.now());
        previous.setEndDate(LocalDate.now().plusWeeks(1));
        previous = payrollRepository.save(previous);

        assertThrows(IllegalArgumentException.class, () -> algorithm.generatePayroll(0, team.getId()));
        assertEquals(PayrollStatus.ACTIVE, payrollRepository.findById(previous.getId()).orElseThrow().getStatus());
        assertEquals(1, payrollRepository.findByStatus(PayrollStatus.ACTIVE).size());
    }

    @Test
    void rejectsNegativeOrExcessiveWeeksWithoutCreatingPayroll() {
        for (int weeks : List.of(-1, 53)) {
            assertThrows(IllegalArgumentException.class, () -> algorithm.generatePayroll(weeks, -1L));
        }
        assertEquals(0, payrollRepository.findAll().size());
    }

    @Test
    void coversOverlappingSpecificTasksWhenACompleteAssignmentExists() {
        Area a = area("A");
        Area b = area("B");
        Area c = area("C");
        Employee workerA = employee("Persona A", a, true);
        employee("Persona B", b, true);
        employee("Persona C", c, true);
        Task onlyA = specificTask("Solo A", a);
        Task aOrB = specificTask("A o B", a, b);
        Task bOrC = specificTask("B o C", b, c);
        Team team = team("Equipo", List.of(a, b, c), List.of(aOrB, bOrC, onlyA));

        Payroll payroll = algorithm.generatePayroll(4, team.getId());
        List<Assignment> assignments = assignmentRepository.findByPayrollId(payroll.getId());

        for (int week = 1; week <= 4; week++) {
            final int currentWeek = week;
            List<Assignment> weekly = assignments.stream()
                    .filter(assignment -> assignment.getWeekNumber() == currentWeek).toList();
            assertEquals(3, weekly.size());
            assertEquals(3, weekly.stream().filter(assignment -> assignment.getStatus() == AssignmentStatus.ASSIGNED).count());
            assertEquals(Set.of(onlyA.getId(), aOrB.getId(), bOrC.getId()), weekly.stream()
                    .map(assignment -> assignment.getTask().getId())
                    .collect(java.util.stream.Collectors.toSet()));
            assertEquals(workerA.getId(), weekly.stream()
                    .filter(assignment -> assignment.getTask().getId().equals(onlyA.getId()))
                    .findFirst().orElseThrow().getEmployee().getId());
        }
    }

    private Area area(String name) {
        Area area = new Area();
        area.setNombre(name);
        return areaRepository.save(area);
    }

    private Employee employee(String name, Area area, boolean active) {
        Employee employee = new Employee();
        employee.setName(name);
        employee.setArea(area);
        employee.setActive(active);
        return employeeRepository.save(employee);
    }

    private Task task(String name, TaskType type) {
        Task task = new Task();
        task.setName(name);
        task.setType(type);
        return taskRepository.save(task);
    }

    private Task specificTask(String name, Area... areas) {
        Task task = task(name, TaskType.SPECIFIC);
        for (Area area : areas) {
            TaskArea link = new TaskArea();
            link.setId(new TaskAreaId(task.getId(), area.getId()));
            link.setTask(task);
            link.setArea(area);
            taskAreaRepository.save(link);
        }
        return task;
    }

    private Team team(String name, List<Area> areas, List<Task> tasks) {
        TaskGroup catalog = new TaskGroup();
        catalog.setName(name + " catálogo");
        catalog.setTasks(new ArrayList<>(tasks));
        catalog = taskGroupRepository.save(catalog);

        Team team = new Team();
        team.setName(name);
        team.setAreas(new ArrayList<>(areas));
        team.setTaskGroups(new ArrayList<>(List.of(catalog)));
        team = teamRepository.save(team);

        entityManager.flush();
        entityManager.clear();
        return team;
    }
}
