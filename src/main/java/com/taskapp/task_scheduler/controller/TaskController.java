package com.taskapp.task_scheduler.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import com.taskapp.task_scheduler.model.Task;
import com.taskapp.task_scheduler.model.TaskType;
import com.taskapp.task_scheduler.service.AreaService;
import com.taskapp.task_scheduler.service.TaskService;

@Controller
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final AreaService areaService;

    // 1. Mostrar la página con la lista de tareas y el formulario de creación
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("tasks", taskService.getAllTasks());
        model.addAttribute("nuevaTarea", new Task());
        model.addAttribute("tipos", TaskType.values()); // Enum para el dropdown
        model.addAttribute("areas", areaService.getAllAreas()); // Para el dropdown de área en tareas SPECIFIC
        return "task/lista";
    }

    // 2. Guardar una nueva tarea
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("nuevaTarea") Task task,
                          @RequestParam(required = false) Long areaId) {
        
        Task savedTask = taskService.createTask(task);
        
        // Si es SPECIFIC y se seleccionó un área válida, crear el vínculo
        if (task.getType() == TaskType.SPECIFIC && areaId != null) {
            taskService.vincularArea(savedTask, areaId);
        }
        
        return "redirect:/tasks";
    }

    // 3. Mostrar el formulario pre-cargado para editar
    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable Long id, Model model) {
        model.addAttribute("task", taskService.getTaskById(id));
        model.addAttribute("tipos", TaskType.values());
        return "task/editar";
    }

    // 4. Actualizar la tarea existente
    @PostMapping("/editar/{id}")
    public String actualizar(@PathVariable Long id, @ModelAttribute("task") Task task) {
        taskService.updateTask(id, task);
        return "redirect:/tasks";
    }

    // 5. Eliminar la tarea permanentemente
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id) {
        taskService.deleteTask(id);
        return "redirect:/tasks";
    }
}