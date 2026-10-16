package com.taskapp.task_scheduler.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.taskapp.task_scheduler.model.*;
import com.taskapp.task_scheduler.repository.*;
import com.taskapp.task_scheduler.security.OwnerAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PayrollService {
    private final PayrollRepository payrollRepository;
    private final AssignmentRepository assignmentRepository;
    private final OwnerAccess owners;

    public Optional<Payroll> getActivePayroll() {
        Long ownerId = owners.id();
        var active = payrollRepository.findByOwner_IdAndStatus(ownerId, PayrollStatus.ACTIVE);
        active.forEach(p -> checkPayroll(p, ownerId));
        return active.stream().findFirst();
    }

    public List<Payroll> getAllPayrolls() {
        Long ownerId = owners.id();
        var payrolls = payrollRepository.findAllByOwner_Id(ownerId);
        payrolls.forEach(p -> checkPayroll(p, ownerId));
        return payrolls;
    }

    public Payroll getPayroll(Long id) {
        Long ownerId = owners.id();
        var payroll = owners.require(payrollRepository.findByIdAndOwner_Id(id, ownerId));
        checkPayroll(payroll, ownerId);
        return payroll;
    }

    public List<Assignment> getAssignments(Long id) {
        Long ownerId = owners.id();
        var payroll = owners.require(payrollRepository.findByIdAndOwner_Id(id, ownerId));
        checkPayroll(payroll, ownerId);
        var assignments = assignmentRepository.findByPayroll_IdAndPayroll_Owner_Id(id, ownerId);
        assignments.forEach(a -> {
            owners.employee(a.getEmployee(), ownerId);
            if (a.getTask() != null) owners.task(a.getTask(), ownerId);
        });
        return assignments;
    }

    @Transactional
    public Payroll archiveCurrentAndCreate() {
        Long ownerId = owners.id();
        var owner = owners.lock(ownerId);
        payrollRepository.findByOwner_IdAndStatus(ownerId, PayrollStatus.ACTIVE)
                .forEach(p -> p.setStatus(PayrollStatus.ARCHIVED));
        Payroll payroll = new Payroll();
        payroll.setOwner(owner);
        payroll.setStatus(PayrollStatus.ACTIVE);
        payroll.setStartDate(LocalDate.now());
        payroll.setEndDate(LocalDate.now().plusWeeks(16));
        return payrollRepository.save(payroll);
    }

    @Transactional
    public void deletePayroll(Long id) {
        Long ownerId = owners.id();
        owners.lock(ownerId);
        var payroll = owners.require(payrollRepository.findByIdAndOwner_Id(id, ownerId));
        checkPayroll(payroll, ownerId);
        assignmentRepository.deleteAll(assignmentRepository.findByPayroll_IdAndPayroll_Owner_Id(id, ownerId));
        payrollRepository.delete(payroll);
    }

    private void checkPayroll(Payroll payroll, Long ownerId) {
        owners.check(payroll, ownerId);
        if (payroll.getTeam() != null) owners.check(payroll.getTeam(), ownerId);
    }
}