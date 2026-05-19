package com.taskapp.task_scheduler.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import com.taskapp.task_scheduler.model.Area;
import com.taskapp.task_scheduler.service.AreaService;

@Controller
@RequestMapping("/areas")
@RequiredArgsConstructor
public class AreaController {

    private final AreaService areaService;

    // 1. Mostrar la página con la tabla de áreas y el formulario de creación
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("areas", areaService.getAllAreas());
        model.addAttribute("nuevaArea", new Area()); 
        return "area/lista";  
    }

    // 2. Recibir los datos del formulario HTML y guardar el área nueva
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("nuevaArea") Area area) {
        areaService.createArea(area);
        return "redirect:/areas"; 
    }

    // 3. Eliminar un área
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id) {
        areaService.deleteArea(id);
        return "redirect:/areas";
    }

    // 4. Mostrar el formulario PRE-CARGADO con los datos del área a editar
    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable Long id, Model model) {
        model.addAttribute("area", areaService.getAreaById(id));
        return "area/editar"; // Busca el archivo src/main/resources/templates/area/editar.html
    }

    // 5. Recibir los datos actualizados y guardar los cambios
    @PostMapping("/editar/{id}")
    public String actualizar(@PathVariable Long id, @ModelAttribute("area") Area area) {
        areaService.updateArea(id, area);
        return "redirect:/areas";
    }
}