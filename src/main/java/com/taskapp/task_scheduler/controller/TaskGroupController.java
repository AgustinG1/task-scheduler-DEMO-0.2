package com.taskapp.task_scheduler.controller;

import java.util.List;
import com.taskapp.task_scheduler.model.TaskGroup;
import com.taskapp.task_scheduler.service.CatalogService;
import com.taskapp.task_scheduler.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/task-groups")
@RequiredArgsConstructor
public class TaskGroupController {
    private final CatalogService catalogs;
    private final TaskService tasks;

    @InitBinder
    void fields(WebDataBinder binder) { binder.setAllowedFields("id", "name", "description"); }

    @GetMapping
    public String listarCatalogos(Model model) {
        model.addAttribute("catalogos", catalogs.list());
        model.addAttribute("nuevoCatalogo", new TaskGroup());
        return "taskgroups/task-groups";
    }

    @PostMapping("/guardar")
    public String guardarCatalogo(@ModelAttribute("nuevoCatalogo") TaskGroup input) {
        catalogs.create(input);
        return "redirect:/task-groups";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarCatalogo(@PathVariable Long id) {
        catalogs.delete(id);
        return "redirect:/task-groups";
    }

    @GetMapping("/editar/{id}")
    public String editarCatalogo(@PathVariable Long id, Model model) {
        model.addAttribute("catalogo", catalogs.get(id));
        model.addAttribute("todasLasTareas", tasks.getAllTasks());
        return "taskgroups/task-group-edit";
    }

    @PostMapping("/actualizar")
    public String actualizarCatalogo(@ModelAttribute("catalogo") TaskGroup input,
            @RequestParam(value = "tasksIds", required = false) List<Long> ids) {
        catalogs.update(input, ids);
        return "redirect:/task-groups/editar/" + input.getId();
    }
}