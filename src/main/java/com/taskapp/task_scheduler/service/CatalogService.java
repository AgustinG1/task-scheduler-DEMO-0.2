package com.taskapp.task_scheduler.service;

import java.util.List;
import com.taskapp.task_scheduler.model.TaskGroup;
import com.taskapp.task_scheduler.repository.TaskGroupRepository;
import com.taskapp.task_scheduler.repository.TaskRepository;
import com.taskapp.task_scheduler.security.OwnerAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final TaskGroupRepository groups;
    private final TaskRepository tasks;
    private final OwnerAccess owners;

    public List<TaskGroup> list() {
        Long ownerId = owners.id();
        var result = groups.findAllByOwner_Id(ownerId);
        result.forEach(g -> owners.catalog(g, ownerId));
        return result;
    }

    public TaskGroup get(Long id) {
        Long ownerId = owners.id();
        var group = owners.require(groups.findByIdAndOwner_Id(id, ownerId));
        owners.catalog(group, ownerId);
        return group;
    }

    @Transactional
    public void create(TaskGroup input) {
        Long ownerId = owners.id();
        var owner = owners.lock(ownerId);
        var group = new TaskGroup();
        group.setName(input.getName());
        group.setDescription(input.getDescription());
        group.setOwner(owner);
        groups.save(group);
    }

    @Transactional
    public void update(TaskGroup input, List<Long> taskIds) {
        Long ownerId = owners.id();
        owners.lock(ownerId);
        var group = owners.require(groups.findByIdAndOwner_Id(input.getId(), ownerId));
        owners.catalog(group, ownerId);
        var selected = owners.selection(taskIds, ids -> tasks.findAllByOwner_IdAndIdIn(ownerId, ids));
        selected.forEach(t -> owners.task(t, ownerId));
        group.setName(input.getName());
        group.setDescription(input.getDescription());
        group.setTasks(selected);
    }

    @Transactional
    public void delete(Long id) {
        Long ownerId = owners.id();
        owners.lock(ownerId);
        var group = owners.require(groups.findByIdAndOwner_Id(id, ownerId));
        owners.catalog(group, ownerId);
        group.getTeams().forEach(t -> owners.check(t, ownerId));
        group.getTasks().clear();
        group.getTeams().forEach(t -> t.getTaskGroups().remove(group));
        group.getTeams().clear();
        groups.delete(group);
    }
}
