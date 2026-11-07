package com.example.workflow_platform.execution.dispatcher;

import com.example.workflow_platform.execution.dto.ReadyJobCandidate;
import com.example.workflow_platform.execution.enums.ExecutionStatus;
import com.example.workflow_platform.execution.enums.JobExecutionStatus;
import com.example.workflow_platform.execution.repository.JobExecutionRepository;
import com.example.workflow_platform.execution.service.JobExecutionService;
import com.example.workflow_platform.execution.worker.DemoJobProcessor;
import com.example.workflow_platform.execution.worker.WorkerIdentity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

@Component
public class JobDispatcher {

    private static final Logger log =
            LoggerFactory.getLogger(JobDispatcher.class);

    private final JobExecutionRepository jobExecutionRepository;
    private final JobExecutionService jobExecutionService;
    private final DemoJobProcessor jobProcessor;
    private final WorkerIdentity workerIdentity;
    private final ExecutorService workerExecutor;

    private final Semaphore availableSlots;

    public JobDispatcher(
            JobExecutionRepository jobExecutionRepository,
            JobExecutionService jobExecutionService,
            DemoJobProcessor jobProcessor,
            WorkerIdentity workerIdentity,
            @Qualifier("jobWorkerExecutor")
            ExecutorService workerExecutor,
            @Value("${workflow.worker.pool-size:3}") int poolSize) {

        this.jobExecutionRepository = jobExecutionRepository;
        this.jobExecutionService = jobExecutionService;
        this.jobProcessor = jobProcessor;
        this.workerIdentity = workerIdentity;
        this.workerExecutor = workerExecutor;

        this.availableSlots = new Semaphore(poolSize);
    }

    /**
     * Periodically searches for READY jobs and dispatches them.
     */
    @Scheduled(fixedDelayString =
            "${workflow.dispatcher.poll-interval-ms:1000}")
    public void dispatchReadyJobs() {

        while (availableSlots.tryAcquire()) {

            List<ReadyJobCandidate> candidates =
                    jobExecutionRepository.findReadyJobs(
                            JobExecutionStatus.READY,
                            ExecutionStatus.RUNNING,
                            PageRequest.of(0, 1)
                    );

            if (candidates.isEmpty()) {
                availableSlots.release();
                break;
            }

            ReadyJobCandidate candidate = candidates.get(0);

            Long executionId = candidate.workflowExecutionId();
            Long jobId = candidate.jobId();

            String workerId = workerIdentity.getWorkerId();

            Long fencingToken;

            // 1. Atomically claim the job before submitting it.
            try {
                fencingToken = jobExecutionService.claimJob(
                        executionId,
                        jobId,
                        workerId
                );

            } catch (ResponseStatusException ex) {

                availableSlots.release();

                // Another dispatcher may have claimed this job.
                if (ex.getStatusCode().equals(HttpStatus.CONFLICT)) {
                    continue;
                }

                log.error("Unable to claim job {}", jobId, ex);
                break;
            }

            log.info(
                    "Claimed job {} for execution {} with token {}",
                    jobId,
                    executionId,
                    fencingToken
            );

            // 2. Submit claimed job to the worker pool.
            try {
                workerExecutor.execute(() ->
                        executeJob(
                                executionId,
                                jobId,
                                workerId,
                                fencingToken
                        )
                );

            } catch (RejectedExecutionException ex) {

                availableSlots.release();

                // Job remains RUNNING with a lease.
                // The lease recovery scheduler can recover it later.
                log.error(
                        "Worker pool rejected job {}. Lease recovery will handle it.",
                        jobId,
                        ex
                );

                break;
            }
        }
    }

    /**
     * Executes one claimed job and reports its outcome.
     */
    private void executeJob(
            Long executionId,
            Long jobId,
            String workerId,
            Long fencingToken) {

        try {

            // 3. Execute the actual job logic.
            try {
                jobProcessor.execute(executionId, jobId);

            } catch (InterruptedException ex) {

                Thread.currentThread().interrupt();

                reportFailure(
                        executionId,
                        jobId,
                        workerId,
                        fencingToken,
                        "Worker interrupted"
                );

                return;

            } catch (Exception ex) {

                reportFailure(
                        executionId,
                        jobId,
                        workerId,
                        fencingToken,
                        ex.getMessage() == null
                                ? "Job execution failed"
                                : ex.getMessage()
                );

                return;
            }

            // 4. Report successful completion.
            List<Long> newlyReadyJobs =
                    jobExecutionService.completeJob(
                            executionId,
                            jobId,
                            workerId,
                            fencingToken
                    );

            log.info(
                    "Completed job {} in execution {}. Newly READY jobs: {}",
                    jobId,
                    executionId,
                    newlyReadyJobs
            );

        } catch (RuntimeException ex) {

            // The lease might have expired or ownership might have changed.
            // Do not blindly mark a stale job as failed.
            log.error(
                    "Could not finalize job {} in execution {}",
                    jobId,
                    executionId,
                    ex
            );

        } finally {

            // 5. Release worker-pool capacity.
            availableSlots.release();
        }
    }

    private void reportFailure(
            Long executionId,
            Long jobId,
            String workerId,
            Long fencingToken,
            String errorMessage) {

        try {
            jobExecutionService.failJob(
                    executionId,
                    jobId,
                    workerId,
                    fencingToken,
                    errorMessage
            );

            log.warn(
                    "Job {} failed in execution {}",
                    jobId,
                    executionId
            );

        } catch (RuntimeException ex) {

            log.error(
                    "Could not report failure for job {} in execution {}",
                    jobId,
                    executionId,
                    ex
            );
        }
    }
}