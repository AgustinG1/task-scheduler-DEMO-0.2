package com.taskapp.task_scheduler.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import com.taskapp.task_scheduler.model.OwnedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface OwnedRepository<T extends OwnedEntity> extends JpaRepository<T, Long> {
    List<T> findAllByOwner_Id(Long ownerId);
    Optional<T> findByIdAndOwner_Id(Long id, Long ownerId);
    List<T> findAllByOwner_IdAndIdIn(Long ownerId, Collection<Long> ids);
    long countByOwner_Id(Long ownerId);
}
