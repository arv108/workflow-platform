package com.example.workflow_platform.job.repository;

import com.example.workflow_platform.job.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByWorkflow_Id(Long workflowId);
}