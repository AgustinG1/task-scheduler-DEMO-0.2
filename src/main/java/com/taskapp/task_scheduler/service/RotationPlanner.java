package com.taskapp.task_scheduler.service;

import com.taskapp.task_scheduler.model.Employee;
import com.taskapp.task_scheduler.model.Task;
import com.taskapp.task_scheduler.model.TaskType;

import java.util.*;

/** Plans a whole payroll in memory; all history belongs to this invocation. */
final class RotationPlanner {
    private static final long FORBIDDEN = Long.MAX_VALUE / 4;

    List<Map<Long, Task>> plan(List<Employee> employees, List<Task> tasks, int weeks, Random random) {
        List<Employee> orderedEmployees = new ArrayList<>(employees);
        List<Task> orderedTasks = new ArrayList<>(tasks);
        Collections.shuffle(orderedEmployees, random);
        Collections.shuffle(orderedTasks, random);
        int people = orderedEmployees.size(), taskCount = orderedTasks.size();
        boolean[][] allowed = new boolean[people][taskCount];
        Map<List<Boolean>, List<Integer>> groups = new LinkedHashMap<>();
        for (int e = 0; e < people; e++) {
            List<Boolean> signature = new ArrayList<>();
            for (int t = 0; t < taskCount; t++) {
                allowed[e][t] = isAuthorized(orderedEmployees.get(e), orderedTasks.get(t));
                signature.add(allowed[e][t]);
            }
            groups.computeIfAbsent(signature, key -> new ArrayList<>()).add(e);
        }

        // Each person chooses one real task or a dummy rest slot. The weights
        // exceed the entire lower-priority cost of a week, not just one edge.
        long varietyBound = people * (2L * weeks + 1);
        long cycleWeight = varietyBound + 1;
        long repeatWeight = people * cycleWeight + varietyBound + 1;
        long restWeight = people * repeatWeight + people * cycleWeight + varietyBound + 1;
        int[][] counts = new int[people][taskCount + 1];
        boolean[][] cycle = new boolean[people][taskCount];
        int[][] schedule = new int[weeks][people];
        for (int week = 0; week < weeks; week++) {
            long[][] costs = new long[people][taskCount + people];
            for (int e = 0; e < people; e++) {
                for (int t = 0; t < taskCount; t++) {
                    costs[e][t] = allowed[e][t]
                            ? 2L * counts[e][t] + 1
                                + (week > 0 && schedule[week - 1][e] == t ? repeatWeight : 0)
                                + (cycle[e][t] ? cycleWeight : 0)
                            : FORBIDDEN;
                }
                Arrays.fill(costs[e], taskCount, taskCount + people,
                        restWeight + 2L * counts[e][taskCount] + 1);
            }
            int[] matching = minimumCostAssignment(costs);
            for (int e = 0; e < people; e++) {
                int task = Math.min(matching[e], taskCount);
                schedule[week][e] = task;
                counts[e][task]++;
                recordCycle(cycle[e], allowed[e], task);
            }
        }

        balanceSchedule(schedule, counts, groups.values(), allowed, taskCount, cycleWeight, repeatWeight, restWeight);
        List<Map<Long, Task>> result = new ArrayList<>();
        for (int[] week : schedule) {
            Map<Long, Task> assignments = new LinkedHashMap<>();
            for (int e = 0; e < people; e++) {
                if (week[e] < taskCount) assignments.put(orderedEmployees.get(e).getId(), orderedTasks.get(week[e]));
            }
            result.add(assignments);
        }
        return result;
    }

    private boolean isAuthorized(Employee employee, Task task) {
        if (task.getType() == TaskType.GENERAL) return true;
        return task.getType() == TaskType.SPECIFIC && task.getAuthorizedAreas() != null
                && task.getAuthorizedAreas().stream()
                    .anyMatch(link -> link.getArea().getId().equals(employee.getArea().getId()));
    }

