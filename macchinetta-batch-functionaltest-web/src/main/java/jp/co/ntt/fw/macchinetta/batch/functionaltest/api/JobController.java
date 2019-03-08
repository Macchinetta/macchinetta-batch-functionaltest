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

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyExistsException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.BooleanLatch;

import javax.inject.Inject;

/**
 * Controller for launching jobs and referring job information.
 * <p>
 * The API to be provided is as follows.
 * </p>
 * <ul>
 * <li>POST /job/{jobName} : Launching job.</li>
 * <li>GET /job/{jobExecutionId} : Reference job execution information.</li>
 * <li>PUT /job/stop/{jobExecutionId} : Stop a running job.</li>
 * <li>PUT /job/restart/{jobExecutionId} : Restart the job.</li>
 * </ul>
 *
 * @since 2.0.1
 */
@RequestMapping("job")
@RestController
public class JobController {

    /**
     * Job Operation interface.
     */
    @Inject
    JobOperator jobOperator;

    /**
     * Job explorer interface.
     */
    @Inject
    JobExplorer jobExplorer;

    @Inject
    DataSourceInitializer dataSourceInitializer;

    BooleanLatch booleanLatch = BooleanLatch.getInstance();

    /**
     * Launching job.
     *
     * @param jobName Job name.
     * @param requestResource Job operation resource. Job parameters are specified.
     * @return Job operation resource whose value is set to JobExecutionId when job execution succeeds, and ErrorMessage when
     *         failed.
     */
    @RequestMapping(value = "{jobName}", method = RequestMethod.POST)
    public ResponseEntity<JobOperationResource> launch(@PathVariable("jobName") String jobName,
            @RequestBody JobOperationResource requestResource) {

        JobOperationResource responseResource = new JobOperationResource();
        responseResource.setJobName(jobName);
        responseResource.setJobParams(requestResource.getJobParams());

        try {
            Long jobExecutionId = jobOperator.start(jobName, requestResource.getJobParams());
            responseResource.setJobExecutionId(jobExecutionId);
            responseResource.setResultMessage("Job launching task is scheduled.");
            return ResponseEntity.ok().body(responseResource);
        } catch (NoSuchJobException | JobInstanceAlreadyExistsException | JobParametersInvalidException e) {
            responseResource.setErrorMessage(e.getMessage());
            return ResponseEntity.badRequest().body(responseResource);
        }
    }

    /**
     * Reference job execution information.
     *
     * @param jobExecutionId Job execution id.
     * @return Job execution resource.
     */
    @RequestMapping(value = "{jobExecutionId}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public JobExecutionResource getJob(@PathVariable("jobExecutionId") Long jobExecutionId) {

        JobExecutionResource responseResource = new JobExecutionResource();
        responseResource.setJobExecutionId(jobExecutionId);

        JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);

        if (jobExecution == null) {
            responseResource.setErrorMessage("Job execution not found.");
        } else {
            mappingExecutionInfo(jobExecution, responseResource);
        }

        return responseResource;
    }

    /**
     * Stop a running job.
     *
     * @param jobExecutionId Job execution id.
     * @return Job operation resource
     */
    @RequestMapping(value = "stop/{jobExecutionId}", method = RequestMethod.PUT)
    public ResponseEntity<JobOperationResource> stop(@PathVariable("jobExecutionId") Long jobExecutionId) {

        JobOperationResource responseResource = new JobOperationResource();
        responseResource.setJobExecutionId(jobExecutionId);
        boolean result = false;
        try {
            result = jobOperator.stop(jobExecutionId);
            responseResource.setResultMessage(result ? "Successfully" : "Unsuccessfully");
            return ResponseEntity.ok().body(responseResource);
        } catch (NoSuchJobExecutionException | JobExecutionNotRunningException e) {
            responseResource.setErrorMessage(e.getMessage());
            return ResponseEntity.badRequest().body(responseResource);
        }
    }

    /**
     * Restart the job.
     *
     * @param jobExecutionId Job execution id.
     * @return Job operation resource
     */
    @RequestMapping(value = "restart/{jobExecutionId}", method = RequestMethod.PUT)
    public ResponseEntity<JobOperationResource> restart(@PathVariable("jobExecutionId") Long jobExecutionId) {

        JobOperationResource responseResource = new JobOperationResource();
        responseResource.setJobExecutionId(jobExecutionId);
        try {
            Long id = jobOperator.restart(jobExecutionId);
            responseResource.setResultMessage("Job restarted on job execution id = " + id);
            return ResponseEntity.ok().body(responseResource);
        } catch (JobInstanceAlreadyCompleteException | NoSuchJobExecutionException | NoSuchJobException | JobRestartException | JobParametersInvalidException e) {
            responseResource.setErrorMessage(e.getMessage());
            return ResponseEntity.badRequest().body(responseResource);
        }
    }

    /**
     * Copy job execution information.
     *
     * @param src Source of information.
     * @param dest Destination of information.
     */
    private void mappingExecutionInfo(JobExecution src, JobExecutionResource dest) {
        dest.setJobName(src.getJobInstance().getJobName());
        dest.setJobParameters(src.getJobParameters().toString());
        for (StepExecution se : src.getStepExecutions()) {
            StepExecutionResource ser = new StepExecutionResource();
            ser.setStepExecutionId(se.getId());
            ser.setStepName(se.getStepName());
            ser.setStatus(se.getStatus().toString());
            ser.setReadCount(se.getReadCount());
            ser.setWriteCount(se.getWriteCount());
            ser.setCommitCount(se.getCommitCount());
            ser.setRollbackCount(se.getRollbackCount());
            ser.setReadSkipCount(se.getReadSkipCount());
            ser.setProcessSkipCount(se.getProcessSkipCount());
            ser.setWriteSkipCount(se.getWriteSkipCount());
            ser.setStartTime(se.getStartTime());
            ser.setEndTime(se.getEndTime());
            ser.setLastUpdated(se.getLastUpdated());
            ser.setExecutionContext(se.getExecutionContext().toString());
            ser.setExitStatus(se.getExitStatus().toString());
            ser.setTerminateOnly(se.isTerminateOnly());
            ser.setFilterCount(se.getFilterCount());
            for (Throwable th : se.getFailureExceptions()) {
                ser.getFailureExceptions().add(th.toString());
            }
            dest.getStepExecutions().add(ser);
        }
        dest.setStatus(src.getStatus().toString());
        dest.setStartTime(src.getStartTime());
        dest.setCreateTime(src.getCreateTime());
        dest.setEndTime(src.getEndTime());
        dest.setLastUpdated(src.getLastUpdated());
        dest.setExitStatus(src.getExitStatus().toString());
        dest.setExecutionContext(src.getExecutionContext().toString());
        dest.setJobConfigurationName(src.getJobConfigurationName());
        for (Throwable th : src.getFailureExceptions()) {
            dest.getFailureExceptions().add(th.toString());
        }

    }

    @RequestMapping(value = "signal", method = RequestMethod.GET)
    public ResponseEntity<String> signal() {
        booleanLatch.signal();
        return ResponseEntity.ok().body("sent signal.");
    }

    @RequestMapping(value = "reset", method = RequestMethod.GET)
    public ResponseEntity<String> reset() {
        booleanLatch.initial();
        return ResponseEntity.ok().body("sent reset.");
    }
}
