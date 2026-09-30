package com.taskapp.task_scheduler.service;

import com.taskapp.task_scheduler.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RotationPlannerTests {
    @ParameterizedTest
    @ValueSource(ints = {4, 6, 8, 12, 24, 52})
    void balancesTheScreenshotConfigurationWithoutConsecutiveRepeats(int weeks) {
        for (int seed = 0; seed < 40; seed++) {
            Fixture fixture = pastryFixture();
            List<Map<Long, Task>> plan = new RotationPlanner().plan(fixture.employees, fixture.tasks, weeks, new Random(seed));
            String context = "weeks=" + weeks + ", seed=" + seed;
            verifyStructure(fixture, plan);
            for (int w = 0; w < weeks; w++) {
                assertEquals(8, plan.get(w).size(), context + ", coverage week=" + w);
                if (w > 0) for (Employee e : fixture.employees) {
                    assertNotEquals(plan.get(w - 1).get(e.getId()), plan.get(w).get(e.getId()),
                            context + ", repeated week=" + w + ", employee=" + e.getName());
                }
            }
            verifyBalancedCounts(fixture, plan, context);
            verifyCyclePriority(fixture, plan, context);
        }
    }

    @Test
    void matchesAnExhaustiveOracleForCoverageAndMinimumConsecutiveRepeats() {
        // Small arbitrary authorization graphs; the oracle enumerates all weekly
        // assignments independently of the production matching algorithm.
        Random random = new Random(4219);
        for (int sample = 0; sample < 80; sample++) {
            boolean[][] allowed = new boolean[4][5];
            for (boolean[] row : allowed) for (int t = 0; t < row.length; t++) row[t] = random.nextBoolean();
            Fixture fixture = fixture(allowed);
            List<Map<Long, Task>> plan = new RotationPlanner().plan(fixture.employees, fixture.tasks, 6, new Random(sample));
            verifyStructure(fixture, plan);
            verifyCyclePriority(fixture, plan, "authorization graph=" + sample);
            int[] previous = {-1,-1,-1,-1};
            for (Map<Long, Task> week : plan) {
                int optimum = enumerate(allowed, previous, 0, 0, 0, 0);
                int covered = 0, repeats = 0;
                int[] current = new int[4]; Arrays.fill(current, -1);
                for (int e = 0; e < 4; e++) {
                    Task task = week.get(fixture.employees.get(e).getId());
                    if (task == null) continue;
                    current[e] = fixture.tasks.indexOf(task);
                    covered++;
                    if (current[e] == previous[e]) repeats++;
                }
                assertEquals(optimum, covered * 5 - repeats, "sample=" + sample);
                previous = current;
            }
        }
    }

    @Test
    void balancesRestAndDoesNotTreatARestWeekAsAConsecutiveTask() {
        Fixture fixture = fixture(new boolean[][]{{true}, {true}, {true}});
        List<Map<Long, Task>> plan = new RotationPlanner().plan(fixture.employees, fixture.tasks, 12, new Random(4));
        verifyStructure(fixture, plan);
        assertTrue(plan.stream().allMatch(week -> week.size() == 1));
        verifyBalancedCounts(fixture, plan, "rest");
        for (int w = 1; w < plan.size(); w++) assertNotEquals(plan.get(w - 1).keySet(), plan.get(w).keySet());
    }

    @Test
    void rotatesAllAvailableTasksWhenThereAreMoreTasksThanPeople() {
        Fixture fixture = fixture(new boolean[][]{{true, true, true}});
        List<Map<Long, Task>> plan = new RotationPlanner().plan(fixture.employees, fixture.tasks, 12, new Random(5));
        verifyStructure(fixture, plan);
        for (Task task : fixture.tasks) assertEquals(4, plan.stream().filter(week -> week.containsValue(task)).count());
        for (int w = 1; w < plan.size(); w++) assertNotEquals(plan.get(w - 1), plan.get(w));
        for (int start = 0; start < plan.size(); start += 3) {
            assertEquals(3, plan.subList(start, start + 3).stream().map(week -> week.get(1L)).distinct().count());
        }
    }

    @Test
    void completesEveryPersonalCycleWhenAllEmployeesCanCoverAllTasks() {
        Fixture fixture = fixture(new boolean[][]{{true,true,true}, {true,true,true}, {true,true,true}});
        for (int seed = 0; seed < 40; seed++) {
            var plan = new RotationPlanner().plan(fixture.employees, fixture.tasks, 12, new Random(seed));
            for (Employee employee : fixture.employees) for (int start = 0; start < 12; start += 3) {
                assertEquals(3, plan.subList(start, start + 3).stream()
                        .map(week -> week.get(employee.getId())).distinct().count(), "seed=" + seed);
            }
        }
    }

    @Test
    void keepsThePersonalCycleAcrossRestWeeks() {
        Fixture fixture = fixture(new boolean[][]{{true,true}, {true,true}, {true,true}});
        var plan = new RotationPlanner().plan(fixture.employees, fixture.tasks, 12, new Random(17));
        verifyCyclePriority(fixture, plan, "rest cycle");
        for (Employee employee : fixture.employees) {
            var worked = plan.stream().map(week -> week.get(employee.getId())).filter(Objects::nonNull).toList();
            for (int start = 0; start + 1 < worked.size(); start += 2) assertNotEquals(worked.get(start), worked.get(start + 1));
        }
    }

    @Test
    void groupsByAuthorizationsEvenWhenEmployeesHaveDifferentAreas() {
        Fixture fixture = fixture(new boolean[][]{{true,true}, {true,true}, {true,true}});
        List<Map<Long, Task>> plan = new RotationPlanner().plan(fixture.employees, fixture.tasks, 12, new Random(9));
        verifyStructure(fixture, plan);
        verifyBalancedCounts(fixture, plan, "different areas");
    }

    @Test
    void allowsNecessaryRepeatsAndKeepsUnauthorizedTasksUnassigned() {
        Fixture fixture = fixture(new boolean[][]{{true,false}, {false,false}});
        List<Map<Long, Task>> plan = new RotationPlanner().plan(fixture.employees, fixture.tasks, 5, new Random(10));
        verifyStructure(fixture, plan);
        assertTrue(plan.stream().allMatch(week -> week.equals(Map.of(1L, fixture.tasks.get(0)))));
    }

    @Test
    void startsFreshForEveryPayrollAndDoesNotReorderTheInputs() {
        Fixture fixture = pastryFixture();
        List<Employee> originalEmployees = List.copyOf(fixture.employees);
        List<Task> originalTasks = List.copyOf(fixture.tasks);
        RotationPlanner planner = new RotationPlanner();
        var first = planner.plan(fixture.employees, fixture.tasks, 8, new Random(7));
        planner.plan(fixture.employees, fixture.tasks, 52, new Random(13));
        assertEquals(first, planner.plan(fixture.employees, fixture.tasks, 8, new Random(7)));
        assertEquals(originalEmployees, fixture.employees);
        assertEquals(originalTasks, fixture.tasks);
    }

    private void verifyStructure(Fixture fixture, List<Map<Long, Task>> plan) {
        for (Map<Long, Task> week : plan) {
            assertEquals(week.size(), new HashSet<>(week.values()).size(), "Duplicate task");
            for (var assignment : week.entrySet()) {
                int e = assignment.getKey().intValue() - 1;
                int t = fixture.tasks.indexOf(assignment.getValue());
                assertTrue(e >= 0 && e < fixture.employees.size() && t >= 0 && fixture.allowed[e][t], "Unauthorized assignment");
            }
        }
    }

    private void verifyBalancedCounts(Fixture fixture, List<Map<Long, Task>> plan, String context) {
        for (int a = 0; a < fixture.employees.size(); a++) for (int b = a + 1; b < fixture.employees.size(); b++) {
            if (!Arrays.equals(fixture.allowed[a], fixture.allowed[b])) continue;
            // Include REST as an extra bucket.
            for (int t = 0; t <= fixture.tasks.size(); t++) {
                Task task = t == fixture.tasks.size() ? null : fixture.tasks.get(t);
                long firstId = fixture.employees.get(a).getId(), secondId = fixture.employees.get(b).getId();
                long first = plan.stream().filter(week -> Objects.equals(week.get(firstId), task)).count();
                long second = plan.stream().filter(week -> Objects.equals(week.get(secondId), task)).count();
                assertTrue(Math.abs(first - second) <= 1, context + ", employees=" + firstId + "/" + secondId
                        + ", task=" + (task == null ? "REST" : task.getName()) + ", counts=" + first + "/" + second);
            }
        }
    }

    private int enumerate(boolean[][] allowed, int[] previous, int employee, int used, int covered, int repeats) {
        if (employee == allowed.length) return covered * (allowed.length + 1) - repeats;
        int best = enumerate(allowed, previous, employee + 1, used, covered, repeats);
        for (int t = 0; t < allowed[employee].length; t++) if (allowed[employee][t] && (used & (1 << t)) == 0) {
            best = Math.max(best, enumerate(allowed, previous, employee + 1, used | (1 << t), covered + 1,
                    repeats + (previous[employee] == t ? 1 : 0)));
        }
        return best;
    }

    private void verifyCyclePriority(Fixture fixture, List<Map<Long, Task>> plan, String context) {
        int people = fixture.employees.size(), taskCount = fixture.tasks.size(), weight = people + 1;
        int[] previous = new int[people]; Arrays.fill(previous, -1);
        List<Set<Integer>> cycles = new ArrayList<>();
        for (int e = 0; e < people; e++) cycles.add(new HashSet<>());
        for (int w = 0; w < plan.size(); w++) {
            int[][] memo = new int[people][1 << taskCount];
            for (int[] row : memo) Arrays.fill(row, Integer.MIN_VALUE);
            int optimum = enumerateCycle(fixture.allowed, previous, cycles, 0, 0, memo);
            int actual = 0;
            for (int e = 0; e < people; e++) {
                Task task = plan.get(w).get(fixture.employees.get(e).getId());
                if (task == null) { previous[e] = -1; continue; }
                int t = fixture.tasks.indexOf(task);
                actual += weight * weight - (previous[e] == t ? weight : 0) - (cycles.get(e).contains(t) ? 1 : 0);
                previous[e] = t;
                cycles.get(e).add(t);
                int authorized = 0;
                for (boolean allowed : fixture.allowed[e]) if (allowed) authorized++;
                if (cycles.get(e).size() == authorized) cycles.get(e).clear();
            }
            assertEquals(optimum, actual, context + ", avoidable cycle repetition week=" + (w + 1));
        }
    }

    // Independent exhaustive oracle with memoization by employee/used tasks.
    // Coverage, consecutive repeats and incomplete-cycle repeats have separate priorities.
    private int enumerateCycle(boolean[][] allowed, int[] previous, List<Set<Integer>> cycles,
            int employee, int used, int[][] memo) {
        if (employee == allowed.length) return 0;
        if (memo[employee][used] != Integer.MIN_VALUE) return memo[employee][used];
        int best = enumerateCycle(allowed, previous, cycles, employee + 1, used, memo);
        int weight = allowed.length + 1;
        for (int t = 0; t < allowed[employee].length; t++) if (allowed[employee][t] && (used & (1 << t)) == 0) {
            int score = weight * weight - (previous[employee] == t ? weight : 0) - (cycles.get(employee).contains(t) ? 1 : 0);
            best = Math.max(best, score + enumerateCycle(allowed, previous, cycles, employee + 1, used | (1 << t), memo));
        }
        return memo[employee][used] = best;
    }

    private Fixture pastryFixture() {
        boolean[][] allowed = new boolean[8][8];
        for (int e = 0; e < 8; e++) for (int t = 0; t < 8; t++) allowed[e][t] = t < 4 || (t < 6 ? e < 3 : e >= 3);
        Fixture fixture = fixture(allowed);
        String[] names = {"Barbara", "Melanye", "Agustin", "Johan", "Clei", "Johnaiker", "Rafa", "Erliud"};
        String[] taskNames = {"Basura", "Lavadero", "Refrigerador", "Chocolatera", "Camara Pasteles", "Maquinas Pasteles", "Camara Tortas", "Maquinas Tortas"};
        Area pasteles = fixture.employees.get(0).getArea(); pasteles.setNombre("Pasteles");
        Area tortas = fixture.employees.get(3).getArea(); tortas.setNombre("Tortas");
        for (int e = 0; e < 8; e++) {
            fixture.employees.get(e).setName(names[e]);
            fixture.employees.get(e).setArea(e < 3 ? pasteles : tortas);
        }
        for (int t = 0; t < 8; t++) {
            Task task = fixture.tasks.get(t);
            task.setName(taskNames[t]);
            if (t < 4) { task.setType(TaskType.GENERAL); task.setAuthorizedAreas(List.of()); }
            else {
                TaskArea link = new TaskArea(); link.setArea(t < 6 ? pasteles : tortas);
                task.setAuthorizedAreas(List.of(link));
            }
        }
        return fixture;
    }

    private Fixture fixture(boolean[][] allowed) {
        List<Employee> employees = new ArrayList<>();
        List<Task> tasks = new ArrayList<>();
        for (int e = 0; e < allowed.length; e++) {
            Area area = new Area(); area.setId((long)e + 1);
            Employee employee = new Employee(); employee.setId((long)e + 1); employee.setName("Employee " + e);
            employee.setArea(area); employee.setActive(true); employees.add(employee);
        }
        for (int t = 0; t < allowed[0].length; t++) {
            Task task = new Task(); task.setId((long)t + 1); task.setName("Task " + t); task.setType(TaskType.SPECIFIC);
            List<TaskArea> links = new ArrayList<>();
            for (int e = 0; e < allowed.length; e++) if (allowed[e][t]) {
                TaskArea link = new TaskArea(); link.setArea(employees.get(e).getArea()); links.add(link);
            }
            task.setAuthorizedAreas(links); tasks.add(task);
        }
        return new Fixture(employees, tasks, allowed);
    }

    private record Fixture(List<Employee> employees, List<Task> tasks, boolean[][] allowed) {}
}
