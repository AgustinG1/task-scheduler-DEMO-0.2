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

    // 1. Traer la planilla activa (usamos el método que creaste en el repositorio)
    public Optional<Payroll> getActivePayroll() {
        return payrollRepository.findByStatus(PayrollStatus.ACTIVE);
    }

    // 2. Traer todo el historial de planillas
    public List<Payroll> getAllPayrolls() {
        return payrollRepository.findAll();
    }

    // 3. Archivar la actual (si existe) y crear una nueva
    public Payroll archiveCurrentAndCreate() {
        // Paso A: Buscamos si hay una planilla activa en este momento
        Optional<Payroll> planillaActivaOpt = getActivePayroll();
        
        // Si existe, la "archivamos" para que no moleste a la nueva
        if (planillaActivaOpt.isPresent()) {
            Payroll planillaActiva = planillaActivaOpt.get();
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