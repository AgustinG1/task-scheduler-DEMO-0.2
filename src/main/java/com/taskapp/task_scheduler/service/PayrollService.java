package com.taskapp.task_scheduler.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import com.taskapp.task_scheduler.model.Assignment;
import com.taskapp.task_scheduler.model.Payroll;
import com.taskapp.task_scheduler.model.PayrollStatus;
import com.taskapp.task_scheduler.repository.PayrollRepository;
import org.springframework.transaction.annotation.Transactional;
import com.taskapp.task_scheduler.repository.AssignmentRepository;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final PayrollRepository payrollRepository;
    private final AssignmentRepository assignmentRepository; // Añadido para poder borrar los turnos vinculados

    // 1. Traer la planilla activa (toma la primera si hay varias)
    public Optional<Payroll> getActivePayroll() {
        List<Payroll> activas = payrollRepository.findByStatus(PayrollStatus.ACTIVE);
        return activas.isEmpty() ? Optional.empty() : Optional.of(activas.get(0));
    }

    // 2. Traer todo el historial de planillas
    public List<Payroll> getAllPayrolls() {
        return payrollRepository.findAll();
    }

    // 3. Archivar TODAS las activas (si existen) y crear una nueva
    public Payroll archiveCurrentAndCreate() {
        // Paso A: Archivamos TODAS las planillas activas (por seguridad)
        List<Payroll> activas = payrollRepository.findByStatus(PayrollStatus.ACTIVE);
        for (Payroll planillaActiva : activas) {
            planillaActiva.setStatus(PayrollStatus.ARCHIVED);
            payrollRepository.save(planillaActiva);
        }

        // Paso B: Preparamos la nueva planilla en blanco
        Payroll nuevaPlanilla = new Payroll();
        nuevaPlanilla.setStatus(PayrollStatus.ACTIVE);
        
        // Configuramos que empiece hoy y termine exactamente en 16 semanas
        nuevaPlanilla.setStartDate(LocalDate.now());
        nuevaPlanilla.setEndDate(LocalDate.now().plusWeeks(16));
        
        // Paso C: Guardamos y devolvemos la nueva planilla
        return payrollRepository.save(nuevaPlanilla);
    }
    
    @Transactional
    public void deletePayroll(Long id) {
        // 1. Borrar todas las asignaciones (turnos) vinculadas a esta planilla
        List<Assignment> asignaciones = assignmentRepository.findByPayrollId(id);
        assignmentRepository.deleteAll(asignaciones);
        
        // 2. Borrar la planilla limpia
        payrollRepository.deleteById(id);
    }
    
}