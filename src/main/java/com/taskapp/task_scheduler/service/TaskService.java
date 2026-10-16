package com.taskapp.task_scheduler.service;

import java.util.List;
import com.taskapp.task_scheduler.model.*;
import com.taskapp.task_scheduler.repository.*;
import com.taskapp.task_scheduler.security.OwnerAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final AreaRepository areaRepository;
    private final TaskAreaRepository taskAreaRepository;
    private final AssignmentRepository assignmentRepository;
    private final OwnerAccess owners;

    public List<Task> getAllTasks() {
        Long ownerId = owners.id();
        var tasks = taskRepository.findAllByOwner_Id(ownerId);
        tasks.forEach(t -> owners.task(t, ownerId));
        return tasks;
    }

    public Task getTaskById(Long id) {
        Long ownerId = owners.id();
        Task task = owners.require(taskRepository.findByIdAndOwner_Id(id, ownerId));
        owners.task(task, ownerId);
        return task;
    }

    public List<Task> getTasksByType(TaskType type) {
        Long ownerId = owners.id();
        var tasks = taskRepository.findByOwner_IdAndType(ownerId, type);
        tasks.forEach(t -> owners.task(t, ownerId));
        return tasks;
    }

    @Transactional
    public Task createTask(Task input) { return createTask(input, null); }

    @Transactional
    public Task createTask(Task input, Long areaId) {
        Long ownerId = owners.id();
        var owner = owners.lock(ownerId);
        Area area = areaId == null ? null : owners.require(areaRepository.findByIdAndOwner_Id(areaId, ownerId));
        Task task = new Task();
        task.setName(input.getName());
        task.setDescription(input.getDescription());
        task.setType(input.getType());
        task.setOwner(owner);
        task = taskRepository.save(task);
        if (task.getType() == TaskType.SPECIFIC && area != null) link(task, area);
        return task;
    }

    @Transactional
    public Task updateTask(Long id, Task input) {
        Long ownerId = owners.id();
        owners.lock(ownerId);
        Task task = owners.require(taskRepository.findByIdAndOwner_Id(id, ownerId));
        owners.task(task, ownerId);
        task.setName(input.getName());
        task.setDescription(input.getDescription());
        task.setType(input.getType());
        if (task.getType() == TaskType.GENERAL) {
            taskAreaRepository.deleteByTaskId(id);
            task.getAuthorizedAreas().clear();
        }
        return taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(Long id) {
        Long ownerId = owners.id();
        owners.lock(ownerId);
        Task task = owners.require(taskRepository.findByIdAndOwner_Id(id, ownerId));
        owners.task(task, ownerId);
        task.getTaskGroups().forEach(group -> owners.check(group, ownerId));
        task.getTeams().forEach(team -> owners.check(team, ownerId));
        var assignments = assignmentRepository.findByTask_IdAndPayroll_Owner_Id(id, ownerId);
        assignments.forEach(a -> owners.employee(a.getEmployee(), ownerId));
        taskAreaRepository.deleteByTaskId(id);
        task.getAuthorizedAreas().clear();
        for (Assignment assignment : assignments) {
            assignment.setTask(null);
            assignment.setStatus(AssignmentStatus.REST);
        }
        task.getTeams().clear();
        taskRepository.delete(task);
    }

    @Transactional
    public void vincularArea(Task input, Long areaId) {
        Long ownerId = owners.id();
        owners.lock(ownerId);
        Task task = owners.require(taskRepository.findByIdAndOwner_Id(input.getId(), ownerId));
        Area area = owners.require(areaRepository.findByIdAndOwner_Id(areaId, ownerId));
        link(task, area);
    }

    private void link(Task task, Area area) {
        TaskArea link = new TaskArea();
        link.setId(new TaskAreaId(task.getId(), area.getId()));
        link.setTask(task);
        link.setArea(area);
        taskAreaRepository.save(link);
    }
}