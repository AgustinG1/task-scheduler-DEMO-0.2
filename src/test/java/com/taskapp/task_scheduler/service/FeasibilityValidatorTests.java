package com.taskapp.task_scheduler.service;

import com.taskapp.task_scheduler.model.Employee;
import com.taskapp.task_scheduler.model.Task;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FeasibilityValidatorTests {

    private final FeasibilityValidator validator = new FeasibilityValidator();

    @Test
    void rejectsAnEmptyTeam() {
        assertThrows(IllegalStateException.class, () -> validator.validar(List.of(), List.of(new Task())));
    }

    @Test
    void rejectsAnEmptyTaskCatalog() {
        assertThrows(IllegalStateException.class, () -> validator.validar(List.of(new Employee()), List.of()));
    }
}
