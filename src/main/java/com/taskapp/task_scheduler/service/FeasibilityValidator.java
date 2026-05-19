package com.taskapp.task_scheduler.service;

import org.springframework.stereotype.Service;
import com.taskapp.task_scheduler.model.Employee;
import com.taskapp.task_scheduler.model.Task;
import java.util.List;

@Service
public class FeasibilityValidator {

    // Este método lanzará un error si descubre que es imposible generar la planilla
    public void validar(List<Employee> empleadosActivos, List<Task> tareasDisponibles) {
        
        // 1. Validar que tengamos gente trabajando
        if (empleadosActivos == null || empleadosActivos.isEmpty()) {
            throw new IllegalStateException("Error: No hay empleados activos para generar la planilla.");
        }

        // 2. Validar que tengamos tareas para asignar
        if (tareasDisponibles == null || tareasDisponibles.isEmpty()) {
            throw new IllegalStateException("Error: No hay tareas registradas en el sistema.");
        }

        // (Aquí luego agregaremos más reglas, como revisar que los de 'Tortas' tengan tareas de 'Tortas')
        System.out.println("Validación inicial superada: Hay " + empleadosActivos.size() + " empleados y " + tareasDisponibles.size() + " tareas.");
    }
}