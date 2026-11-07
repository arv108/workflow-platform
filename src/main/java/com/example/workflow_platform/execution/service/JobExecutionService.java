package com.example.workflow_platform.execution.service;

import com.example.workflow_platform.execution.dto.JobFailureResponse;
import com.example.workflow_platform.execution.dto.JobHeartbeatResponse;
import com.example.workflow_platform.execution.entity.JobExecution;
import com.example.workflow_platform.execution.entity.WorkflowExecution;
import com.example.workflow_platform.execution.enums.ExecutionStatus;
import com.example.workflow_platform.execution.enums.JobExecutionStatus;
import com.example.workflow_platform.execution.repository.JobExecutionRepository;
import com.example.workflow_platform.execution.repository.WorkflowExecutionRepository;
import com.example.workflow_platform.job.entity.JobDependency;
import com.example.workflow_platform.job.repository.JobDependencyRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class JobExecutionService {

    private static final long RETRY_DELAY_SECONDS = 10;
    private static final long LEASE_DURATION_SECONDS = 30;

    private final JobExecutionRepository jobExecutionRepository;
    private final JobDependencyRepository dependencyRepository;
    private final WorkflowExecutionRepository workflowExecutionRepository;

    public JobExecutionService(
            JobExecutionRepository jobExecutionRepository,
            JobDependencyRepository dependencyRepository,
            WorkflowExecutionRepository workflowExecutionRepository) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.dependencyRepository = dependencyRepository;
        this.workflowExecutionRepository = workflowExecutionRepository;
    }

    /**
     * Atomically claims a job if its current status is READY.
     *
     * Only one competing worker should successfully claim the job.
     */
    @Transactional
    public Long claimJob(
            Long workflowExecutionId,
            Long jobId,
            String workerId) {

        if (workerId == null || workerId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Worker ID must not be blank"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime leaseExpiry =
                now.plusSeconds(LEASE_DURATION_SECONDS);

        int updated = jobExecutionRepository.claimReadyJob(
                workflowExecutionId,
                jobId,
                workerId,
                JobExecutionStatus.READY,
                JobExecutionStatus.RUNNING,
                ExecutionStatus.RUNNING,
                now,
                leaseExpiry
        );

        if (updated == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Job is not READY or workflow is not RUNNING"
            );
        }

        JobExecution jobExecution =
                jobExecutionRepository
                        .findByWorkflowExecution_IdAndJob_Id(
                                workflowExecutionId,
                                jobId
                        )
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Job execution not found"
                        ));

        return jobExecution.getFencingToken();
    }

    /**
     * Marks a running job as SUCCESS and evaluates its dependent jobs.
     *
     * Returns the IDs of job definitions that have newly become READY.
     */
    @Transactional
    public List<Long> completeJob(
            Long workflowExecutionId,
            Long jobId,
            String workerId,
            Long fencingToken) {

        LocalDateTime now = LocalDateTime.now();

        int updated = jobExecutionRepository.completeRunningJob(
                workflowExecutionId,
                jobId,
                workerId,
                fencingToken,
                JobExecutionStatus.RUNNING,
                JobExecutionStatus.SUCCESS,
                now
        );

        if (updated == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Job is not RUNNING or worker has stale ownership"
            );
        }

        List<Long> newlyReadyJobs = new ArrayList<>();

        // 2. Find all job definitions that depend on this job.
        List<JobDependency> dependents =
                dependencyRepository.findByDependsOnJob_Id(jobId);

        for (JobDependency dependency : dependents) {

            Long dependentJobId = dependency.getJob().getId();

            // 3. Get this dependent job's execution in the same workflow run.
            Optional<JobExecution> dependentExecutionOptional =
                    jobExecutionRepository
                            .findByWorkflowExecution_IdAndJob_Id(
                                    workflowExecutionId,
                                    dependentJobId
                            );

            if (dependentExecutionOptional.isEmpty()) {
                continue;
            }

            JobExecution dependentExecution =
                    dependentExecutionOptional.get();

            // Only BLOCKED jobs can become READY.
            if (dependentExecution.getStatus()
                    != JobExecutionStatus.BLOCKED) {
                continue;
            }

            // 4. Every prerequisite must have succeeded.
            boolean allPrerequisitesSucceeded =
                    allPrerequisitesSucceeded(
                            workflowExecutionId,
                            dependentJobId
                    );

            if (!allPrerequisitesSucceeded) {
                continue;
            }

            // 5. Atomically transition BLOCKED -> READY.
            int released = jobExecutionRepository.unblockJob(
                    dependentExecution.getId(),
                    JobExecutionStatus.BLOCKED,
                    JobExecutionStatus.READY,
                    ExecutionStatus.RUNNING,
                    now
            );

            if (released == 1) {
                newlyReadyJobs.add(dependentJobId);
            }
        }



        // Check whether every job in this workflow execution has succeeded.
        long unfinishedCount =
                jobExecutionRepository
                        .countByWorkflowExecution_IdAndStatusNot(
                                workflowExecutionId,
                                JobExecutionStatus.SUCCESS
                        );

        if (unfinishedCount == 0) {

            WorkflowExecution workflowExecution =
                    workflowExecutionRepository.findById(workflowExecutionId)
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Workflow execution not found: "
                                            + workflowExecutionId
                            ));

            if (workflowExecution.getStatus() == ExecutionStatus.RUNNING) {
                workflowExecution.setStatus(ExecutionStatus.SUCCESS);
                workflowExecution.setCompletedAt(now);

                workflowExecutionRepository.save(workflowExecution);
            }
        }

        return newlyReadyJobs;
    }

    /**
     * Checks whether every prerequisite of the specified job
     * has a SUCCESS execution within the given workflow run.
     */
    private boolean allPrerequisitesSucceeded(
            Long workflowExecutionId,
            Long jobId) {

        List<JobDependency> prerequisites =
                dependencyRepository.findByJob_Id(jobId);

        for (JobDependency prerequisite : prerequisites) {

            Long prerequisiteJobId =
                    prerequisite.getDependsOnJob().getId();

            Optional<JobExecution> prerequisiteExecution =
                    jobExecutionRepository
                            .findByWorkflowExecution_IdAndJob_Id(
                                    workflowExecutionId,
                                    prerequisiteJobId
                            );

            if (prerequisiteExecution.isEmpty()
                    || prerequisiteExecution.get().getStatus()
                    != JobExecutionStatus.SUCCESS) {
                return false;
            }
        }

        return true;
    }

    @Transactional
    public JobFailureResponse failJob(
            Long workflowExecutionId,
            Long jobId,
            String workerId,
            Long fencingToken,
            String errorMessage) {

        LocalDateTime now = LocalDateTime.now();

        // 1. Lock and retrieve the job execution.
        JobExecution jobExecution =
                jobExecutionRepository.findForUpdate(
                        workflowExecutionId,
                        jobId
                ).orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job execution not found"
                ));

        // 2. Verify that the worker still owns the job.
        if (jobExecution.getStatus() != JobExecutionStatus.RUNNING
                || !Objects.equals(jobExecution.getWorkerId(), workerId)
                || !Objects.equals(jobExecution.getFencingToken(), fencingToken)
                || jobExecution.getLeaseExpiry() == null
                || !jobExecution.getLeaseExpiry().isAfter(now)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Job lease expired or worker no longer owns it"
            );
        }

        WorkflowExecution workflowExecution =
                jobExecution.getWorkflowExecution();

        if (workflowExecution.getStatus() != ExecutionStatus.RUNNING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Workflow execution is not RUNNING"
            );
        }

        // 3. Store the failure information.
        jobExecution.setErrorMessage(errorMessage);
        jobExecution.setWorkerId(null);
        jobExecution.setLeaseExpiry(null);

        int currentAttempt = jobExecution.getAttempt();
        int maxRetries = jobExecution.getJob().getMaxRetries();

        // 4. Decide whether another attempt is allowed.
        if (currentAttempt <= maxRetries) {

            LocalDateTime nextRetryAt =
                    now.plusSeconds(RETRY_DELAY_SECONDS);

            jobExecution.setStatus(JobExecutionStatus.RETRYING);
            jobExecution.setNextRetryAt(nextRetryAt);

            jobExecutionRepository.save(jobExecution);

            return new JobFailureResponse(
                    workflowExecutionId,
                    jobId,
                    currentAttempt,
                    "RETRYING",
                    nextRetryAt,
                    "Job failed. Retry scheduled."
            );
        }

        // 5. No retries remain. Mark the job as FAILED.
        jobExecution.setStatus(JobExecutionStatus.FAILED);
        jobExecution.setNextRetryAt(null);
        jobExecution.setCompletedAt(now);

        jobExecutionRepository.save(jobExecution);

        // 6. Fail the parent workflow execution.
        workflowExecution.setStatus(ExecutionStatus.FAILED);
        workflowExecution.setCompletedAt(now);

        workflowExecutionRepository.save(workflowExecution);

        // 7. Cancel all other nonterminal jobs (fail-fast policy).
        jobExecutionRepository.cancelOtherNonTerminalJobExecutions(
                workflowExecutionId,
                jobExecution.getId(),
                List.of(
                        JobExecutionStatus.READY,
                        JobExecutionStatus.BLOCKED,
                        JobExecutionStatus.RUNNING,
                        JobExecutionStatus.RETRYING
                ),
                JobExecutionStatus.CANCELLED,
                now
        );

        return new JobFailureResponse(
                workflowExecutionId,
                jobId,
                currentAttempt,
                "FAILED",
                null,
                "Maximum retry attempts exhausted. Workflow failed."
        );
    }

    @Transactional
    public int promoteDueRetries() {

        return jobExecutionRepository.promoteDueRetries(
                LocalDateTime.now(),
                JobExecutionStatus.RETRYING,
                JobExecutionStatus.READY,
                ExecutionStatus.RUNNING
        );
    }

    @Transactional
    public JobHeartbeatResponse heartbeatJob(
            Long workflowExecutionId,
            Long jobId,
            String workerId,
            Long fencingToken) {

        if (workerId == null || workerId.isBlank() || fencingToken == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Worker ID and fencing token are required"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime newLeaseExpiry =
                now.plusSeconds(LEASE_DURATION_SECONDS);

        int updated = jobExecutionRepository.renewLease(
                workflowExecutionId,
                jobId,
                workerId,
                fencingToken,
                JobExecutionStatus.RUNNING,
                ExecutionStatus.RUNNING,
                now,
                newLeaseExpiry
        );

        if (updated == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Lease expired or worker no longer owns this job"
            );
        }

        return new JobHeartbeatResponse(
                workflowExecutionId,
                jobId,
                workerId,
                fencingToken,
                newLeaseExpiry,
                "RUNNING"
        );
    }

    @Transactional
    public boolean recoverOneExpiredJob() {

        LocalDateTime now = LocalDateTime.now();

        // 1. Find one expired job and lock its database row.
        Optional<JobExecution> expiredJobOptional =
                jobExecutionRepository.findOneExpiredJobForRecovery(now);

        if (expiredJobOptional.isEmpty()) {
            return false;
        }

        JobExecution jobExecution = expiredJobOptional.get();

        WorkflowExecution workflowExecution =
                jobExecution.getWorkflowExecution();

        // 2. Ignore jobs belonging to an inactive execution.
        if (workflowExecution.getStatus() != ExecutionStatus.RUNNING) {

            jobExecution.setStatus(JobExecutionStatus.CANCELLED);
            jobExecution.setWorkerId(null);
            jobExecution.setLeaseExpiry(null);
            jobExecution.setNextRetryAt(null);
            jobExecution.setCompletedAt(now);

            jobExecutionRepository.save(jobExecution);
            return true;
        }
        
        // 3. Release old worker ownership.
        jobExecution.setWorkerId(null);
        jobExecution.setLeaseExpiry(null);

        jobExecution.setErrorMessage(
                "Worker lease expired before job completion"
        );

        int currentAttempt = jobExecution.getAttempt();
        int maxRetries = jobExecution.getJob().getMaxRetries();

        // 4. Schedule another attempt if retries remain.
        if (currentAttempt <= maxRetries) {

            LocalDateTime nextRetryAt =
                    now.plusSeconds(RETRY_DELAY_SECONDS);

            jobExecution.setStatus(JobExecutionStatus.RETRYING);
            jobExecution.setNextRetryAt(nextRetryAt);

            jobExecutionRepository.save(jobExecution);

            return true;
        }

        // 5. No retries remain. Mark the job as FAILED.
        jobExecution.setStatus(JobExecutionStatus.FAILED);
        jobExecution.setNextRetryAt(null);
        jobExecution.setCompletedAt(now);

        jobExecutionRepository.save(jobExecution);

        // 6. Fail the parent workflow.
        workflowExecution.setStatus(ExecutionStatus.FAILED);
        workflowExecution.setCompletedAt(now);

        workflowExecutionRepository.save(workflowExecution);

        // 7. Cancel other unfinished jobs.
        jobExecutionRepository.cancelOtherNonTerminalJobExecutions(
                workflowExecution.getId(),
                jobExecution.getId(),
                List.of(
                        JobExecutionStatus.READY,
                        JobExecutionStatus.BLOCKED,
                        JobExecutionStatus.RUNNING,
                        JobExecutionStatus.RETRYING
                ),
                JobExecutionStatus.CANCELLED,
                now
        );

        return true;
    }
}