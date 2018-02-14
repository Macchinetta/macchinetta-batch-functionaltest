/*
 * Copyright (C) 2017 NTT Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package jp.co.ntt.fw.macchinetta.batch.functionaltest.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Resource of job execution information.
 *
 * @since 5.0.0
 */
public class JobExecutionResource {

    /**
     * Job execution id.
     */
    private Long jobExecutionId = null;

    /**
     * Job name.
     */
    private String jobName = null;

    /**
     * Job parameters.
     */
    private String jobParameters = null;

    /**
     * Step executions.
     */
    private List<StepExecutionResource> stepExecutions = new ArrayList<>();

    /**
     * Batch status.
     */
    private String status = null;

    /**
     * Job start time.
     */
    private Date startTime = null;

    /**
     * Job repository create time.
     */
    private Date createTime = null;

    /**
     * Job end time.
     */
    private Date endTime = null;

    /**
     * Job repository last updated time.
     */
    private Date lastUpdated = null;

    /**
     * Exit status.
     */
    private String exitStatus = null;

    /**
     * Job execution context.
     */
    private String executionContext = null;

    /**
     * Job failure exceptions.
     */
    private List<String> failureExceptions = new ArrayList<>();

    /**
     * Job configuration name.
     */
    private String jobConfigurationName = null;

    /**
     * Job explorer error message.
     */
    private String errorMessage;

    /**
     * Job execution id.
     *
     * @return The current of job execution id.
     */
    public Long getJobExecutionId() {
        return jobExecutionId;
    }

    /**
     * Job execution id.
     *
     * @param jobExecutionId New job execution id.
     */
    public void setJobExecutionId(Long jobExecutionId) {
        this.jobExecutionId = jobExecutionId;
    }

    /**
     * Job name.
     *
     * @return The current of job name.
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * Job name.
     *
     * @param jobName New job name.
     */
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    /**
     * Job parameters.
     *
     * @return The current of job parameters.
     */
    public String getJobParameters() {
        return jobParameters;
    }

    /**
     * Job parameters.
     *
     * @param jobParameters New job parameters.
     */
    public void setJobParameters(String jobParameters) {
        this.jobParameters = jobParameters;
    }

    /**
     * Step executions.
     *
     * @return The current of step executions.
     */
    public List<StepExecutionResource> getStepExecutions() {
        return stepExecutions;
    }

    /**
     * Step executions.
     *
     * @param stepExecutions New step executions.
     */
    public void setStepExecutions(List<StepExecutionResource> stepExecutions) {
        this.stepExecutions = stepExecutions;
    }

    /**
     * Batch status.
     *
     * @return The current of batch status.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Batch status.
     *
     * @param status New batch status.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Job start time.
     *
     * @return The current of job start time.
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * Job start time.
     *
     * @param startTime New job start time.
     */
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    /**
     * Job repository create time.
     *
     * @return The current of job repository create time.
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * Job repository create time.
     *
     * @param createTime New job repository create time.
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * Job end time.
     *
     * @return The current of job end time.
     */
    public Date getEndTime() {
        return endTime;
    }

    /**
     * Job end time.
     *
     * @param endTime New job end time.
     */
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    /**
     * Job repository last updated time.
     *
     * @return The current of job repository last updated time.
     */
    public Date getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Job repository last updated time.
     *
     * @param lastUpdated New job repository last updated time.
     */
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * Exit status.
     *
     * @return The current of exit status.
     */
    public String getExitStatus() {
        return exitStatus;
    }

    /**
     * Exit status.
     *
     * @param exitStatus New exit status.
     */
    public void setExitStatus(String exitStatus) {
        this.exitStatus = exitStatus;
    }

    /**
     * Job execution context.
     *
     * @return The current of job execution context.
     */
    public String getExecutionContext() {
        return executionContext;
    }

    /**
     * Job execution context.
     *
     * @param executionContext New job execution context.
     */
    public void setExecutionContext(String executionContext) {
        this.executionContext = executionContext;
    }

    /**
     * Job failure exceptions.
     *
     * @return The current of job failure exceptions.
     */
    public List<String> getFailureExceptions() {
        return failureExceptions;
    }

    /**
     * Job failure exceptions.
     *
     * @param failureExceptions New job failure exceptions.
     */
    public void setFailureExceptions(List<String> failureExceptions) {
        this.failureExceptions = failureExceptions;
    }

    /**
     * Job configuration name.
     *
     * @return The current of job configuration name.
     */
    public String getJobConfigurationName() {
        return jobConfigurationName;
    }

    /**
     * Job configuration name.
     *
     * @param jobConfigurationName New job configuration name.
     */
    public void setJobConfigurationName(String jobConfigurationName) {
        this.jobConfigurationName = jobConfigurationName;
    }

    /**
     * Job explorer error message.
     *
     * @return The current of job explorer error message.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Job explorer error message.
     *
     * @param errorMessage New job explorer error message.
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
