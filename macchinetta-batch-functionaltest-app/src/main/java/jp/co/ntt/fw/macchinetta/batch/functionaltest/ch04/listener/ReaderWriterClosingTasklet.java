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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * Tasklet to close reader and writer.
 *
 * @since 5.1.0
 */
@Component
@Scope("step")
public class ReaderWriterClosingTasklet implements Tasklet {

    /**
     * Reader.
     */
    @Inject
    LoggingReader reader;

    /**
     * Writer.
     */
    @Inject
    LoggingWriter writer;

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(ReaderWriterClosingTasklet.class);

    /**
     * Close reader and writer.
     *
     * @param contribution Step contribution.
     * @param chunkContext Chunk context.
     * @return RepeatStatus.FINISHED
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        reader.close();
        writer.close();

        return RepeatStatus.FINISHED;
    }
}
