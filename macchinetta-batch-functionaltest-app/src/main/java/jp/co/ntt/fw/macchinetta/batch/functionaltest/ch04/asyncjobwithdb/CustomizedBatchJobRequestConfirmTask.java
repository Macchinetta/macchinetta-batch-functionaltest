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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.asyncjobwithdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Tasks to make sure customized tables and SQL are available. Confirm that the customized SQL has been executed by logging the
 * registration order and execution order set for the job parameters.
 *
 * @since 5.0.0
 */
@Component
public class CustomizedBatchJobRequestConfirmTask implements Tasklet {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(CustomizedBatchJobRequestConfirmTask.class);

    /**
     * logging the registration order and execution order set.
     *
     * @param contribution Step contribution.
     * @param chunkContext Chunk context.
     * @return Repeat status (FINISHED).
     * @throws Exception Exception that occurred.
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        Map<String, Object> params = chunkContext.getStepContext().getJobParameters();
        String registrationOrder = (String) params.get("registrationOrder");
        String executionOrder = (String) params.get("executionOrder");
        String jobExecutionId = chunkContext.getStepContext().getId();

        logger.info("Customized job request execution info [registrationOrder:{}][executionOrder:{}][id:{}].",
                registrationOrder, executionOrder, jobExecutionId);

        return RepeatStatus.FINISHED;
    }

}
