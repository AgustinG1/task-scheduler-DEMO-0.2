package com.taskapp.task_scheduler.security;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import com.taskapp.task_scheduler.model.*;
import com.taskapp.task_scheduler.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OwnerAccess {
    private final AppUserService accounts;
    private final AppUserRepository users;

    public Long id() { return accounts.requireCurrent().id(); }
    public AppUser reference(Long ownerId) { return users.getReferenceById(ownerId); }

    /** Called inside a business transaction. Serializes writes/generation for this owner. */
    public AppUser lock(Long ownerId) {
        AppUser account = require(users.findForUpdate(ownerId));
        if (!account.isActive()) throw new org.springframework.security.access.AccessDeniedException("Account unavailable");
        return account;
    }

    public <T> T require(Optional<T> result) { return result.orElseThrow(OwnerAccess::notFound); }

    public void check(OwnedEntity value, Long ownerId) {
        if (value == null || value.getOwner() == null || !Objects.equals(value.getOwner().getId(), ownerId)) {
            throw notFound();
        }
    }

    public void task(Task task, Long ownerId) {
        check(task, ownerId);
        task.getAuthorizedAreas().forEach(link -> check(link.getArea(), ownerId));
    }

    public void employee(Employee employee, Long ownerId) {
        check(employee, ownerId);
        check(employee.getArea(), ownerId);
    }

    public void catalog(TaskGroup group, Long ownerId) {
        check(group, ownerId);
        group.getTasks().forEach(task -> task(task, ownerId));
    }

    public void team(Team team, Long ownerId) {
        check(team, ownerId);
        team.getAreas().forEach(area -> check(area, ownerId));
        team.getTaskGroups().forEach(group -> catalog(group, ownerId));
    }

    /** Rejects an entire manipulated selection, including a missing id, before any mutation. */
    public <T> List<T> selection(Collection<Long> ids, Function<Collection<Long>, List<T>> query) {
        if (ids == null || ids.isEmpty()) return new java.util.ArrayList<>();
        if (ids.stream().anyMatch(Objects::isNull)) throw notFound();
        var unique = new java.util.LinkedHashSet<>(ids);
        List<T> result = query.apply(unique);
        if (result.size() != unique.size()) throw notFound();
        return new java.util.ArrayList<>(result);
    }

    public static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
