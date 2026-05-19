package com.taskapp.task_scheduler.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.taskapp.task_scheduler.model.Payroll;
import com.taskapp.task_scheduler.model.Assignment;
import com.taskapp.task_scheduler.model.Task;
import com.taskapp.task_scheduler.repository.AssignmentRepository;
import com.taskapp.task_scheduler.repository.TaskRepository;
import com.taskapp.task_scheduler.repository.TeamRepository; 
import com.taskapp.task_scheduler.service.PayrollService;
import com.taskapp.task_scheduler.service.AssignmentAlgorithm;
import com.taskapp.task_scheduler.service.ExcelExportService;

@Controller
@RequestMapping("/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;
    private final AssignmentAlgorithm assignmentAlgorithm;
    private final AssignmentRepository assignmentRepository;
    private final TaskRepository taskRepository; 
    private final TeamRepository teamRepository; 
    private final ExcelExportService excelExportService;
    
    @GetMapping
    public String verPlanilla(Model model) {
        Optional<Payroll> planillaActiva = payrollService.getActivePayroll();
        
        model.addAttribute("planilla", planillaActiva.orElse(null));
        model.addAttribute("todosLosEquipos", teamRepository.findAll()); 
        
        if (planillaActiva.isPresent()) {
            Payroll p = planillaActiva.get();
            List<Assignment> asignaciones = assignmentRepository.findByPayrollId(p.getId());
            model.addAttribute("asignaciones", asignaciones);
            model.addAttribute("equipoActivo", p.getTeam());
            
            // Extraer las columnas dinámicamente de las asignaciones reales
            List<Task> tareasReales = asignaciones.stream()
                    .map(Assignment::getTask) 
                    .filter(Objects::nonNull) 
                    .distinct() 
                    .collect(Collectors.toList());
            
            // --- BLOQUE DE DEBUG PARA LA CONSOLA ---
            System.out.println("====== DEBUG DEL CONTROLADOR ======");
            System.out.println("Cantidad de tareas extraídas de las asignaciones: " + tareasReales.size());
            for (Task t : tareasReales) {
                System.out.println("- Columna a dibujar: " + t.getName());
            }
            
            if (tareasReales.isEmpty()) {
                System.out.println("¡ALERTA! tareasReales estaba vacía. Se activó el Fallback (findAll).");
                tareasReales = taskRepository.findAll();
            }
            System.out.println("===================================");
            
            model.addAttribute("tareas", tareasReales);
            
            int totalSemanas = asignaciones.stream()
                    .mapToInt(Assignment::getWeekNumber)
                    .max()
                    .orElse(16);
            model.addAttribute("totalSemanas", totalSemanas);
        }
        
        return "payroll/vista";
    }

    @PostMapping("/generar")
    public String generar(@RequestParam Long teamId, @RequestParam(defaultValue = "16") int semanas) {
        assignmentAlgorithm.generatePayroll(semanas, teamId);
        return "redirect:/payroll";
    }

    @GetMapping("/historial")
    public String historial(Model model) {
        model.addAttribute("planillas", payrollService.getAllPayrolls());
        return "payroll/historial";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarPlanilla(@PathVariable Long id) {
        payrollService.deletePayroll(id);
        return "redirect:/payroll/historial";
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadExcel(@PathVariable Long id) {
        byte[] excelData = excelExportService.generateExcelForPayroll(id);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Matriz_Turnos_Pasteleria.xlsx");
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelData);
    }
}