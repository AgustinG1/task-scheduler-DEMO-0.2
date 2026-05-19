package com.taskapp.task_scheduler.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.taskapp.task_scheduler.model.Area;
import java.util.Optional;

@Repository
public interface AreaRepository extends JpaRepository<Area, Long> {
    
    // Spring genera: SELECT * FROM areas WHERE nombre = ?
    Optional<Area> findByNombre(String nombre);
    
}