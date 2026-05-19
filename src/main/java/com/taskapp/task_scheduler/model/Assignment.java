package com.taskapp.task_scheduler.model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity


@Table(name = "asignaciones")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "semana", nullable = false)
    private int weekNumber;

    @Column(name = "estado", nullable = false)
    @Enumerated(EnumType.STRING)
    private AssignmentStatus status;

    @ManyToOne
    @JoinColumn(name = "planilla_id", nullable = false)
    private Payroll payroll;

    @ManyToOne
    @JoinColumn(name = "empleado_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "tarea_id", nullable = true)
    private Task task;

}
