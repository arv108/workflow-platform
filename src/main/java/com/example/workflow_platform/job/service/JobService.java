package com.example.workflow_platform.job.service;

import com.example.workflow_platform.job.dto.JobRequestDto;
import com.example.workflow_platform.job.dto.JobResponseDto;
import com.example.workflow_platform.job.entity.Job;
import com.example.workflow_platform.job.repository.JobRepository;
import com.example.workflow_platform.workflow.entity.Workflow;
import com.example.workflow_platform.workflow.repository.WorkflowRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final WorkflowRepository workflowRepository;

    public JobService(
            JobRepository jobRepository,
            WorkflowRepository workflowRepository) {
        this.jobRepository = jobRepository;
        this.workflowRepository = workflowRepository;
    }

    @Transactional
    public JobResponseDto createJob(
            Long workflowId,
            JobRequestDto request) {

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Workflow not found: " + workflowId
                ));

        Job job = new Job();
        job.setWorkflow(workflow);
        job.setName(request.name());
        job.setJobType(request.type());
        job.setMaxRetries(request.maxRetries());
        job.setTimeoutSeconds(request.timeoutSeconds());

        Job saved = jobRepository.save(job);

        return new JobResponseDto(
                saved.getId(),
                saved.getWorkflow().getId(),
                saved.getName(),
                saved.getJobType(),
                saved.getMaxRetries(),
                saved.getTimeoutSeconds()
        );
    }
}