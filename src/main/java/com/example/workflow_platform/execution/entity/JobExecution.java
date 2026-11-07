package com.example.workflow_platform.execution.entity;

import com.example.workflow_platform.execution.enums.JobExecutionStatus;
import com.example.workflow_platform.job.entity.Job;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_execution")
@Getter
@Setter
@NoArgsConstructor
public class JobExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobExecutionStatus status;

    // Job definition being executed
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "job_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_job_execution_job")
    )
    private Job job;

    // Workflow run to which this execution belongs
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "workflow_execution_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_job_execution_workflow_execution"
            )
    )
    private WorkflowExecution workflowExecution;

    @Column(name = "attempt", nullable = false)
    private int attempt = 0;

    // Worker currently responsible for executing the job
    @Column(name = "worker_id")
    private String workerId;

    // Time until the worker's lease remains valid
    @Column(name = "lease_expiry")
    private LocalDateTime leaseExpiry;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "fencing_token", nullable = false)
    private Long fencingToken = 0L;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}