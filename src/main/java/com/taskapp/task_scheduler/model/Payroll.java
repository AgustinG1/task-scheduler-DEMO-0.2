package com.taskapp.task_scheduler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "planillas")
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate startDate;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate endDate;

    @Column(name = "estado", nullable = false)
    @Enumerated(EnumType.STRING)
    private PayrollStatus status;

    @Column(name = "generada_en", nullable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();

    // NUEVO: La planilla sabe para qué equipo se generó
    @ManyToOne(optional = true)
    @JoinColumn(name = "equipo_id")
    private Team team;
}