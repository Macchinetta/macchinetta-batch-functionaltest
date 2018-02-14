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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Job execution logging listener.
 *
 * @since 5.0.0
 */
@Component
public class StepExecutionLoggingListener implements StepExecutionListener {
    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(StepExecutionLoggingListener.class);

    /**
     * Logging step started messages.
     *
     * @param stepExecution Job execution information.
     */
    @Override
    public void beforeStep(StepExecution stepExecution) {
        logger.info("step started. [stepName:{}]", stepExecution.getStepName());
    }

    /**
     * Logging step stopped messages.
     *
     * @param stepExecution Job execution information.
     * @return {@inheritDoc}
     */
    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        logger.info("step stopped. [stepName:{}]", stepExecution.getStepName());
        return null;
    }
}
