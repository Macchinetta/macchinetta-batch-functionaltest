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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.listener.annotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterChunk;
import org.springframework.batch.core.annotation.AfterChunkError;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.AfterProcess;
import org.springframework.batch.core.annotation.AfterRead;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.AfterWrite;
import org.springframework.batch.core.annotation.BeforeChunk;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.batch.core.annotation.BeforeProcess;
import org.springframework.batch.core.annotation.BeforeRead;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.annotation.BeforeWrite;
import org.springframework.batch.core.annotation.OnProcessError;
import org.springframework.batch.core.annotation.OnReadError;
import org.springframework.batch.core.annotation.OnWriteError;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;

import java.util.List;

/**
 * ItemProcessor to check amount.
 *
 * @since 5.0.0
 */
@Component("annotationAmountCheckProcessor")
public class AnnotationAmountCheckProcessor implements ItemProcessor<SalesPlanDetail, SalesPlanDetail> {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(AnnotationAmountCheckProcessor.class);

    /**
     * Check that the amount is 0 or more.
     *
     * @param item Item to be checked.
     * @return Item passed the check.
     * @throws Exception {@link IllegalArgumentException} occurs if the check fails.
     */
    @Override
    public SalesPlanDetail process(SalesPlanDetail item) throws Exception {
        if (item.getAmount().signum() == -1) {
            throw new IllegalArgumentException("amount is negative.");
        }

        return item;
    }

    // listeners by annotations

    @BeforeJob
    public void beforeJob(JobExecution jobExecution) {
        logger.info("Annotation Logging: job started. [{}]", jobExecution.getJobInstance().getJobName());
    }

    @AfterJob
    public void afterJob(JobExecution jobExecution) {
        logger.info("Annotation Logging: job finished. [{}]", jobExecution.getJobInstance().getJobName());
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        logger.info("Annotation Logging: step started. [{}]", stepExecution.getStepName());
    }

    @AfterStep
    public ExitStatus afterStep(StepExecution stepExecution) {
        logger.info("Annotation Logging: step finished. [{}]", stepExecution.getStepName());
        return null;
    }

    @BeforeChunk
    public void beforeChunk(ChunkContext context) {
        logger.info("Annotation Logging: before chunk.");
    }

    @AfterChunk
    public void afterChunk(ChunkContext context) {
        logger.info("Annotation Logging: after chunk.");
    }

    @AfterChunkError
    public void afterChunkError(ChunkContext context) {
        Exception e = (Exception) context.getAttribute(ChunkListener.ROLLBACK_EXCEPTION_KEY);
        logger.error("Annotation Logging: after chunk error.", e);
    }

    @BeforeRead
    public void beforeRead() {
        logger.info("Annotation Logging: before read.");
    }

    @AfterRead
    public void afterRead(Object item) {
        logger.info("Annotation Logging: after read. [Read item: {}]", item);
    }

    @OnReadError
    public void onReadError(Exception ex) {
        logger.error("Annotation Logging: on read error.", ex);
    }

    @BeforeWrite
    public void beforeWrite(List<?> items) {
        logger.info("Annotation Logging: before write. [Item size :{}]", items.size());
    }

    @AfterWrite
    public void afterWrite(List<?> items) {
        logger.info("Annotation Logging: after write. [Item size :{}]", items.size());
    }

    @OnWriteError
    public void onWriteError(Exception exception, List<?> items) {
        logger.error("Annotation Logging: on write error.", exception);
    }

    @BeforeProcess
    public void beforeProcess(Object item) {
        logger.info("Annotation Logging: before process. [Item :{}]", item);
    }

    @AfterProcess
    public void afterProcess(Object item, Object result) {
        logger.info("Annotation Logging: after process. [Result :{}]", result);
    }

    @OnProcessError
    public void onProcessError(Object item, Exception e) {
        logger.error("Annotation Logging: on process error.", e);
    }

}
