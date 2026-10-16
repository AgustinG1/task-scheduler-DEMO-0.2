package com.taskapp.task_scheduler.controller;

import java.util.List;
import com.taskapp.task_scheduler.model.Team;
import com.taskapp.task_scheduler.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {
    private final TeamService teams;
    private final AreaService areas;
    private final EmployeeService employees;
    private final CatalogService catalogs;

    @InitBinder
    void fields(WebDataBinder binder) { binder.setAllowedFields("id", "name", "description"); }

    @GetMapping
    public String listarEquipos(Model model) {
        model.addAttribute("equipos", teams.list());
        model.addAttribute("nuevoEquipo", new Team());
        model.addAttribute("todasLasAreas", areas.getAllAreas());
        return "teams/teams";
    }

    @PostMapping("/guardar")
    public String guardarEquipo(@ModelAttribute("nuevoEquipo") Team input,
            @RequestParam(value = "areasIds", required = false) List<Long> ids) {
        teams.create(input, ids);
        return "redirect:/teams";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarEquipo(@PathVariable Long id) {
        teams.delete(id);
        return "redirect:/teams";
    }

    @GetMapping("/editar/{id}")
    public String editarEquipo(@PathVariable Long id, Model model) {
        Team team = teams.get(id);
        model.addAttribute("equipo", team);
        model.addAttribute("todasLasAreas", areas.getAllAreas());
        model.addAttribute("todosLosEmpleados", employees.getAllEmployees().stream()
                .filter(e -> team.getAreas().contains(e.getArea())).toList());
        model.addAttribute("todosLosCatalogos", catalogs.list());
        return "teams/team-edit";
    }

    @PostMapping("/actualizar")
    public String actualizarEquipo(@ModelAttribute("equipo") Team input,
            @RequestParam(value = "areasIds", required = false) List<Long> areaIds,
            @RequestParam(value = "taskGroupsIds", required = false) List<Long> groupIds) {
        teams.update(input, areaIds, groupIds);
        return "redirect:/teams/editar/" + input.getId();
    }

    @PostMapping("/empleado/actualizar")
    public String actualizarEmpleadoDesdeModal(@RequestParam("empleadoId") Long employeeId,
            @RequestParam("equipoId") Long teamId, @RequestParam("active") boolean active,
            @RequestParam("areaId") Long areaId) {
        teams.updateEmployee(teamId, employeeId, areaId, active);
        return "redirect:/teams/editar/" + teamId;
    }
}