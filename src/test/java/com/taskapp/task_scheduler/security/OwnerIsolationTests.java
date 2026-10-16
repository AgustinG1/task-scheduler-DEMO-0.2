package com.taskapp.task_scheduler.security;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.*;
import com.taskapp.task_scheduler.model.*;
import com.taskapp.task_scheduler.repository.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:owner_isolation;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=20000",
        "spring.jpa.show-sql=false", "spring.sql.init.mode=never"
})
@ActiveProfiles("local")
@AutoConfigureMockMvc
@Import(AccountIntegrationTests.TestProvider.class)
class OwnerIsolationTests {
    @Autowired MockMvc http;
    @Autowired ClientRegistrationRepository registrations;
    @Autowired JdbcTemplate jdbc;
    @Autowired AppUserRepository users;
    @Autowired AreaRepository areas;
    @Autowired EmployeeRepository employees;
    @Autowired TaskRepository tasks;
    @Autowired TaskGroupRepository groups;
    @Autowired TeamRepository teams;
    @Autowired PayrollRepository payrolls;
    @Autowired AssignmentRepository assignments;
    Fixture a;
    Fixture b;

    @Test
    void managementPagesServeTheirStylesAndScriptsThroughTheSecurityChain() throws Exception {
        for (String path : List.of("/employees", "/tasks", "/areas")) {
            String html = http.perform(get(path).with(identity("A"))).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            var resources = java.util.regex.Pattern.compile("(?:href|src)=\"(/(?:css|js)/[^\"]+)\"")
                    .matcher(html);
            int checked = 0;
            while (resources.find()) {
                String resource = resources.group(1);
                String mediaType = resource.contains("/css/") ? "text/css" : "text/javascript";
                for (var request : List.of(get(resource), get(resource).with(identity("A")))) {
                    http.perform(request).andExpect(status().isOk())
                            .andExpect(content().contentTypeCompatibleWith(mediaType))
                            .andExpect(content().string(not(isEmptyString())));
                }
                checked++;
            }
            assertThat(checked).as("CSS and script for %s", path).isEqualTo(2);
        }
    }

