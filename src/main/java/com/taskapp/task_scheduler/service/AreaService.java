package com.taskapp.task_scheduler.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;

import com.taskapp.task_scheduler.model.Area;
import com.taskapp.task_scheduler.repository.AreaRepository;

@Service
@RequiredArgsConstructor // Magia de Lombok: crea el constructor para inyectar el repositorio
public class AreaService {

    // Traemos a nuestro "recepcionista" de la base de datos de áreas
    private final AreaRepository areaRepository;
     

    // 1. Traer todas las áreas
    public List<Area> getAllAreas() {
        return areaRepository.findAll();
    }

    // 2. Buscar por ID (o lanzar error si no existe)
    public Area getAreaById(Long id) {
        // findById devuelve un Optional. Con orElseThrow decimos: "dámelo, o explota con este mensaje"
        return areaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: No se encontró el área con ID " + id));
    }

    // 3. Crear una nueva área
    public Area createArea(Area area) {
        // Podemos agregar reglas aquí en el futuro (ej. que no se repita el nombre)
        return areaRepository.save(area); 
    }

    // 4. Actualizar un área existente
    public Area updateArea(Long id, Area areaActualizada) {
        // Paso A: Buscamos el área original usando el método que ya creamos arriba
        Area areaExistente = getAreaById(id);
        
        // Paso B: Le cambiamos los datos por los nuevos
        areaExistente.setNombre(areaActualizada.getNombre());
        areaExistente.setDescripcion(areaActualizada.getDescripcion());
        
        // Paso C: Guardamos los cambios
        return areaRepository.save(areaExistente);
    }

    // 5. Eliminar un área (Físicamente)
    public void deleteArea(Long id) {
        // Primero verificamos que exista
        Area area = getAreaById(id);
        areaRepository.delete(area);
    }
}