package com.taskapp.task_scheduler.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "equipos") // Siguiendo tu convención de nombres en español
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    @NotBlank(message = "El nombre del equipo es obligatorio")
    private String name; // Ej: "Sucursal Macul - Turno Mañana"

    @Column(name = "descripcion", length = 255)
    private String description;

    // Relación bidireccional: El equipo conoce a sus empleados
    @ManyToMany(mappedBy = "teams")
    private List<Employee> employees = new ArrayList<>();

    // Relación bidireccional: El equipo conoce sus tareas disponibles
    @ManyToMany(mappedBy = "teams")
    private List<Task> tasks = new ArrayList<>();

    // El equipo contiene múltiples áreas operativas
    @ManyToMany
    @JoinTable(
        name = "equipo_area",
        joinColumns = @JoinColumn(name = "equipo_id"),
        inverseJoinColumns = @JoinColumn(name = "area_id")
    )
    private List<Area> areas = new ArrayList<>();

    // NUEVO: Catálogos/Paquetes de tareas asignados a esta sucursal
    @ManyToMany
    @JoinTable(
        name = "equipo_catalogo",
        joinColumns = @JoinColumn(name = "equipo_id"),
        inverseJoinColumns = @JoinColumn(name = "catalogo_id")
    )
    private List<TaskGroup> taskGroups = new ArrayList<>();
}