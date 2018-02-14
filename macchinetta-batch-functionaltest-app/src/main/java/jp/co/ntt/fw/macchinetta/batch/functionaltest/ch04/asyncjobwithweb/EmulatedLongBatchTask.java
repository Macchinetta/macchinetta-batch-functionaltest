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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.asyncjobwithweb;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.BooleanLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * This tasklet is enable to observe job situations of executing, ended, and exhaust threads.
 */
public class EmulatedLongBatchTask implements Tasklet {

    private static final Logger logger = LoggerFactory.getLogger(EmulatedLongBatchTask.class);

    private final BooleanLatch booleanLatch = BooleanLatch.getInstance();

    /**
     * To observe JOB and STEP executions, await this thread till signalled by client request.
     *
     * @param contribution step contribution
     * @param chunkContext context
     * @return always FINISHED.
     * @throws Exception unexpected exception
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        logger.info("start tasklet. [jobExecutionId={}]", chunkContext.getStepContext().getStepExecution()
                .getJobExecutionId());
        booleanLatch.await();
        return RepeatStatus.FINISHED;
    }
}
