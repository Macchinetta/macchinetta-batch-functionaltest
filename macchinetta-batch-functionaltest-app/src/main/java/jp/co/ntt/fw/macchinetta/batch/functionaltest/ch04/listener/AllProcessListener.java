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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepListenerSupport;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * A listener for logging all process. The process of log output is as follows.
 * <ul>
 * <li>Job Execution</li>
 * <li>Step Execution</li>
 * <li>Chunk</li>
 * <li>ItemReader</li>
 * <li>ItemProcessor</li>
 * <li>ItemWriter</li>
 * </ul>
 *
 * @since 2.0.1
 */
@Component
public class AllProcessListener extends StepListenerSupport<Object, Object> implements JobExecutionListener {
    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(AllProcessListener.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeJob(JobExecution jobExecution) {
        logger.info("Common Logging: job started. [{}]", jobExecution.getJobInstance().getJobName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterJob(JobExecution jobExecution) {
        logger.info("Common Logging: job finished. [{}]", jobExecution.getJobInstance().getJobName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeStep(StepExecution stepExecution) {
        logger.info("Common Logging: step started. [{}]", stepExecution.getStepName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        logger.info("Common Logging: step finished. [{}]", stepExecution.getStepName());
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeChunk(ChunkContext context) {
        logger.info("Common Logging: before chunk.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterChunk(ChunkContext context) {
        logger.info("Common Logging: after chunk.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterChunkError(ChunkContext context) {
        Exception e = (Exception) context.getAttribute(ChunkListener.ROLLBACK_EXCEPTION_KEY);
        logger.error("Common Logging: after chunk error.", e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeRead() {
        logger.info("Common Logging: before read.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterRead(Object item) {
        logger.info("Common Logging: after read. [Read item: {}]", item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReadError(Exception ex) {
        logger.error("Common Logging: on read error.", ex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeWrite(List<?> items) {
        logger.info("Common Logging: before write. [Item size :{}]", items.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterWrite(List<?> items) {
        logger.info("Common Logging: after write. [Item size :{}]", items.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onWriteError(Exception exception, List<?> items) {
        logger.error("Common Logging: on write error.", exception);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeProcess(Object item) {
        logger.info("Common Logging: before process. [Item :{}]", item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterProcess(Object item, Object result) {
        logger.info("Common Logging: after process. [Result :{}]", result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onProcessError(Object item, Exception e) {
        logger.error("Common Logging: on process error.", e);
    }

}
