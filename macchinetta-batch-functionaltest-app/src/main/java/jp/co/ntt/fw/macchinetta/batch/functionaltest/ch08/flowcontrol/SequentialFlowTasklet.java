/*
 * Copyright (C) 2018 NTT Corporation
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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.flowcontrol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * This task is for observing sequential flow on multi-steps.
 */
public class SequentialFlowTasklet implements Tasklet {

    private static final Logger logger = LoggerFactory.getLogger(SequentialFlowTasklet.class);

    private String failExecutionStep;

    /**
     * This step will be failed when StepContext has JobParameter named "failExecutionStep" and has value of same step name.
     * Another will be finished normally.
     *
     * @param contribution step contribution
     * @param chunkContext chunk context
     * @return repeat status (FINISHED permanently)
     * @throws Exception chunkContext has JobParameter named "flowExecution" and value "fail".
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        String stepName = chunkContext.getStepContext().getStepName();
        String failSkip = System.getProperty("failSkip");
        if (failSkip == null && failExecutionStep != null && failExecutionStep.contains(stepName)) {
            throw new IllegalArgumentException("This step will be failure [stepName=" + stepName
                    + ", failExecutionStep=" + failExecutionStep + "].");
        }
        logger.info("This step will be succeeded [stepName={}, failExecutionStep={}].", stepName, failExecutionStep);
        return RepeatStatus.FINISHED;
    }

    public void setFailExecutionStep(String failExecutionStep) {
        this.failExecutionStep = failExecutionStep;
    }
}
