/* 
package com.taskapp.task_scheduler.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.List;
import jakarta.persistence.EntityManager;

import com.taskapp.task_scheduler.model.*;
import com.taskapp.task_scheduler.repository.*;
import com.taskapp.task_scheduler.service.AssignmentAlgorithm;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AreaRepository areaRepository;
    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final AssignmentAlgorithm assignmentAlgorithm;
    private final AssignmentRepository assignmentRepository;
    private final EntityManager entityManager;
    private final TaskAreaRepository taskAreaRepository;

    @Override
    @Transactional // Garantiza que todo se ejecuta en una sola transacción
    public void run(String... args) throws Exception {
        
        // Evitamos que se dupliquen datos si la base de datos ya tiene registros
        if (employeeRepository.count() > 0) {
            return;
        }

        System.out.println("=========================================");
        System.out.println("CARGANDO DATOS DE PRUEBA...");

        // 1. Crear Área
        Area area1 = new Area();
        area1.setNombre("Producción");
        area1.setDescripcion("Área principal");
        areaRepository.save(area1);

        // 2. Crear Empleados (4 empleados, más que las 3 tareas)
        Employee emp1 = new Employee();
        emp1.setName("Carlos");
        emp1.setApellido("Gómez");
        emp1.setArea(area1);
        emp1.setActive(true);
        employeeRepository.save(emp1);

        Employee emp2 = new Employee();
        emp2.setName("Ana");
        emp2.setApellido("López");
        emp2.setArea(area1);
        emp2.setActive(true);
        employeeRepository.save(emp2);

        Employee emp3 = new Employee();
        emp3.setName("Pedro");
        emp3.setApellido("Martínez");
        emp3.setArea(area1);
        emp3.setActive(true);
        employeeRepository.save(emp3);

        Employee emp4 = new Employee();
        emp4.setName("María");
        emp4.setApellido("García");
        emp4.setArea(area1);
        emp4.setActive(true);
        employeeRepository.save(emp4);
        
        // 3. Crear Tareas (Generales para probar la rotación)
        Task t1 = new Task();
        t1.setName("Limpieza General");
        t1.setType(TaskType.GENERAL);
        taskRepository.save(t1);

        Task t2 = new Task();
        t2.setName("Revisión de Inventario");
        t2.setType(TaskType.GENERAL);
        taskRepository.save(t2);

        Task t3 = new Task();
        t3.setName("Preparación de Insumos");
        t3.setType(TaskType.GENERAL);
        taskRepository.save(t3);

        // Área específica
        Area area2 = new Area();
        area2.setNombre("Tortas");
        area2.setDescripcion("Área de tortas");
        areaRepository.save(area2);

        // Empleado del área Tortas
        Employee emp5 = new Employee();
        emp5.setName("Laura");
        emp5.setApellido("Pérez");
        emp5.setArea(area2);
        emp5.setActive(true);
        employeeRepository.save(emp5);

        // Tarea específica solo para Tortas
        Task t4 = new Task();
        t4.setName("Decorado de Tortas");
        t4.setType(TaskType.SPECIFIC);
        taskRepository.save(t4);

        // Vincular tarea al área
        TaskArea taskArea = new TaskArea();
        taskArea.setId(new TaskAreaId(t4.getId(), area2.getId()));
        taskArea.setTask(t4);
        taskArea.setArea(area2);

        // --- SOLUCIÓN APLICADA AQUÍ ---
        // 1. Guardamos el TaskArea en la base de datos
        taskAreaRepository.save(taskArea);

        // 2. Forzamos la sincronización y limpiamos la caché de Hibernate.
        // Así, cuando generatePayroll() llame a taskRepository.findAll(), 
        // traerá t4 con su lista de áreas autorizadas llena.
        entityManager.flush();
        entityManager.clear();
        // ------------------------------

        // 4. Ejecutar el Algoritmo
        System.out.println("EJECUTANDO ALGORITMO DE ASIGNACIÓN...");
        assignmentAlgorithm.generatePayroll();
        
        // 5. Imprimir resultados en consola
        System.out.println("=========================================");
        System.out.println("RESULTADOS DE LAS 16 SEMANAS:");
        
        List<Assignment> asignaciones = assignmentRepository.findAll();
        
        for (int i = 1; i <= 16; i++) {
            final int semana = i;
            System.out.println("--- SEMANA " + semana + " ---");
            
            asignaciones.stream()
                .filter(a -> a.getWeekNumber() == semana)
                .forEach(a -> {
                    String nombreTarea = (a.getTask() != null) ? a.getTask().getName() : "DESCANSO";
                    System.out.println(a.getEmployee().getName() + " -> " + nombreTarea);
                });
        }
        System.out.println("=========================================");
    }
}
*/