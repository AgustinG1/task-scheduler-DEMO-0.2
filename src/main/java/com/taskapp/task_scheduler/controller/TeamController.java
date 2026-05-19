package com.taskapp.task_scheduler.controller;

import com.taskapp.task_scheduler.model.Team;
import com.taskapp.task_scheduler.model.Area;
import com.taskapp.task_scheduler.model.Employee;
import com.taskapp.task_scheduler.model.TaskGroup; // <-- IMPORTANTE: Importamos el nuevo modelo de Catálogos
import com.taskapp.task_scheduler.repository.TeamRepository;
import com.taskapp.task_scheduler.repository.AreaRepository;
import com.taskapp.task_scheduler.repository.EmployeeRepository;
import com.taskapp.task_scheduler.repository.TaskGroupRepository; // <-- IMPORTANTE: Usamos el repo de Catálogos
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamRepository teamRepository;
    private final AreaRepository areaRepository;
    private final EmployeeRepository employeeRepository; 
    private final TaskGroupRepository taskGroupRepository; // <-- Reemplazamos taskRepository por taskGroupRepository

    // 1. Mostrar la página con la lista de equipos y el formulario
    @GetMapping
    public String listarEquipos(Model model) {
        model.addAttribute("equipos", teamRepository.findAll());
        model.addAttribute("nuevoEquipo", new Team());
        model.addAttribute("todasLasAreas", areaRepository.findAll()); 
        return "teams/teams"; 
    }

    // 2. Guardar un nuevo equipo desde el formulario
    @PostMapping("/guardar")
    public String guardarEquipo(@ModelAttribute("nuevoEquipo") Team team,
                                @RequestParam(value = "areasIds", required = false) List<Long> areasIds) {
        // Si el usuario seleccionó áreas, las buscamos por ID y las asociamos al equipo
        if (areasIds != null && !areasIds.isEmpty()) {
            List<Area> areasSeleccionadas = areaRepository.findAllById(areasIds);
            team.setAreas(areasSeleccionadas);
        }
        teamRepository.save(team);
        return "redirect:/teams"; 
    }

    // 3. Eliminar un equipo
    @GetMapping("/eliminar/{id}")
    public String eliminarEquipo(@PathVariable Long id) {
        teamRepository.deleteById(id);
        return "redirect:/teams";
    }
    
    // 4. Editar un equipo (Carga los datos para el Panel de Control)
    @GetMapping("/editar/{id}")
    public String editarEquipo(@PathVariable Long id, Model model) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID de equipo inválido:" + id));
        
        // Filtramos los empleados dinámicamente:
        // Solo incluimos en la lista a aquellos cuyo área coincida con las áreas operativas del equipo.
        List<Employee> empleadosFiltrados = employeeRepository.findAll().stream()
                .filter(emp -> team.getAreas() != null && team.getAreas().contains(emp.getArea()))
                .toList();
        
        model.addAttribute("equipo", team);
        model.addAttribute("todasLasAreas", areaRepository.findAll());
        model.addAttribute("todosLosEmpleados", empleadosFiltrados); 
        // NUEVO: Enviamos todos los catálogos disponibles para que el usuario pueda seleccionarlos
        model.addAttribute("todosLosCatalogos", taskGroupRepository.findAll()); 
        
        return "teams/team-edit";  
    }

    // 5. Guardar los cambios generales del Equipo (Nombre, Descripción, Áreas y CATÁLOGOS operativas)
    @PostMapping("/actualizar")
    public String actualizarEquipo(@ModelAttribute("equipo") Team teamActualizado,
                                   @RequestParam(value = "areasIds", required = false) List<Long> areasIds,
                                   @RequestParam(value = "taskGroupsIds", required = false) List<Long> taskGroupsIds) { // <-- RECIBIMOS LOS IDs DE LOS CATÁLOGOS
        
        // Obtenemos el equipo original de la base de datos para no perder sus empleados
        Team teamOriginal = teamRepository.findById(teamActualizado.getId())
                .orElseThrow(() -> new IllegalArgumentException("ID de equipo inválido:" + teamActualizado.getId()));
        
        teamOriginal.setName(teamActualizado.getName());
        teamOriginal.setDescription(teamActualizado.getDescription());
        
        // Limpiamos las áreas anteriores y cargamos las nuevas seleccionadas
        teamOriginal.getAreas().clear();
        if (areasIds != null && !areasIds.isEmpty()) {
            List<Area> areasSeleccionadas = areaRepository.findAllById(areasIds);
            teamOriginal.setAreas(areasSeleccionadas);
        }
        
        // NUEVO: Limpiamos los catálogos anteriores y asignamos los nuevos seleccionados
        teamOriginal.getTaskGroups().clear();
        if (taskGroupsIds != null && !taskGroupsIds.isEmpty()) {
            List<TaskGroup> catalogosSeleccionados = taskGroupRepository.findAllById(taskGroupsIds);
            teamOriginal.setTaskGroups(catalogosSeleccionados);
        }
        
        teamRepository.save(teamOriginal);
        
        // Redirige de vuelta a la misma pantalla de edición
        return "redirect:/teams/editar/" + teamOriginal.getId(); 
    }

    // 6. Procesar los datos que vienen de la ventana emergente (Modal) del empleado
    @PostMapping("/empleado/actualizar")
    public String actualizarEmpleadoDesdeModal(
            @RequestParam("empleadoId") Long empleadoId,
            @RequestParam("equipoId") Long equipoId, // Para saber a qué equipo volver
            @RequestParam("active") boolean active,
            @RequestParam("areaId") Long areaId) {
        
        var empleado = employeeRepository.findById(empleadoId)
                .orElseThrow(() -> new IllegalArgumentException("ID de empleado inválido:" + empleadoId));
        var areaNueva = areaRepository.findById(areaId)
                .orElseThrow(() -> new IllegalArgumentException("ID de área inválido:" + areaId));
        
        empleado.setActive(active);
        empleado.setArea(areaNueva);
        
        employeeRepository.save(empleado);
        return "redirect:/teams/editar/" + equipoId;
    }
}