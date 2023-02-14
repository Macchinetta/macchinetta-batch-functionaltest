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
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Tasklet that emulates a long task.
 *
 * @since 2.4.0
 */
@Component
@Scope("step")
public class EmulateLongTaskTasklet implements Tasklet {

    /**
     * Logging.
     */
    private static final Logger logger = LoggerFactory.getLogger(EmulateLongTaskTasklet.class);

    /**
     * Interval.
     */
    private long interval = 500L;

    /**
     * Causes this process to fail.
     */
    private boolean shouldFail = false;

    /**
     * Emulate long processing.
     * <p>
     * Emulate long-time processing by executing sleep processing.
     * </p>
     *
     * @return {@inheritDoc}
     * @throws Exception {@inheritDoc}
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        if (shouldFail) {
            throw new Exception("This exception is caused by 'shouldFail' variable.");
        }

        logger.info("Emulate long processing by sleeping for {} ms.", interval);
        TimeUnit.MILLISECONDS.sleep(interval);

        return RepeatStatus.FINISHED;
    }

    /**
     * Interval.
     *
     * @param interval New interval.
     */
    @Value("#{jobParameters['interval'] ?: 500L}")
    public void setInterval(long interval) {
        this.interval = interval;
    }

    /**
     * Causes this process to fail.
     *
     * @param shouldFail If set to true, this process will fail.
     */
    @Value("#{jobParameters['shouldFail'] ?: false}")
    public void setShouldFail(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }
}
