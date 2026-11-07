package com.example.workflow_platform.job.service;

import com.example.workflow_platform.job.dto.JobDependencyResponse;
import com.example.workflow_platform.job.entity.Job;
import com.example.workflow_platform.job.entity.JobDependency;
import com.example.workflow_platform.job.repository.JobDependencyRepository;
import com.example.workflow_platform.job.repository.JobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Service
public class JobDependencyService {

    private final JobRepository jobRepository;
    private final JobDependencyRepository dependencyRepository;

    public JobDependencyService(
            JobRepository jobRepository,
            JobDependencyRepository dependencyRepository
    ) {
        this.jobRepository = jobRepository;
        this.dependencyRepository = dependencyRepository;
    }

    @Transactional
    public JobDependencyResponse addDependency(
            Long jobId,
            Long dependsOnJobId
    ) {
        if (Objects.equals(jobId, dependsOnJobId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A job cannot depend on itself"
            );
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job not found: " + jobId
                ));

        Job prerequisite = jobRepository.findById(dependsOnJobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Prerequisite job not found: " + dependsOnJobId
                ));

        if (!Objects.equals(
                workflowIdOf(job),
                workflowIdOf(prerequisite)
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Jobs must belong to the same workflow"
            );
        }

        if (dependencyRepository.existsByJobIdAndDependsOnJobId(
                jobId, dependsOnJobId
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This dependency already exists"
            );
        }

        if (wouldCreateCycle(jobId, dependsOnJobId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This dependency would create a cycle"
            );
        }

        JobDependency dependency = new JobDependency();
        dependency.setJob(job);
        dependency.setDependsOnJob(prerequisite);

        dependencyRepository.save(dependency);

        return new JobDependencyResponse(
                job.getId(),
                prerequisite.getId()
        );
    }

    private Long workflowIdOf(Job job) {
        return job == null || job.getWorkflow() == null ? null : job.getWorkflow().getId();
    }

    private boolean wouldCreateCycle(
            Long jobId,
            Long dependsOnJobId
    ) {
        Set<Long> visited = new HashSet<>();
        Deque<Long> stack = new ArrayDeque<>();

        stack.push(dependsOnJobId);

        while (!stack.isEmpty()) {
            Long currentJobId = stack.pop();

            if (Objects.equals(currentJobId, jobId)) {
                return true;
            }

            if (!visited.add(currentJobId)) {
                continue;
            }

            for (JobDependency dependency :
                    dependencyRepository.findByJob_Id(currentJobId)) {

                stack.push(dependency.getDependsOnJob().getId());
            }
        }

        return false;
    }
}
