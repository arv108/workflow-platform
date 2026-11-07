package com.example.workflow_platform.job.controller;

import com.example.workflow_platform.job.dto.JobRequestDto;
import com.example.workflow_platform.job.dto.JobResponseDto;
import com.example.workflow_platform.job.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/workflows/{workflowId}/jobs")
    public ResponseEntity<JobResponseDto> createJobs(
            @PathVariable Long workflowId,
            @Valid @RequestBody JobRequestDto request) {

        JobResponseDto response =
                jobService.createJob(workflowId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}