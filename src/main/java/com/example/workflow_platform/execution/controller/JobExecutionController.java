package com.example.workflow_platform.execution.controller;

import com.example.workflow_platform.execution.dto.*;
import com.example.workflow_platform.execution.service.JobExecutionService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/executions")
public class JobExecutionController {

    private final JobExecutionService jobExecutionService;

    public JobExecutionController(
            JobExecutionService jobExecutionService) {
        this.jobExecutionService = jobExecutionService;
    }

    @PostMapping("/{executionId}/jobs/{jobId}/claim")
    public ResponseEntity<JobClaimResponse> claimJob(
            @PathVariable Long executionId,
            @PathVariable Long jobId,
            @Valid @RequestBody ClaimJobRequest request) {

        Long fencingToken = jobExecutionService.claimJob(
                executionId,
                jobId,
                request.workerId()
        );

        return ResponseEntity.ok(
                new JobClaimResponse(
                        executionId,
                        jobId,
                        request.workerId(),
                        "RUNNING",
                        fencingToken
                )
        );
    }

    @PostMapping("/{executionId}/jobs/{jobId}/complete")
    public ResponseEntity<JobCompletionResponse> completeJob(
            @PathVariable Long executionId,
            @PathVariable Long jobId,
            @Valid @RequestBody CompleteJobRequest request) {

        List<Long> newlyReadyJobIds =
                jobExecutionService.completeJob(
                        executionId,
                        jobId,
                        request.workerId(),
                        request.fencingToken()
                );

        return ResponseEntity.ok(
                new JobCompletionResponse(
                        executionId,
                        jobId,
                        "SUCCESS",
                        newlyReadyJobIds
                )
        );
    }

    @PostMapping("/{executionId}/jobs/{jobId}/fail")
    public ResponseEntity<JobFailureResponse> failJob(
            @PathVariable Long executionId,
            @PathVariable Long jobId,
            @Valid @RequestBody FailJobRequest request) {

        JobFailureResponse response =
                jobExecutionService.failJob(
                        executionId,
                        jobId,
                        request.workerId(),
                        request.fencingToken(),
                        request.errorMessage()
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{executionId}/jobs/{jobId}/heartbeat")
    public ResponseEntity<JobHeartbeatResponse> heartbeat(
            @PathVariable Long executionId,
            @PathVariable Long jobId,
            @Valid @RequestBody HeartbeatJobRequest request) {

        JobHeartbeatResponse response =
                jobExecutionService.heartbeatJob(
                        executionId,
                        jobId,
                        request.workerId(),
                        request.fencingToken()
                );

        return ResponseEntity.ok(response);
    }

    public record JobClaimResponse(
            Long executionId,
            Long jobId,
            String workerId,
            String status,
            Long fencingToken
    ) {
    }

    public record JobCompletionResponse(
            Long executionId,
            Long completedJobId,
            String status,
            List<Long> newlyReadyJobIds
    ) {
    }
}
