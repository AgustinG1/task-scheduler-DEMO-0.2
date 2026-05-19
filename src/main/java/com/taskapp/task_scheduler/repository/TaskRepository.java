package com.taskapp.task_scheduler.repository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.taskapp.task_scheduler.model.Task;
import com.taskapp.task_scheduler.model.TaskType;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    List<Task> findByType(TaskType type);
    
}
