package com.taskapp.task_scheduler.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import com.taskapp.task_scheduler.model.Employee;
import com.taskapp.task_scheduler.model.TaskType;
import com.taskapp.task_scheduler.model.Area;
import com.taskapp.task_scheduler.service.EmployeeService;
import com.taskapp.task_scheduler.service.AreaService;

@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final AreaService areaService;

    // 1. Listar empleados y preparar formulario de creación
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("employees", employeeService.getAllEmployees());
        model.addAttribute("areas", areaService.getAllAreas());
        
        // 🔧 CORRECCIÓN: Inicializar area para evitar null en el binding del formulario
        Employee nuevo = new Employee();
        nuevo.setArea(new Area());  // Area vacía (id = null)
        model.addAttribute("nuevoEmployee", nuevo);
        
        return "employee/lista";
    }

    // 2. Guardar un nuevo empleado
// 2. Guardar un nuevo empleado (con @RequestParam)
@PostMapping("/guardar")
public String guardar(@RequestParam("nombre") String nombre,
                      @RequestParam(value = "area.id", required = false) Long areaId) {
    Employee employee = new Employee();
    employee.setName(nombre);
    
    if (areaId != null) {
        Area area = areaService.getAreaById(areaId);
        employee.setArea(area);
    } else {
        employee.setArea(null);
    }
    
    employeeService.createEmployee(employee);
    return "redirect:/employees";
}

    // 3. Mostrar el formulario pre-cargado para la edición (GET)
// 3. Mostrar formulario de edición (GET)
    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable Long id, Model model) {
        Employee emp = employeeService.getEmployeeById(id);
        // Si por alguna razón el área es null (no debería), evita error
        if (emp.getArea() == null) {
            emp.setArea(new Area()); // o maneja el error apropiadamente
        }
        
        model.addAttribute("employee", emp);
        model.addAttribute("areas", areaService.getAllAreas());
        return "employee/editar";
    }
    // 4. Actualizar los datos del empleado (con @RequestParam)
@PostMapping("/editar/{id}")
public String actualizar(@PathVariable Long id,
                         @RequestParam("nombre") String nombre,
                         @RequestParam(value = "apellido", required = false) String apellido,
                         @RequestParam(value = "area.id", required = false) Long areaId) {
    Employee employee = employeeService.getEmployeeById(id);
    employee.setName(nombre);
    employee.setApellido(apellido); // si tu entidad tiene apellido
    
    if (areaId != null) {
        Area area = areaService.getAreaById(areaId);
        employee.setArea(area);
    } else {
        employee.setArea(null);
    }
    
    employeeService.updateEmployee(id, employee);
    return "redirect:/employees";
}

    // 5. Borrado lógico
    @GetMapping("/desactivar/{id}")
    public String desactivar(@PathVariable Long id) {
        employeeService.deactivateEmployee(id);
        return "redirect:/employees";
    }

    // 6. Borrado físico
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return "redirect:/employees";
    }
}