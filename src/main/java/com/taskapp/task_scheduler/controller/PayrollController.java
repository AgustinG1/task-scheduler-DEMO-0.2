package com.taskapp.task_scheduler.controller;

import java.util.Objects;
import com.taskapp.task_scheduler.model.*;
import com.taskapp.task_scheduler.service.*;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/payroll")
@RequiredArgsConstructor
public class PayrollController {
    private final PayrollService payrollService;
    private final AssignmentAlgorithm assignmentAlgorithm;
    private final TeamService teamService;
    private final ExcelExportService excelExportService;

    @GetMapping
    public String verPlanilla(Model model) {
        var active = payrollService.getActivePayroll();
        model.addAttribute("planilla", active.orElse(null));
        model.addAttribute("todosLosEquipos", teamService.list());
        if (active.isPresent()) {
            var payroll = active.get();
            var assignments = payrollService.getAssignments(payroll.getId());
            model.addAttribute("asignaciones", assignments);
            model.addAttribute("equipoActivo", payroll.getTeam());
            model.addAttribute("tareas", assignments.stream().map(Assignment::getTask)
                    .filter(Objects::nonNull).distinct().toList());
            model.addAttribute("totalSemanas", assignments.stream().mapToInt(Assignment::getWeekNumber).max().orElse(16));
        }
        return "payroll/vista";
    }

    @PostMapping("/generar")
    public String generar(@RequestParam Long teamId, @RequestParam(defaultValue = "16") int semanas,
            RedirectAttributes redirectAttributes) {
        try {
            assignmentAlgorithm.generatePayroll(semanas, teamId);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/payroll";
    }

    @GetMapping("/historial")
    public String historial(Model model) {
        model.addAttribute("planillas", payrollService.getAllPayrolls());
        return "payroll/historial";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarPlanilla(@PathVariable Long id) {
        payrollService.deletePayroll(id);
        return "redirect:/payroll/historial";
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadExcel(@PathVariable Long id) {
        byte[] bytes = excelExportService.generateExcelForPayroll(id);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=Matriz_Turnos_Pasteleria.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(bytes);
    }
}