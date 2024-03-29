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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch07.jobmanagement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Listener to change job exit code.
 * <p>
 * If the end state of the step includes "STEP COPLETED WITH SKIPS", put "JOB COPLETED WITH SKIPS" in the end state of the job.
 * </p>
 *
 * @since 2.0.1
 */
@Component
public class JobExitCodeChangeListener implements JobExecutionListener {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(JobExitCodeChangeListener.class);

    /**
     * Change job exit code.
     *
     * @param jobExecution Job execution.
     */
    @Override
    public void afterJob(JobExecution jobExecution) {
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            if ("STEP COMPLETED WITH SKIPS".equals(stepExecution.getExitStatus().getExitCode())) {
                jobExecution.setExitStatus(new ExitStatus("JOB COMPLETED WITH SKIPS"));
                logger.info("Change status 'JOB COMPLETED WITH SKIPS'");
                break;
            }
            if ("FORCE_FAIL_WITH_EXIT_CODE".equals(stepExecution.getExitStatus().getExitCode())) {
                jobExecution.setExitStatus(new ExitStatus("FAILED_CUSTOM"));
                logger.info("Change status 'FAILED_CUSTOM'");
                break;
            }
            if ("FORCE_STOP_WITH_EXIT_CODE".equals(stepExecution.getExitStatus().getExitCode())) {
                jobExecution.setExitStatus(new ExitStatus("STOPPED_CUSTOM"));
                logger.info("Change status 'STOPPED_CUSTOM'");
                break;
            }
        }
    }
}
