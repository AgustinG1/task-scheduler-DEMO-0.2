package com.taskapp.task_scheduler.controller;

import com.taskapp.task_scheduler.model.TaskGroup;
import com.taskapp.task_scheduler.model.Task;
import com.taskapp.task_scheduler.repository.TaskGroupRepository;
import com.taskapp.task_scheduler.repository.TaskRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/task-groups")
@RequiredArgsConstructor
public class TaskGroupController {

    private final TaskGroupRepository taskGroupRepository;
    private final TaskRepository taskRepository;

    // 1. Mostrar la página principal con las tarjetas de catálogos
    @GetMapping
    public String listarCatalogos(Model model) {
        model.addAttribute("catalogos", taskGroupRepository.findAll());
        model.addAttribute("nuevoCatalogo", new TaskGroup());
        return "taskgroups/task-groups"; 
    }

    // 2. Guardar el nombre y descripción de un catálogo nuevo
    @PostMapping("/guardar")
    public String guardarCatalogo(@ModelAttribute("nuevoCatalogo") TaskGroup taskGroup) {
        taskGroupRepository.save(taskGroup);
        return "redirect:/task-groups";
    }

    // 3. Eliminar un catálogo entero
    @GetMapping("/eliminar/{id}")
        @Transactional
        public String eliminarCatalogo(@PathVariable Long id) {
            TaskGroup catalogo = taskGroupRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("ID inválido: " + id));

    // Limpiar relaciones antes de borrar
    catalogo.getTasks().clear();
    catalogo.getTeams().forEach(team -> team.getTaskGroups().remove(catalogo));
    catalogo.getTeams().clear();
    
    taskGroupRepository.delete(catalogo);
    return "redirect:/task-groups";
}
    // 4. Entrar a editar el catálogo para meterle tareas
    @GetMapping("/editar/{id}")
    public String editarCatalogo(@PathVariable Long id, Model model) {
        TaskGroup catalogo = taskGroupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID de catálogo inválido:" + id));
        
        model.addAttribute("catalogo", catalogo);
        model.addAttribute("todasLasTareas", taskRepository.findAll());
        return "taskgroups/task-group-edit";
    }

    // 5. Guardar las tareas seleccionadas dentro del catálogo
    @PostMapping("/actualizar")
    public String actualizarCatalogo(@ModelAttribute("catalogo") TaskGroup catalogoActualizado,
                                     @RequestParam(value = "tasksIds", required = false) List<Long> tasksIds) {
        TaskGroup catalogoOriginal = taskGroupRepository.findById(catalogoActualizado.getId())
                .orElseThrow(() -> new IllegalArgumentException("ID inválido:" + catalogoActualizado.getId()));
        
        catalogoOriginal.setName(catalogoActualizado.getName());
        catalogoOriginal.setDescription(catalogoActualizado.getDescription());
        
        catalogoOriginal.getTasks().clear();
        if (tasksIds != null && !tasksIds.isEmpty()) {
            List<Task> tareasSeleccionadas = taskRepository.findAllById(tasksIds);
            catalogoOriginal.setTasks(tareasSeleccionadas);
        }
        
        taskGroupRepository.save(catalogoOriginal);
        return "redirect:/task-groups/editar/" + catalogoOriginal.getId();
    }
}