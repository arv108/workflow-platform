package com.example.workflow_platform.job.controller;

import com.example.workflow_platform.job.dto.CreateJobDependencyRequest;
import com.example.workflow_platform.job.dto.JobDependencyResponse;
import com.example.workflow_platform.job.service.JobDependencyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobDependencyController {

    private final JobDependencyService jobDependencyService;

    public JobDependencyController(
            JobDependencyService jobDependencyService) {
        this.jobDependencyService = jobDependencyService;
    }

    @PostMapping("/{jobId}/dependencies")
    @ResponseStatus(HttpStatus.CREATED)
    public JobDependencyResponse addDependency(
            @PathVariable Long jobId,
            @Valid @RequestBody CreateJobDependencyRequest request) {

        return jobDependencyService.addDependency(
                jobId,
                request.dependsOnJobId()
        );
    }
}