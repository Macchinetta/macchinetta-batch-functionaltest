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
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import java.util.Locale;

/**
 * Tasklet to get message.
 *
 * @since 2.0.1
 */
@Component
@Scope("step")
public class MessageGettingTasklet implements Tasklet{

    @Inject
    MessageSource messageSource;

    private static final Logger logger = LoggerFactory.getLogger(MessageGettingTasklet.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        logger.info(messageSource
                .getMessage("i.it.ch07", new String[] {"Test Message"}, Locale.getDefault()));

        return RepeatStatus.FINISHED;
    }

}
