package com.taskapp.task_scheduler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
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
