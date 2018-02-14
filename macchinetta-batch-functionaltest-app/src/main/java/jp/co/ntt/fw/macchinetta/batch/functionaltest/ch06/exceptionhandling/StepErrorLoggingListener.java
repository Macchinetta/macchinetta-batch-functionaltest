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
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Logging listener for Step.
 *
 * @since 5.0.0
 */
@Component
public class StepErrorLoggingListener implements StepExecutionListener {
    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(StepErrorLoggingListener.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeStep(StepExecution stepExecution) {
        // do nothing.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        List<Throwable> exceptions = stepExecution.getFailureExceptions();
        if (exceptions.isEmpty()) {
            return ExitStatus.COMPLETED;
        }

        logger.info("This step has occurred some exceptions as follow. [step-name:{}] [size:{}]",
                stepExecution.getStepName(), exceptions.size());
        for (Throwable th : exceptions) {
            logger.error("exception has occurred in job.", th);
        }
        return ExitStatus.FAILED;
    }
}
