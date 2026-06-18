package com.taskapp.task_scheduler.controller;

import com.taskapp.task_scheduler.repository.AreaRepository;
import com.taskapp.task_scheduler.repository.EmployeeRepository;
import com.taskapp.task_scheduler.repository.PayrollRepository;
import com.taskapp.task_scheduler.repository.TaskGroupRepository;
import com.taskapp.task_scheduler.repository.TaskRepository;
import com.taskapp.task_scheduler.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final AreaRepository areaRepository;
    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final TaskGroupRepository taskGroupRepository;
    private final TeamRepository teamRepository;
    private final PayrollRepository payrollRepository;

    @GetMapping("/")
    public String index(Model model) {
        long modulosConDatos = 0;

        if (areaRepository.count() > 0) {
            modulosConDatos++;
        }
        if (taskRepository.count() > 0) {
            modulosConDatos++;
        }
        if (employeeRepository.count() > 0) {
            modulosConDatos++;
        }
        if (taskGroupRepository.count() > 0) {
            modulosConDatos++;
        }
        if (teamRepository.count() > 0) {
            modulosConDatos++;
        }
        if (payrollRepository.count() > 0) {
            modulosConDatos++;
        }

        model.addAttribute("modulosConDatos", modulosConDatos);
        return "index";
    }
}