    /** Redistribute existing weekly slots only among people with identical authorizations.
     * Coverage and the authorized task multiset of every group/week stay unchanged.
     * Strictly improving integer costs guarantee termination. */
    private void balanceSchedule(int[][] schedule, int[][] counts, Collection<List<Integer>> groups,
            boolean[][] allowed, int rest, long cycleWeight, long repeatWeight, long restWeight) {
        boolean improved;
        do {
            improved = false;
            for (int w = 0; w < schedule.length; w++) {
                boolean[][] cycle = cyclesBefore(schedule, allowed, w);
                for (List<Integer> group : groups) {
                    int size = group.size();
                    if (size < 2) continue;
                    int[] slots = new int[size];
                    for (int i = 0; i < size; i++) slots[i] = schedule[w][group.get(i)];
                    long[][] costs = new long[size][size];
                    for (int i = 0; i < size; i++) {
                        int e = group.get(i);
                        for (int j = 0; j < size; j++) {
                            int task = slots[j];
                            int countWithoutThisWeek = counts[e][task] - (slots[i] == task ? 1 : 0);
                            int repeats = task == rest ? 0
                                    : (w > 0 && schedule[w - 1][e] == task ? 1 : 0)
                                    + (w + 1 < schedule.length && schedule[w + 1][e] == task ? 1 : 0);
                            costs[i][j] = 2L * countWithoutThisWeek + 1 + repeats * repeatWeight
                                    + (task < rest && cycle[e][task] ? cycleWeight : 0);
                        }
                    }
                    int[] matching = minimumCostAssignment(costs);
                    long before = 0, after = 0;
                    for (int i = 0; i < size; i++) {
                        before += costs[i][i];
                        after += costs[i][matching[i]];
                    }
                    if (after >= before) continue;
                    for (int i = 0; i < size; i++) schedule[w][group.get(i)] = slots[matching[i]];
                    if (!respectsCycles(schedule, allowed, cycleWeight, repeatWeight, restWeight)) {
                        for (int i = 0; i < size; i++) schedule[w][group.get(i)] = slots[i];
                        continue;
                    }
                    for (int i = 0; i < size; i++) {
                        int e = group.get(i), task = slots[matching[i]];
                        counts[e][slots[i]]--;
                        counts[e][task]++;
                        schedule[w][e] = task;
                    }
                    improved = true;
                }
            }
            // A fairer allocation can require moving a run of weeks together:
            // changing only one week may be blocked by its two neighbours.
            if (!improved) improved = improveBlock(schedule, counts, groups, allowed, rest, cycleWeight, repeatWeight, restWeight);
        } while (improved);
    }

