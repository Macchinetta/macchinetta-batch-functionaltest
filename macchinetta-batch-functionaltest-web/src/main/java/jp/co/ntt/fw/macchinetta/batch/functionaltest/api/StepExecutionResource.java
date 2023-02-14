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

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private long readCount = 0L;

    /**
     * Write count.
     */
    private long writeCount = 0L;

    /**
     * Commit count.
     */
    private long commitCount = 0L;

    /**
     * Rollback count.
     */
    private long rollbackCount = 0L;

    /**
     * Read skip count.
     */
    private long readSkipCount = 0L;

    /**
     * Process skip count.
     */
    private long processSkipCount = 0L;

    /**
     * Write skip count.
     */
    private long writeSkipCount = 0L;

    /**
     * Step start time.
     */
    private LocalDateTime startTime = null;

    /**
     * Step end time.
     */
    private LocalDateTime endTime = null;

    /**
     * Job repository last updated time.
     */
    private LocalDateTime lastUpdated = null;

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
    private long filterCount;

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
    public long getReadCount() {
        return readCount;
    }

    /**
     * Read count.
     *
     * @param readCount New read count.
     */
    public void setReadCount(long readCount) {
        this.readCount = readCount;
    }

    /**
     * Write count.
     *
     * @return The current of write count.
     */
    public long getWriteCount() {
        return writeCount;
    }

    /**
     * Write count.
     *
     * @param writeCount New write count.
     */
    public void setWriteCount(long writeCount) {
        this.writeCount = writeCount;
    }

    /**
     * Commit count.
     *
     * @return The current of commit count.
     */
    public long getCommitCount() {
        return commitCount;
    }

    /**
     * Commit count.
     *
     * @param commitCount New commit count.
     */
    public void setCommitCount(long commitCount) {
        this.commitCount = commitCount;
    }

    /**
     * Rollback count.
     *
     * @return The current of rollback count.
     */
    public long getRollbackCount() {
        return rollbackCount;
    }

    /**
     * Rollback count.
     *
     * @param rollbackCount New rollback count.
     */
    public void setRollbackCount(long rollbackCount) {
        this.rollbackCount = rollbackCount;
    }

    /**
     * Read skip count.
     *
     * @return The current of read skip count.
     */
    public long getReadSkipCount() {
        return readSkipCount;
    }

    /**
     * Read skip count.
     *
     * @param readSkipCount New read skip count.
     */
    public void setReadSkipCount(long readSkipCount) {
        this.readSkipCount = readSkipCount;
    }

    /**
     * Process skip count.
     *
     * @return The current of process skip count.
     */
    public long getProcessSkipCount() {
        return processSkipCount;
    }

    /**
     * Process skip count.
     *
     * @param processSkipCount New process skip count.
     */
    public void setProcessSkipCount(long processSkipCount) {
        this.processSkipCount = processSkipCount;
    }

    /**
     * Write skip count.
     *
     * @return The current of write skip count.
     */
    public long getWriteSkipCount() {
        return writeSkipCount;
    }

    /**
     * Write skip count.
     *
     * @param writeSkipCount New write skip count.
     */
    public void setWriteSkipCount(long writeSkipCount) {
        this.writeSkipCount = writeSkipCount;
    }

    /**
     * Step start time.
     *
     * @return The current of step start time.
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * Step start time.
     *
     * @param startTime New step start time.
     */
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    /**
     * Step end time.
     *
     * @return The current of step end time.
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }

    /**
     * Step end time.
     *
     * @param endTime New step end time.
     */
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    /**
     * Job repository last updated time.
     *
     * @return The current of job repository last updated time.
     */
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Job repository last updated time.
     *
     * @param lastUpdated New job repository last updated time.
     */
    public void setLastUpdated(LocalDateTime lastUpdated) {
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
    public long getFilterCount() {
        return filterCount;
    }

    /**
     * Filter count.
     *
     * @param filterCount New filter count.
     */
    public void setFilterCount(long filterCount) {
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
