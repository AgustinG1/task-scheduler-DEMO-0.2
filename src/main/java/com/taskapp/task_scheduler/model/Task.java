package com.taskapp.task_scheduler.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tareas")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    @NotBlank(message = "El título es obligatorio")
    private String name;

    @Column(name = "descripcion", length = 255)
    private String description;

    @Column(name = "tipo", nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskType type;

    // Relación agregada para conectar la tarea con sus áreas autorizadas
    @OneToMany(mappedBy = "task", fetch = FetchType.EAGER)
    private List<TaskArea> authorizedAreas = new ArrayList<>();
    
    // Genera una etiqueta visual amigable para la interfaz
    public String getLabelIntuitivo() {
        if (this.type == TaskType.GENERAL) {
            return "General";
        } else {
            // Si es SPECIFIC, buscamos a qué áreas pertenece
            if (this.authorizedAreas == null || this.authorizedAreas.isEmpty()) {
                return "Específico (Sin área)";
            }
            
            // Extraemos los nombres de las áreas y los unimos separados por coma
            List<String> nombresAreas = this.authorizedAreas.stream()
                    .map(ta -> ta.getArea().getNombre())
                    .toList();
            
            return "Específico de " + String.join(", ", nombresAreas);
        }
    }

    // Esta tarea está disponible en estos equipos/sucursales
    @ManyToMany
    @JoinTable(
        name = "tarea_equipo",
        joinColumns = @JoinColumn(name = "tarea_id"),
        inverseJoinColumns = @JoinColumn(name = "equipo_id")
    )
    private List<Team> teams = new ArrayList<>();

    // Esta tarea está incluida en estos catálogos/grupos
    @ManyToMany(mappedBy = "tasks")
    private List<TaskGroup> taskGroups = new ArrayList<>();

    // Desvincular automáticamente la tarea de los catálogos antes de borrarla en la BD
    @PreRemove
    private void removerDeCatalogosAntesDeEliminar() {
        if (this.taskGroups != null) {
            for (TaskGroup grupo : this.taskGroups) {
                grupo.getTasks().remove(this);
            }
        }
    }
}