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
        long areasCount = areaRepository.count();
        long tasksCount = taskRepository.count();
        long employeesCount = employeeRepository.countByActiveTrue();
        long groupsCount = taskGroupRepository.count();
        long teamsCount = teamRepository.count();
        long payrollsCount = payrollRepository.count();
        long modulosConDatos = 0;

        if (areasCount > 0) {
            modulosConDatos++;
        }
        if (tasksCount > 0) {
            modulosConDatos++;
        }
        if (employeesCount > 0) {
            modulosConDatos++;
        }
        if (groupsCount > 0) {
            modulosConDatos++;
        }
        if (teamsCount > 0) {
            modulosConDatos++;
        }
        if (payrollsCount > 0) {
            modulosConDatos++;
        }

        model.addAttribute("modulosConDatos", modulosConDatos);
        model.addAttribute("areasCount", areasCount);
        model.addAttribute("tasksCount", tasksCount);
        model.addAttribute("employeesCount", employeesCount);
        model.addAttribute("groupsCount", groupsCount);
        model.addAttribute("teamsCount", teamsCount);
        model.addAttribute("payrollsCount", payrollsCount);
        return "index";
    }
}
