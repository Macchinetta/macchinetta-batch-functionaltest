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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.exceptionhandling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Logging listener for Job.
 *
 * @since 5.0.0
 */
@Component
public class JobErrorLoggingListener implements JobExecutionListener {
    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(JobErrorLoggingListener.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeJob(JobExecution jobExecution) {
        // do nothing.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterJob(JobExecution jobExecution) {

        // whole job execution
        List<Throwable> exceptions = jobExecution.getAllFailureExceptions();
        if (exceptions.isEmpty()) {
            return;
        }
        logger.info("This job has occurred some exceptions as follow. [job-name:{}] [size:{}]",
                jobExecution.getJobInstance().getJobName(), exceptions.size());
        for (Throwable th : exceptions) {
            logger.error("exception has occurred in job.", th);
        }

        // per step execution
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            Object errorItem = stepExecution.getExecutionContext().get("ERROR_ITEM");
            if (errorItem != null) {
                logger.error("detected error on this item processing. [step:{}] [item:{}]", stepExecution.getStepName(),
                        errorItem);
            }
        }
    }
}
