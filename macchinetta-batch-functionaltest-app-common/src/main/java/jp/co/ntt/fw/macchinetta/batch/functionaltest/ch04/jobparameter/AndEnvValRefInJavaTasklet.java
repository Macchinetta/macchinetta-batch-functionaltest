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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.jobparameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Tasklet to output the field to the log
 *
 * @since 2.0.1
 */
@Component
@Scope("step")
public class AndEnvValRefInJavaTasklet implements Tasklet {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(AndEnvValRefInJavaTasklet.class);

    /**
     * Holds a String type value
     */
    @Value("${str}")
    private String str;

    /**
     * Holds a int type value
     */
    @Value("#{jobParameters['num']}")
    private int num;

    /**
     * {@inheritDoc}
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        logger.info("str = {}, num = {}", str, num);
        return null;
    }
}
