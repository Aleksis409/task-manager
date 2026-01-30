package com.javarush.taskmanager.repository;

import com.javarush.taskmanager.enums.TaskStatus;
import com.javarush.taskmanager.model.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.javarush.taskmanager.model.entity.User;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long>,
        JpaSpecificationExecutor<Task> {

    /**
     * Returns all tasks belonging to a specific user.
     */
    List<Task> findAllByOwner(User owner);

    /**
     * Counts all tasks belonging to a specific user.
     */
    long countByOwner(User owner);

    /**
     * Counts completed tasks for a specific user.
     */
    long countByOwnerAndStatus(User owner, TaskStatus status);

    /**
     * Finds a task by id and owner.
     *
     * @param id    task identifier
     * @param owner task owner
     * @return Optional task if found
     */
    Optional<Task> findByIdAndOwner(Long id, User owner);

    /**
     * Filters tasks by optional parameters:
     * - owner (required)
     * - status (optional)
     * - deadline range (optional)
     */
    @Query("""
            SELECT t FROM Task t
            WHERE t.owner = :owner
              AND (:status IS NULL OR t.status = :status)
              AND (:from IS NULL OR t.deadline >= :from)
              AND (:to IS NULL OR t.deadline <= :to)
            """)
    List<Task> filterTasks(
            @Param("owner") User owner,
            @Param("status") TaskStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    void deleteAllByOwner(User owner);
}