package com.taskapp.task_scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.taskapp.task_scheduler.model.TaskArea;
import com.taskapp.task_scheduler.model.TaskAreaId;

@Repository
public interface TaskAreaRepository extends JpaRepository<TaskArea, TaskAreaId> {
    void deleteByTaskId(Long taskId);
}
