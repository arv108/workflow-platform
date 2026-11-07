package com.example.workflow_platform.job.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "job_dependency",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_job_dependency",
                        columnNames = {"job_id", "depends_on_job_id"}
                )
        }
)
@Getter
@Setter
public class JobDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "job_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_dependency_job")
    )
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "depends_on_job_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_dependency_parent")
    )
    private Job dependsOnJob;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public Job getDependsOnJob() {
        return dependsOnJob;
    }

    public void setDependsOnJob(Job dependsOnJob) {
        this.dependsOnJob = dependsOnJob;
    }
}