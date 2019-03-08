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
 * Resource of step execution information.
 *
 * @since 2.0.1
 */
public class StepExecutionResource {

    /**
     * Step execution id.
     */
    private Long stepExecutionId = null;

    /**
     * Step name.
     */
    private String stepName = null;

    /**
     * Batch status of step.
     */
    private String status = null;

    /**
     * Read count.
     */
    private int readCount = 0;

    /**
     * Write count.
     */
    private int writeCount = 0;

    /**
     * Commit count.
     */
    private int commitCount = 0;

    /**
     * Rollback count.
     */
    private int rollbackCount = 0;

    /**
     * Read skip count.
     */
    private int readSkipCount = 0;

    /**
     * Process skip count.
     */
    private int processSkipCount = 0;

    /**
     * Write skip count.
     */
    private int writeSkipCount = 0;

    /**
     * Step start time.
     */
    private Date startTime = null;

    /**
     * Step end time.
     */
    private Date endTime = null;

    /**
     * Job repository last updated time.
     */
    private Date lastUpdated = null;

    /**
     * Step execution context.
     */
    private String executionContext = null;

    /**
     * Exit status of step.
     */
    private String exitStatus = null;

    /**
     * Terminate only flag.
     */
    private boolean terminateOnly;

    /**
     * Filter count.
     */
    private int filterCount;

    /**
     * Step failure exceptions.
     */
    private List<String> failureExceptions = new ArrayList<>();

    /**
     * Step execution id.
     *
     * @return The current of step execution id.
     */
    public Long getStepExecutionId() {
        return stepExecutionId;
    }

    /**
     * Step execution id.
     *
     * @param stepExecutionId New step execution id.
     */
    public void setStepExecutionId(Long stepExecutionId) {
        this.stepExecutionId = stepExecutionId;
    }

    /**
     * Step name.
     *
     * @return The current of step name.
     */
    public String getStepName() {
        return stepName;
    }

    /**
     * Step name.
     *
     * @param stepName New step name.
     */
    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    /**
     * Batch status of step.
     *
     * @return The current of batch status of step.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Batch status of step.
     *
     * @param status New batch status of step.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Read count.
     *
     * @return The current of read count.
     */
    public int getReadCount() {
        return readCount;
    }

    /**
     * Read count.
     *
     * @param readCount New read count.
     */
    public void setReadCount(int readCount) {
        this.readCount = readCount;
    }

    /**
     * Write count.
     *
     * @return The current of write count.
     */
    public int getWriteCount() {
        return writeCount;
    }

    /**
     * Write count.
     *
     * @param writeCount New write count.
     */
    public void setWriteCount(int writeCount) {
        this.writeCount = writeCount;
    }

    /**
     * Commit count.
     *
     * @return The current of commit count.
     */
    public int getCommitCount() {
        return commitCount;
    }

    /**
     * Commit count.
     *
     * @param commitCount New commit count.
     */
    public void setCommitCount(int commitCount) {
        this.commitCount = commitCount;
    }

    /**
     * Rollback count.
     *
     * @return The current of rollback count.
     */
    public int getRollbackCount() {
        return rollbackCount;
    }

    /**
     * Rollback count.
     *
     * @param rollbackCount New rollback count.
     */
    public void setRollbackCount(int rollbackCount) {
        this.rollbackCount = rollbackCount;
    }

    /**
     * Read skip count.
     *
     * @return The current of read skip count.
     */
    public int getReadSkipCount() {
        return readSkipCount;
    }

    /**
     * Read skip count.
     *
     * @param readSkipCount New read skip count.
     */
    public void setReadSkipCount(int readSkipCount) {
        this.readSkipCount = readSkipCount;
    }

    /**
     * Process skip count.
     *
     * @return The current of process skip count.
     */
    public int getProcessSkipCount() {
        return processSkipCount;
    }

    /**
     * Process skip count.
     *
     * @param processSkipCount New process skip count.
     */
    public void setProcessSkipCount(int processSkipCount) {
        this.processSkipCount = processSkipCount;
    }

    /**
     * Write skip count.
     *
     * @return The current of write skip count.
     */
    public int getWriteSkipCount() {
        return writeSkipCount;
    }

    /**
     * Write skip count.
     *
     * @param writeSkipCount New write skip count.
     */
    public void setWriteSkipCount(int writeSkipCount) {
        this.writeSkipCount = writeSkipCount;
    }

    /**
     * Step start time.
     *
     * @return The current of step start time.
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * Step start time.
     *
     * @param startTime New step start time.
     */
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    /**
     * Step end time.
     *
     * @return The current of step end time.
     */
    public Date getEndTime() {
        return endTime;
    }

    /**
     * Step end time.
     *
     * @param endTime New step end time.
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
     * Step execution context.
     *
     * @return The current of step execution context.
     */
    public String getExecutionContext() {
        return executionContext;
    }

    /**
     * Step execution context.
     *
     * @param executionContext New step execution context.
     */
    public void setExecutionContext(String executionContext) {
        this.executionContext = executionContext;
    }

    /**
     * Exit status of step.
     *
     * @return The current of exit status of step.
     */
    public String getExitStatus() {
        return exitStatus;
    }

    /**
     * Exit status of step.
     *
     * @param exitStatus New exit status of step.
     */
    public void setExitStatus(String exitStatus) {
        this.exitStatus = exitStatus;
    }

    /**
     * Terminate only flag.
     *
     * @return The current of terminate only flag.
     */
    public boolean isTerminateOnly() {
        return terminateOnly;
    }

    /**
     * Terminate only flag.
     *
     * @param terminateOnly New terminate only flag.
     */
    public void setTerminateOnly(boolean terminateOnly) {
        this.terminateOnly = terminateOnly;
    }

    /**
     * Filter count.
     *
     * @return The current of filter count.
     */
    public int getFilterCount() {
        return filterCount;
    }

    /**
     * Filter count.
     *
     * @param filterCount New filter count.
     */
    public void setFilterCount(int filterCount) {
        this.filterCount = filterCount;
    }

    /**
     * Step failure exceptions.
     *
     * @return The current of step failure exceptions.
     */
    public List<String> getFailureExceptions() {
        return failureExceptions;
    }

    /**
     * Step failure exceptions.
     *
     * @param failureExceptions New step failure exceptions.
     */
    public void setFailureExceptions(List<String> failureExceptions) {
        this.failureExceptions = failureExceptions;
    }

}
