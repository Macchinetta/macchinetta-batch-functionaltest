/*
 * Copyright (C) 2018 NTT Corporation
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

/**
 * Resource of job operation.
 *
 * @since 5.0.0
 */
public class JobOperationResource {

    /**
     * Job name.
     */
    private String jobName = null;

    /**
     * Job parameters.
     */
    private String jobParams = null;

    /**
     * Job execution id.
     */
    private Long jobExecutionId = null;

    /**
     * Operation result message.
     */
    private String resultMessage = null;

    /**
     * Operation error message.
     */
    private String errorMessage = null;

    /**
     * Job name.
     *
     * @return The current job name.
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
     * @return The current job parameters.
     */
    public String getJobParams() {
        return jobParams;
    }

    /**
     * Job parameters.
     *
     * @param jobParams New job parameters.
     */
    public void setJobParams(String jobParams) {
        this.jobParams = jobParams;
    }

    /**
     * Job execution id.
     *
     * @return The current job execution id.
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
     * Operation result message.
     *
     * @return The current of operation result message.
     */
    public String getResultMessage() {
        return resultMessage;
    }

    /**
     * Operation result message.
     *
     * @param resultMessage New operation result message.
     */
    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    /**
     * Operation error message.
     *
     * @return The current operation error message.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Operation error message.
     *
     * @param errorMessage New operation error message.
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

}
