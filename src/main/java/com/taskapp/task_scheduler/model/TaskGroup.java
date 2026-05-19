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
@Table(name = "catalogos_tareas") // Usamos nombres en español para la BD
public class TaskGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    @NotBlank(message = "El nombre del catálogo es obligatorio")
    private String name; // Ej: "Tareas Base de Cocina"

    @Column(name = "descripcion", length = 255)
    private String description;

    // Relación: Un catálogo contiene muchas tareas
    @ManyToMany
    @JoinTable(
        name = "catalogo_tarea_detalle",
        joinColumns = @JoinColumn(name = "catalogo_id"),
        inverseJoinColumns = @JoinColumn(name = "tarea_id")
    )
    private List<Task> tasks = new ArrayList<>();

    // Relación bidireccional: Saber a qué equipos está asignado este catálogo
    @ManyToMany(mappedBy = "taskGroups")
    private List<Team> teams = new ArrayList<>();
}