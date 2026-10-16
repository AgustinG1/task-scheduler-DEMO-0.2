package com.taskapp.task_scheduler.service;

import java.util.List;
import com.taskapp.task_scheduler.model.Area;
import com.taskapp.task_scheduler.repository.AreaRepository;
import com.taskapp.task_scheduler.security.OwnerAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AreaService {
    private final AreaRepository areaRepository;
    private final OwnerAccess owners;

    public List<Area> getAllAreas() { return areaRepository.findAllByOwner_Id(owners.id()); }

    public Area getAreaById(Long id) {
        return owners.require(areaRepository.findByIdAndOwner_Id(id, owners.id()));
    }

    @Transactional
    public Area createArea(Area input) {
        Long ownerId = owners.id();
        var owner = owners.lock(ownerId);
        Area area = new Area();
        area.setNombre(input.getNombre());
        area.setDescripcion(input.getDescripcion());
        area.setOwner(owner);
        return areaRepository.save(area);
    }

    @Transactional
    public Area updateArea(Long id, Area input) {
        Long ownerId = owners.id();
        owners.lock(ownerId);
        Area area = owners.require(areaRepository.findByIdAndOwner_Id(id, ownerId));
        area.setNombre(input.getNombre());
        area.setDescripcion(input.getDescripcion());
        return areaRepository.save(area);
    }

    @Transactional
    public void deleteArea(Long id) {
        Long ownerId = owners.id();
        owners.lock(ownerId);
        areaRepository.delete(owners.require(areaRepository.findByIdAndOwner_Id(id, ownerId)));
    }
}