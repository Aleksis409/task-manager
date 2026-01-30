package com.javarush.taskmanager.model.entity;

import com.javarush.taskmanager.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TaskStatus status;

    private LocalDate deadline;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    public Task(String title, String description, TaskStatus status, LocalDate deadline, User owner) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.deadline = deadline;
        this.owner = owner;
    }

    public void changeStatus(TaskStatus status) {
        this.status = status;
    }

    public void updateDetails(String title, String description, LocalDate deadline) {
        this.title = title;
        this.description = description;
        this.deadline = deadline;
    }
}