    @BeforeEach
    void fixtures() throws Exception {
        for (String table : List.of("asignaciones", "planillas", "empleado_equipo", "tarea_equipo",
                "equipo_area", "equipo_catalogo", "catalogo_tarea_detalle", "tarea_area", "equipos",
                "catalogos_tareas", "empleados", "tareas", "areas", "app_user")) {
            jdbc.update("delete from " + table);
        }
        a = createFixture("A");
        b = createFixture("B");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/app", "/areas", "/tasks", "/employees", "/task-groups", "/teams",
            "/payroll", "/payroll/historial"})
    void eachPageOnlyShowsTheCurrentAccountsData(String path) throws Exception {
        generate(a);
        generate(b);
        http.perform(get(path).with(identity("A"))).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("B-Tarea"))))
                .andExpect(content().string(not(containsString("B-Persona"))))
                .andExpect(content().string(not(containsString("B-Área"))))
                .andExpect(content().string(not(containsString("B-Catálogo"))));
        if (path.equals("/app")) {
            http.perform(get(path).with(identity("A"))).andExpect(model().attribute("areasCount", 1L))
                    .andExpect(model().attribute("employeesCount", 1L)).andExpect(model().attribute("payrollsCount", 1L));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"areas", "tasks", "employees", "task-groups", "teams", "payroll", "api/payrolls"})
    void anotherAccountsIdsAndMissingIdsHaveTheSameNotFoundResponse(String module) throws Exception {
        long bPayroll = generate(b);
        long id = switch (module) {
            case "areas" -> b.area; case "tasks" -> b.task; case "employees" -> b.employee;
            case "task-groups" -> b.group; case "teams" -> b.team; default -> bPayroll;
        };
        String suffix = module.contains("payroll") ? "/download" : "";
        String prefix = module.contains("payroll") ? "/" + module + "/" : "/" + module + "/editar/";
        http.perform(get(prefix + id + suffix).with(identity("A"))).andExpect(status().isNotFound());
        http.perform(get(prefix + "9999999" + suffix).with(identity("A"))).andExpect(status().isNotFound());
    }

    @Test
    void newAccountsAreEmptyAndHistoricalRowsAreNotAdopted() throws Exception {
        Area legacy = new Area();
        legacy.setNombre("Legacy not assigned");
        legacy = areas.saveAndFlush(legacy);
        http.perform(get("/app").with(identity("C"))).andExpect(status().isOk())
                .andExpect(model().attribute("areasCount", 0L)).andExpect(model().attribute("payrollsCount", 0L));
        http.perform(get("/areas").with(identity("C")))
                .andExpect(content().string(not(containsString("Legacy not assigned"))));
        http.perform(get("/areas/editar/" + legacy.getId()).with(identity("A"))).andExpect(status().isNotFound());
        assertThat(areas.findById(legacy.getId()).orElseThrow().getOwner()).isNull();
    }

    @Test
    void createIgnoresInjectedIdOwnerAndCollections() throws Exception {
        long before = areas.count();
        write("A", post("/areas/guardar").param("nombre", "Safe new area").param("id", b.area.toString())
                .param("owner.id", b.owner.toString()).param("ownerId", b.owner.toString())
                .param("teams[0].id", b.team.toString()));
        assertThat(areas.count()).isEqualTo(before + 1);
        var own = areas.findAllByOwner_Id(a.owner);
        assertThat(own).extracting(Area::getNombre).contains("Safe new area");
        assertThat(areas.findById(b.area).orElseThrow().getNombre()).isEqualTo("B-Área");
    }

    @Test
    void editingForeignResourcesCannotChangeThem() throws Exception {
        forbidden(post("/areas/editar/" + b.area).param("nombre", "Changed"));
        forbidden(post("/tasks/editar/" + b.task).param("name", "Changed").param("type", "GENERAL"));
        forbidden(post("/employees/editar/" + b.employee).param("nombre", "Changed").param("area.id", a.area.toString()));
        forbidden(post("/task-groups/actualizar").param("id", b.group.toString()).param("name", "Changed"));
        forbidden(post("/teams/actualizar").param("id", b.team.toString()).param("name", "Changed"));
        assertThat(areas.findById(b.area).orElseThrow().getNombre()).isEqualTo("B-Área");
        assertThat(tasks.findById(b.task).orElseThrow().getName()).isEqualTo("B-Tarea");
        assertThat(employees.findById(b.employee).orElseThrow().getName()).isEqualTo("B-Persona");
        assertThat(groups.findById(b.group).orElseThrow().getName()).isEqualTo("B-Catálogo");
        assertThat(teams.findById(b.team).orElseThrow().getName()).isEqualTo("Equipo compartido");
    }

    @Test
    void foreignRelationsRejectTheWholeRequestBeforeChanges() throws Exception {
        forbidden(post("/employees/editar/" + a.employee).param("nombre", "Changed").param("area.id", b.area.toString()));
        forbidden(post("/task-groups/actualizar").param("id", a.group.toString()).param("name", "Changed")
                .param("tasksIds", a.task.toString(), b.task.toString()));
        forbidden(post("/teams/actualizar").param("id", a.team.toString()).param("name", "Changed")
                .param("areasIds", a.area.toString(), b.area.toString()));
        forbidden(post("/teams/actualizar").param("id", a.team.toString()).param("name", "Changed")
                .param("areasIds", a.area.toString()).param("taskGroupsIds", b.group.toString()));
        forbidden(post("/teams/empleado/actualizar").param("equipoId", a.team.toString())
                .param("empleadoId", b.employee.toString()).param("areaId", a.area.toString()).param("active", "false"));
        forbidden(post("/teams/empleado/actualizar").param("equipoId", b.team.toString())
                .param("empleadoId", a.employee.toString()).param("areaId", a.area.toString()).param("active", "false"));
        assertThat(employees.findById(a.employee).orElseThrow().getName()).isEqualTo("A-Persona");
        assertThat(employees.findById(b.employee).orElseThrow().isActive()).isTrue();
        assertThat(groups.findById(a.group).orElseThrow().getName()).isEqualTo("A-Catálogo");
        assertThat(teams.findById(a.team).orElseThrow().getName()).isEqualTo("Equipo compartido");
        assertThat(jdbc.queryForObject("select count(*) from catalogo_tarea_detalle where catalogo_id=?", Long.class, a.group)).isEqualTo(1);
    }

    @Test
    void foreignAreaCannotLeaveAPartiallyCreatedTaskOrTeam() throws Exception {
        long taskCount = tasks.count();
        long teamCount = teams.count();
        forbidden(post("/tasks/guardar").param("name", "Rejected task").param("type", "SPECIFIC").param("areaId", b.area.toString()));
        forbidden(post("/teams/guardar").param("name", "Rejected team").param("areasIds", b.area.toString()));
        assertThat(tasks.count()).isEqualTo(taskCount);
        assertThat(teams.count()).isEqualTo(teamCount);
    }

    @Test
    void aMissingSelectionIdIsRejectedInsteadOfSilentlyDiscarded() throws Exception {
        forbidden(post("/task-groups/actualizar").param("id", a.group.toString()).param("name", "Changed")
                .param("tasksIds", a.task.toString(), "9999999"));
        assertThat(groups.findById(a.group).orElseThrow().getName()).isEqualTo("A-Catálogo");
    }

    @Test
    void generatingAndDeletingOnlyAffectsTheCurrentAccountsPayrolls() throws Exception {
        long firstA = generate(a);
        long firstB = generate(b);
        long nextA = generate(a);
        assertThat(payrolls.findById(firstA).orElseThrow().getStatus()).isEqualTo(PayrollStatus.ARCHIVED);
        assertThat(payrolls.findById(firstB).orElseThrow().getStatus()).isEqualTo(PayrollStatus.ACTIVE);
        assertThat(payrolls.findByOwner_IdAndStatus(a.owner, PayrollStatus.ACTIVE)).hasSize(1);
        forbidden(post("/payroll/generar").param("teamId", b.team.toString()).param("semanas", "2"));
        assertThat(payrolls.findById(nextA).orElseThrow().getStatus()).isEqualTo(PayrollStatus.ACTIVE);
        forbidden(post("/payroll/eliminar/" + firstB));
        write("A", post("/payroll/eliminar/" + firstA));
        assertThat(payrolls.findById(firstA)).isEmpty();
        assertThat(assignments.findByPayrollId(firstB)).hasSize(2);
    }

    @Test
    void bothDownloadsExportOnlyOwnedAssignments() throws Exception {
        long own = generate(a);
        long foreign = generate(b);
        for (String prefix : List.of("/payroll/", "/api/payrolls/")) {
            var result = http.perform(get(prefix + own + "/download").with(identity("A")))
                    .andExpect(status().isOk()).andReturn();
            try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
                var sheet = workbook.getSheetAt(0);
                assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("A-Tarea");
                assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("A-Persona");
            }
            http.perform(get(prefix + foreign + "/download").with(identity("A"))).andExpect(status().isNotFound());
        }
    }

    @Test
    void ownEditsAndDeactivationPreserveTheOtherAccountAndHistory() throws Exception {
        long ownPayroll = generate(a);
        write("A", post("/areas/editar/" + a.area).param("nombre", "Updated area"));
        write("A", post("/tasks/editar/" + a.task).param("name", "Updated task").param("type", "GENERAL"));
        write("A", post("/employees/editar/" + a.employee).param("nombre", "Updated employee").param("area.id", a.area.toString()));
        write("A", post("/employees/desactivar/" + a.employee));
        assertThat(employees.findById(a.employee).orElseThrow().isActive()).isFalse();
        assertThat(assignments.findByPayrollId(ownPayroll)).hasSize(2);
        assertThat(employees.findById(b.employee).orElseThrow().getName()).isEqualTo("B-Persona");
        http.perform(get("/areas").with(identity("A"))).andExpect(content().string(containsString("Updated area")));
    }

    @Test
    void deletingAnOwnTaskOnlyConvertsItsOwnAssignmentsToRest() throws Exception {
        long ownPayroll = generate(a);
        long otherPayroll = generate(b);
        write("A", post("/tasks/eliminar/" + a.task));
        assertThat(tasks.findById(a.task)).isEmpty();
        assertThat(assignments.findByPayrollId(ownPayroll)).allMatch(x -> x.getTask() == null && x.getStatus() == AssignmentStatus.REST);
        assertThat(assignments.findByPayrollId(otherPayroll)).allMatch(x -> x.getTask() != null && x.getStatus() == AssignmentStatus.ASSIGNED);
    }

    @Test
    void deletesRequirePostCsrfAndOwnership() throws Exception {
        long foreignPayroll = generate(b);
        for (var entry : java.util.Map.of("areas", b.area, "tasks", b.task, "teams", b.team,
                "task-groups", b.group, "payroll", foreignPayroll).entrySet()) {
            String path = "/" + entry.getKey() + "/eliminar/" + entry.getValue();
            http.perform(get(path).with(identity("A"))).andExpect(status().isMethodNotAllowed());
            http.perform(post(path).with(identity("A"))).andExpect(status().isForbidden());
            forbidden(post(path));
        }
        http.perform(post("/employees/desactivar/" + a.employee).with(identity("A"))).andExpect(status().isForbidden());
        assertThat(employees.findById(a.employee).orElseThrow().isActive()).isTrue();
    }

    @Test
    void twoConcurrentGenerationsLeaveOneActiveForTheirOwner() throws Exception {
        long firstB = generate(b);
        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            var jobs = java.util.stream.IntStream.range(0, 2).mapToObj(i -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out");
                write("A", post("/payroll/generar").param("teamId", a.team.toString()).param("semanas", "2"));
                return true;
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (var job : jobs) assertThat(job.get(25, TimeUnit.SECONDS)).isTrue();
            assertThat(payrolls.findByOwner_IdAndStatus(a.owner, PayrollStatus.ACTIVE)).hasSize(1);
            assertThat(payrolls.findAllByOwner_Id(a.owner)).hasSize(2);
            assertThat(payrolls.findById(firstB).orElseThrow().getStatus()).isEqualTo(PayrollStatus.ACTIVE);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void consoleAndUnknownRoutesStayClosedEvenForVerifiedUsers() throws Exception {
        http.perform(get("/h2-console/").with(identity("A"))).andExpect(status().isForbidden());
        http.perform(get("/unlisted-route").with(identity("A"))).andExpect(status().isForbidden());
    }

    private Fixture createFixture(String subject) throws Exception {
        http.perform(get("/api/me").with(identity(subject))).andExpect(status().isOk());
        Long owner = users.findAll().stream().filter(u -> u.getSubject().equals(subject)).findFirst().orElseThrow().getId();
        write(subject, post("/areas/guardar").param("nombre", subject + "-Área"));
        Long area = areas.findAllByOwner_Id(owner).get(0).getId();
        write(subject, post("/employees/guardar").param("nombre", subject + "-Persona").param("area.id", area.toString()));
        Long employee = employees.findAllByOwner_Id(owner).get(0).getId();
        write(subject, post("/tasks/guardar").param("name", subject + "-Tarea").param("type", "GENERAL"));
        Long task = tasks.findAllByOwner_Id(owner).get(0).getId();
        write(subject, post("/task-groups/guardar").param("name", subject + "-Catálogo"));
        Long group = groups.findAllByOwner_Id(owner).get(0).getId();
        write(subject, post("/task-groups/actualizar").param("id", group.toString()).param("name", subject + "-Catálogo").param("tasksIds", task.toString()));
        write(subject, post("/teams/guardar").param("name", "Equipo compartido").param("areasIds", area.toString()));
        Long team = teams.findAllByOwner_Id(owner).get(0).getId();
        write(subject, post("/teams/actualizar").param("id", team.toString()).param("name", "Equipo compartido")
                .param("areasIds", area.toString()).param("taskGroupsIds", group.toString()));
        return new Fixture(subject, owner, area, employee, task, group, team);
    }

    private long generate(Fixture fixture) throws Exception {
        write(fixture.subject, post("/payroll/generar").param("teamId", fixture.team.toString()).param("semanas", "2"));
        return payrolls.findByOwner_IdAndStatus(fixture.owner, PayrollStatus.ACTIVE).get(0).getId();
    }

    private void write(String subject, MockHttpServletRequestBuilder request) throws Exception {
        http.perform(request.with(identity(subject)).with(csrf())).andExpect(status().is3xxRedirection());
    }

    private void forbidden(MockHttpServletRequestBuilder request) throws Exception {
        http.perform(request.with(identity("A")).with(csrf())).andExpect(status().isNotFound());
    }

    private OidcLoginRequestPostProcessor identity(String subject) {
        return oidcLogin().clientRegistration(registrations.findByRegistrationId("auth0"))
                .idToken(t -> t.issuer("https://idp.example.test/").subject(subject)
                        .claim("email", subject + "@example.test").claim("name", subject)
                        .claim("email_verified", true));
    }

    record Fixture(String subject, Long owner, Long area, Long employee, Long task, Long group, Long team) { }
}
