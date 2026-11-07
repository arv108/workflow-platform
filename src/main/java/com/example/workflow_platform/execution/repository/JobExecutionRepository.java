package com.example.workflow_platform.execution.repository;

import com.example.workflow_platform.execution.dto.ReadyJobCandidate;
import com.example.workflow_platform.execution.entity.JobExecution;
import com.example.workflow_platform.execution.enums.ExecutionStatus;
import com.example.workflow_platform.execution.enums.JobExecutionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JobExecutionRepository
        extends JpaRepository<JobExecution, Long> {

    Optional<JobExecution> findByWorkflowExecution_IdAndJob_Id(
            Long workflowExecutionId,
            Long jobId
    );

    long countByWorkflowExecution_IdAndStatusNot(
            Long workflowExecutionId,
            JobExecutionStatus status
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE JobExecution je
       SET je.status = :runningStatus,
           je.workerId = :workerId,
           je.startedAt = :now,
           je.attempt = je.attempt + 1,
           je.fencingToken = je.fencingToken + 1,
           je.leaseExpiry = :leaseExpiry,
           je.nextRetryAt = NULL,
           je.updatedAt = :now
     WHERE je.workflowExecution.id = :executionId
       AND je.job.id = :jobId
       AND je.status = :readyStatus
       AND je.workflowExecution.status = :workflowRunningStatus
    """)
    int claimReadyJob(
            @Param("executionId") Long executionId,
            @Param("jobId") Long jobId,
            @Param("workerId") String workerId,
            @Param("readyStatus") JobExecutionStatus readyStatus,
            @Param("runningStatus") JobExecutionStatus runningStatus,
            @Param("workflowRunningStatus") ExecutionStatus workflowRunningStatus,
            @Param("now") LocalDateTime now,
            @Param("leaseExpiry") LocalDateTime leaseExpiry
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE JobExecution je
       SET je.status = :successStatus,
           je.completedAt = :now,
           je.leaseExpiry = NULL,
           je.updatedAt = :now
     WHERE je.workflowExecution.id = :executionId
       AND je.job.id = :jobId
       AND je.status = :runningStatus
       AND je.workerId = :workerId
       AND je.fencingToken = :fencingToken
       AND je.leaseExpiry > :now
    """)
    int completeRunningJob(
            @Param("executionId") Long executionId,
            @Param("jobId") Long jobId,
            @Param("workerId") String workerId,
            @Param("fencingToken") Long fencingToken,
            @Param("runningStatus") JobExecutionStatus runningStatus,
            @Param("successStatus") JobExecutionStatus successStatus,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE JobExecution je
       SET je.status = :readyStatus,
           je.updatedAt = :now
     WHERE je.id = :jobExecutionId
       AND je.status = :blockedStatus
       AND je.workflowExecution.status = :workflowRunningStatus
    """)
    int unblockJob(
            @Param("jobExecutionId") Long jobExecutionId,
            @Param("blockedStatus") JobExecutionStatus blockedStatus,
            @Param("readyStatus") JobExecutionStatus readyStatus,
            @Param("workflowRunningStatus") ExecutionStatus workflowRunningStatus,
            @Param("now") LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT je
    FROM JobExecution je
    WHERE je.workflowExecution.id = :executionId
      AND je.job.id = :jobId
    """)
    Optional<JobExecution> findForUpdate(
            @Param("executionId") Long executionId,
            @Param("jobId") Long jobId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE JobExecution je
       SET je.status = :readyStatus,
           je.nextRetryAt = NULL,
           je.updatedAt = :now
     WHERE je.status = :retryingStatus
       AND je.nextRetryAt IS NOT NULL
       AND je.nextRetryAt <= :now
       AND je.workflowExecution.status = :workflowRunningStatus
    """)
    int promoteDueRetries(
            @Param("now") LocalDateTime now,
            @Param("retryingStatus") JobExecutionStatus retryingStatus,
            @Param("readyStatus") JobExecutionStatus readyStatus,
            @Param("workflowRunningStatus") ExecutionStatus workflowRunningStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE JobExecution je
       SET je.status = :cancelledStatus,
           je.workerId = NULL,
           je.leaseExpiry = NULL,
           je.nextRetryAt = NULL,
           je.completedAt = :now,
           je.updatedAt = :now
     WHERE je.workflowExecution.id = :executionId
       AND je.id <> :failedJobExecutionId
       AND je.status IN :nonTerminalStatuses
    """)
    int cancelOtherNonTerminalJobExecutions(
            @Param("executionId") Long executionId,
            @Param("failedJobExecutionId") Long failedJobExecutionId,
            @Param("nonTerminalStatuses") List<JobExecutionStatus> nonTerminalStatuses,
            @Param("cancelledStatus") JobExecutionStatus cancelledStatus,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE JobExecution je
       SET je.leaseExpiry = :newLeaseExpiry,
           je.updatedAt = :now
     WHERE je.workflowExecution.id = :executionId
       AND je.job.id = :jobId
       AND je.status = :runningStatus
       AND je.workerId = :workerId
       AND je.fencingToken = :fencingToken
       AND je.leaseExpiry > :now
       AND je.workflowExecution.status = :workflowRunningStatus
    """)
    int renewLease(
            @Param("executionId") Long executionId,
            @Param("jobId") Long jobId,
            @Param("workerId") String workerId,
            @Param("fencingToken") Long fencingToken,
            @Param("runningStatus") JobExecutionStatus runningStatus,
            @Param("workflowRunningStatus") ExecutionStatus workflowRunningStatus,
            @Param("now") LocalDateTime now,
            @Param("newLeaseExpiry") LocalDateTime newLeaseExpiry
    );

    @Query(value = """
    SELECT *
    FROM job_execution
    WHERE status = 'RUNNING'
      AND lease_expiry IS NOT NULL
      AND lease_expiry <= :now
    ORDER BY lease_expiry
    LIMIT 1
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    Optional<JobExecution> findOneExpiredJobForRecovery(
            @Param("now") LocalDateTime now
    );

    @Query("""
    SELECT new com.example.workflow_platform.execution.dto.ReadyJobCandidate(
        je.workflowExecution.id,
        je.job.id
    )
    FROM JobExecution je
    WHERE je.status = :readyStatus
      AND je.workflowExecution.status = :workflowStatus
    ORDER BY je.createdAt ASC
    """)
    List<ReadyJobCandidate> findReadyJobs(
            @Param("readyStatus") JobExecutionStatus readyStatus,
            @Param("workflowStatus") ExecutionStatus workflowStatus,
            Pageable pageable
    );
}
