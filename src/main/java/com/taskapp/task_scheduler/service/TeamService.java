package com.taskapp.task_scheduler.service;

import java.util.List;
import com.taskapp.task_scheduler.model.Team;
import com.taskapp.task_scheduler.repository.*;
import com.taskapp.task_scheduler.security.OwnerAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamService {
    private final TeamRepository teams;
    private final AreaRepository areas;
    private final TaskGroupRepository groups;
    private final EmployeeRepository employees;
    private final OwnerAccess owners;

    public List<Team> list() {
        Long ownerId = owners.id();
        var result = teams.findAllByOwner_Id(ownerId);
        result.forEach(t -> owners.team(t, ownerId));
        return result;
    }

    public Team get(Long id) {
        Long ownerId = owners.id();
        var team = owners.require(teams.findByIdAndOwner_Id(id, ownerId));
        owners.team(team, ownerId);
        return team;
    }

    @Transactional
    public void create(Team input, List<Long> areaIds) {
        Long ownerId = owners.id();
        var owner = owners.lock(ownerId);
        var selected = owners.selection(areaIds, ids -> areas.findAllByOwner_IdAndIdIn(ownerId, ids));
        Team team = new Team();
        team.setName(input.getName());
        team.setDescription(input.getDescription());
        team.setAreas(selected);
        team.setOwner(owner);
        teams.save(team);
    }

    @Transactional
    public void update(Team input, List<Long> areaIds, List<Long> groupIds) {
        Long ownerId = owners.id();
        owners.lock(ownerId);
        Team team = owners.require(teams.findByIdAndOwner_Id(input.getId(), ownerId));
        owners.team(team, ownerId);
        var selectedAreas = owners.selection(areaIds, ids -> areas.findAllByOwner_IdAndIdIn(ownerId, ids));
        var selectedGroups = owners.selection(groupIds, ids -> groups.findAllByOwner_IdAndIdIn(ownerId, ids));
        selectedGroups.forEach(g -> owners.catalog(g, ownerId));
        team.setName(input.getName());
        team.setDescription(input.getDescription());
        team.setAreas(selectedAreas);
        team.setTaskGroups(selectedGroups);
    }

    @Transactional
    public void delete(Long id) {
        Long ownerId = owners.id();
        owners.lock(ownerId);
        var team = owners.require(teams.findByIdAndOwner_Id(id, ownerId));
        owners.team(team, ownerId);
        team.getEmployees().forEach(e -> owners.employee(e, ownerId));
        team.getTasks().forEach(t -> owners.task(t, ownerId));
        team.getEmployees().forEach(e -> e.getTeams().remove(team));
        team.getTasks().forEach(t -> t.getTeams().remove(team));
        team.getEmployees().clear();
        team.getTasks().clear();
        team.getAreas().clear();
        team.getTaskGroups().clear();
        teams.delete(team);
    }

    @Transactional
    public void updateEmployee(Long teamId, Long employeeId, Long areaId, boolean active) {
        Long ownerId = owners.id();
        owners.lock(ownerId);
        var team = owners.require(teams.findByIdAndOwner_Id(teamId, ownerId));
        owners.team(team, ownerId);
        var employee = owners.require(employees.findByIdAndOwner_Id(employeeId, ownerId));
        var area = owners.require(areas.findByIdAndOwner_Id(areaId, ownerId));
        owners.employee(employee, ownerId);
        employee.setArea(area);
        employee.setActive(active);
    }
}