    private boolean improveBlock(int[][] schedule, int[][] counts, Collection<List<Integer>> groups,
            boolean[][] allowed, int rest, long cycleWeight, long repeatWeight, long restWeight) {
        for (List<Integer> group : groups) {
            for (int a = 0; a < group.size(); a++) for (int b = a + 1; b < group.size(); b++) {
                int first = group.get(a), second = group.get(b);
                for (int start = 0; start < schedule.length; start++) {
                    int[] changes = new int[rest + 1];
                    for (int end = start; end < schedule.length; end++) {
                        changes[schedule[end][first]]--;
                        changes[schedule[end][second]]++;
                        long delta = 0;
                        for (int task = 0; task <= rest; task++) {
                            delta += 2L * changes[task] * (counts[first][task] - counts[second][task] + changes[task]);
                        }
                        if (delta >= 0 || !canSwapBoundary(schedule, start - 1, start, first, second, rest)
                                || !canSwapBoundary(schedule, end + 1, end, first, second, rest)) continue;
                        for (int week = start; week <= end; week++) {
                            int task = schedule[week][first];
                            schedule[week][first] = schedule[week][second];
                            schedule[week][second] = task;
                        }
                        if (!respectsCycles(schedule, allowed, cycleWeight, repeatWeight, restWeight)) {
                            for (int week = start; week <= end; week++) {
                                int task = schedule[week][first];
                                schedule[week][first] = schedule[week][second];
                                schedule[week][second] = task;
                            }
                            continue;
                        }
                        for (int task = 0; task <= rest; task++) {
                            counts[first][task] += changes[task];
                            counts[second][task] -= changes[task];
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean canSwapBoundary(int[][] schedule, int outside, int inside, int first, int second, int rest) {
        if (outside < 0 || outside >= schedule.length) return true;
        int before = repeats(schedule[outside][first], schedule[inside][first], rest)
                + repeats(schedule[outside][second], schedule[inside][second], rest);
        int after = repeats(schedule[outside][first], schedule[inside][second], rest)
                + repeats(schedule[outside][second], schedule[inside][first], rest);
        return after <= before;
    }

    private int repeats(int previous, int next, int rest) {
        return previous != rest && previous == next ? 1 : 0;
    }

    private boolean[][] cyclesBefore(int[][] schedule, boolean[][] allowed, int end) {
        boolean[][] cycle = new boolean[allowed.length][allowed[0].length];
        for (int w = 0; w < end; w++) for (int e = 0; e < allowed.length; e++) {
            recordCycle(cycle[e], allowed[e], schedule[w][e]);
        }
        return cycle;
    }

    private void recordCycle(boolean[] cycle, boolean[] allowed, int task) {
        if (task == cycle.length) return;
        cycle[task] = true;
        for (int t = 0; t < allowed.length; t++) if (allowed[t] && !cycle[t]) return;
        Arrays.fill(cycle, false);
    }

    /** A fairness adjustment must not undo cycle protection, including in later
     * weeks whose cycle may have changed as a consequence of the adjustment. */
    private boolean respectsCycles(int[][] schedule, boolean[][] allowed,
            long cycleWeight, long repeatWeight, long restWeight) {
        int people = allowed.length, tasks = allowed[0].length;
        boolean[][] cycle = new boolean[people][tasks];
        for (int w = 0; w < schedule.length; w++) {
            long[][] costs = new long[people][tasks + people];
            long actual = 0;
            for (int e = 0; e < people; e++) {
                for (int t = 0; t < tasks; t++) {
                    costs[e][t] = !allowed[e][t] ? FORBIDDEN
                            : (w > 0 && schedule[w - 1][e] == t ? repeatWeight : 0)
                                + (cycle[e][t] ? cycleWeight : 0);
                }
                Arrays.fill(costs[e], tasks, tasks + people, restWeight);
                actual += costs[e][schedule[w][e]];
            }
            int[] matching = minimumCostAssignment(costs);
            long optimum = 0;
            for (int e = 0; e < people; e++) optimum += costs[e][matching[e]];
            if (actual != optimum) return false;
            for (int e = 0; e < people; e++) recordCycle(cycle[e], allowed[e], schedule[w][e]);
        }
        return true;
    }

    /** Rectangular Hungarian algorithm: rows <= columns; one distinct column per row.
     * Potentials account for displacement chains, so preferences are optimized for
     * the complete matching instead of a greedy choice for each task. */
    private int[] minimumCostAssignment(long[][] costs) {
        int rows = costs.length, columns = costs[0].length;
        long[] rowPotential = new long[rows + 1], columnPotential = new long[columns + 1];
        int[] owner = new int[columns + 1], previous = new int[columns + 1];
        for (int row = 1; row <= rows; row++) {
            owner[0] = row;
            int column = 0;
            long[] distance = new long[columns + 1];
            Arrays.fill(distance, FORBIDDEN);
            boolean[] visited = new boolean[columns + 1];
            do {
                visited[column] = true;
                int currentRow = owner[column], next = 0;
                long delta = FORBIDDEN;
                for (int j = 1; j <= columns; j++) {
                    if (visited[j]) continue;
                    long reducedCost = costs[currentRow - 1][j - 1] - rowPotential[currentRow] - columnPotential[j];
                    if (reducedCost < distance[j]) {
                        distance[j] = reducedCost;
                        previous[j] = column;
                    }
                    if (distance[j] < delta) { delta = distance[j]; next = j; }
                }
                for (int j = 0; j <= columns; j++) {
                    if (visited[j]) { rowPotential[owner[j]] += delta; columnPotential[j] -= delta; }
                    else distance[j] -= delta;
                }
                column = next;
            } while (owner[column] != 0);
            do {
                int next = previous[column];
                owner[column] = owner[next];
                column = next;
            } while (column != 0);
        }
        int[] result = new int[rows];
        for (int j = 1; j <= columns; j++) if (owner[j] != 0) result[owner[j] - 1] = j - 1;
        return result;
    }
}
