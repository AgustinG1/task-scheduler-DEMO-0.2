package com.taskapp.task_scheduler.model;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class OwnedEntity {
    // Nullable only while the historical records await their explicit migration.
    // Application writes always assign an owner; null-owned records are never queried by users.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", updatable = false)
    private AppUser owner;
}
