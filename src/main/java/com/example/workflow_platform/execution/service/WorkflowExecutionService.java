package com.example.workflow_platform.execution.service;

import com.example.workflow_platform.execution.dto.WorkflowExecutionResponseDto;
import com.example.workflow_platform.execution.entity.JobExecution;
import com.example.workflow_platform.execution.entity.WorkflowExecution;
import com.example.workflow_platform.execution.enums.ExecutionStatus;
import com.example.workflow_platform.execution.enums.JobExecutionStatus;
import com.example.workflow_platform.execution.repository.JobExecutionRepository;
import com.example.workflow_platform.execution.repository.WorkflowExecutionRepository;
import com.example.workflow_platform.job.entity.Job;
import com.example.workflow_platform.job.repository.JobDependencyRepository;
import com.example.workflow_platform.job.repository.JobRepository;
import com.example.workflow_platform.workflow.entity.Workflow;
import com.example.workflow_platform.workflow.enums.WorkflowStatus;
import com.example.workflow_platform.workflow.repository.WorkflowRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class WorkflowExecutionService {

    private final WorkflowRepository workflowRepository;
    private final JobRepository jobRepository;
    private final JobDependencyRepository dependencyRepository;
    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final JobExecutionRepository jobExecutionRepository;

    public WorkflowExecutionService(
            WorkflowRepository workflowRepository,
            JobRepository jobRepository,
            JobDependencyRepository dependencyRepository,
            WorkflowExecutionRepository workflowExecutionRepository,
            JobExecutionRepository jobExecutionRepository) {

        this.workflowRepository = workflowRepository;
        this.jobRepository = jobRepository;
        this.dependencyRepository = dependencyRepository;
        this.workflowExecutionRepository = workflowExecutionRepository;
        this.jobExecutionRepository = jobExecutionRepository;
    }

    @Transactional
    public WorkflowExecutionResponseDto startExecution(Long workflowId) {

        // 1. Find the workflow.
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Workflow not found: " + workflowId
                ));

        // 2. Only active workflows can run.
        if (workflow.getStatus() != WorkflowStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot execute an inactive workflow"
            );
        }

        // 3. Find all jobs belonging to this workflow.
        List<Job> jobs = jobRepository.findByWorkflow_Id(workflowId);

        if (jobs.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot execute a workflow with no jobs"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        // 4. Create the workflow execution.
        WorkflowExecution execution = new WorkflowExecution();
        execution.setWorkflow(workflow);
        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setStartedAt(now);
        execution.setCreatedAt(now);

        WorkflowExecution savedExecution =
                workflowExecutionRepository.save(execution);

        // 5. Initialize the execution state for each job.
        List<JobExecution> jobExecutions = new ArrayList<>();

        for (Job job : jobs) {

            boolean hasDependencies =
                    !dependencyRepository.findByJob_Id(job.getId()).isEmpty();

            JobExecution jobExecution = new JobExecution();
            jobExecution.setJob(job);
            jobExecution.setWorkflowExecution(savedExecution);
            jobExecution.setAttempt(0);
            jobExecution.setStatus(
                    hasDependencies
                            ? JobExecutionStatus.BLOCKED
                            : JobExecutionStatus.READY
            );
            jobExecution.setCreatedAt(now);
            jobExecution.setUpdatedAt(now);

            jobExecutions.add(jobExecution);
        }

        jobExecutionRepository.saveAll(jobExecutions);

        return new WorkflowExecutionResponseDto(
                savedExecution.getId(),
                workflow.getId(),
                savedExecution.getStatus().name()
        );
    }
}