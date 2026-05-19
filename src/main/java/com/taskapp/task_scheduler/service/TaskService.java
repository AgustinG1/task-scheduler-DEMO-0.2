package com.taskapp.task_scheduler.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.List;

import com.taskapp.task_scheduler.model.Area;
import com.taskapp.task_scheduler.model.Assignment;
import com.taskapp.task_scheduler.model.AssignmentStatus;
import com.taskapp.task_scheduler.model.Task;
import com.taskapp.task_scheduler.model.TaskArea;
import com.taskapp.task_scheduler.model.TaskAreaId;
import com.taskapp.task_scheduler.model.TaskType;
import com.taskapp.task_scheduler.repository.AreaRepository;
import com.taskapp.task_scheduler.repository.AssignmentRepository;
import com.taskapp.task_scheduler.repository.TaskAreaRepository;
import com.taskapp.task_scheduler.repository.TaskRepository;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final AreaRepository areaRepository;
    private final TaskAreaRepository taskAreaRepository;    
    private final AssignmentRepository assignmentRepository;

    // 1. Traer todas las tareas
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    // 2. Buscar por ID (lanza excepción si no existe)
    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: No se encontró la tarea con ID " + id));
    }

    // 3. Filtrar tareas por tipo (GENERAL o SPECIFIC) usando el método que creaste
    public List<Task> getTasksByType(TaskType type) {
        return taskRepository.findByType(type);
    }

    // 4. Crear una nueva tarea
    public Task createTask(Task task) {
        return taskRepository.save(task);
    }

    // 5. Actualizar los datos de una tarea existente
    public Task updateTask(Long id, Task tareaActualizada) {
        Task tareaExistente = getTaskById(id);
        
        tareaExistente.setName(tareaActualizada.getName());
        tareaExistente.setDescription(tareaActualizada.getDescription());
        tareaExistente.setType(tareaActualizada.getType());
        
        return taskRepository.save(tareaExistente);
    }

    // 6. Eliminar una tarea por ID (CORREGIDO PARA EVITAR ERROR 500)
    @Transactional
    public void deleteTask(Long id) {
        Task tarea = getTaskById(id);
        
        // A. PASO CLAVE: Borramos los vínculos de áreas en la base de datos primero
        taskAreaRepository.deleteByTaskId(id);
        
        // B. Protegemos el historial: los turnos que tenían esta tarea pasan a ser DESCANSO
        List<Assignment> asignaciones = assignmentRepository.findByTaskId(id);
        for (Assignment asignacion : asignaciones) {
            asignacion.setTask(null);
            asignacion.setStatus(AssignmentStatus.REST);
            assignmentRepository.save(asignacion);
        }
        
        // C. Ahora sí borramos la tarea
        taskRepository.delete(tarea);
    }

    // 7. Vincular un área específica a una tarea
    public void vincularArea(Task task, Long areaId) {
        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new RuntimeException("Área no encontrada"));
        
        TaskArea taskArea = new TaskArea();
        taskArea.setId(new TaskAreaId(task.getId(), area.getId()));
        taskArea.setTask(task);
        taskArea.setArea(area);
        taskAreaRepository.save(taskArea);
    }
}