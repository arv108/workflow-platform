package com.example.workflow_platform.execution.dto;

import java.util.ArrayList;
import java.util.List;

public class ExecutionJobResponseDto {

    private long executionId;
    private String status;
    private List<Jobs> jobsList = new ArrayList<>();

    public long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(long executionId) {
        this.executionId = executionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Jobs> getJobsList() {
        return jobsList;
    }

    public void setJobsList(Jobs jobs) {
        this.jobsList.add(jobs);
    }
}