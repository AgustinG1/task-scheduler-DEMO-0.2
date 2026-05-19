package com.taskapp.task_scheduler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tarea_area")
public class TaskArea {

    @EmbeddedId
    private TaskAreaId id;

    @ManyToOne
    @MapsId("taskId")
    @JoinColumn(name = "tarea_id", nullable = false)
    private Task task;

    @ManyToOne
    @MapsId("areaId")
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;
}