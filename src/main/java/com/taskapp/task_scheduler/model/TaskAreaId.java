package com.taskapp.task_scheduler.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class TaskAreaId implements Serializable {
    private Long taskId;
    private Long areaId;
}