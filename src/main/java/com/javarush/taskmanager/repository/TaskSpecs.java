package com.javarush.taskmanager.repository;

import com.javarush.taskmanager.enums.TaskStatus;
import com.javarush.taskmanager.model.entity.Task;
import com.javarush.taskmanager.model.entity.User;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class TaskSpecs {

    private TaskSpecs() {
    }

    public static Specification<Task> byOwner(User owner) {
        return (root, query, cb) ->
                cb.equal(root.get("owner"), owner);
    }

    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }

    public static Specification<Task> deadlineFrom(LocalDate from) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("deadline"), from);
    }

    public static Specification<Task> deadlineTo(LocalDate to) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("deadline"), to);
    }
}
