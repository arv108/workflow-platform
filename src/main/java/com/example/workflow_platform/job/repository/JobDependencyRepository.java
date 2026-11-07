package com.example.workflow_platform.job.repository;

import com.example.workflow_platform.job.entity.JobDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobDependencyRepository
        extends JpaRepository<JobDependency, Long> {

    boolean existsByJobIdAndDependsOnJobId(
            Long jobId,
            Long dependsOnJobId
    );

    List<JobDependency> findByJob_Id(Long jobId);
    List<JobDependency> findByDependsOnJob_Id(Long dependsOnJobId);
}