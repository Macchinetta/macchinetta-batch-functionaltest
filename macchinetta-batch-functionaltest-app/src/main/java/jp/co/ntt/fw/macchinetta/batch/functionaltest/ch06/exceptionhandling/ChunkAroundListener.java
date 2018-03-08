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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.exceptionhandling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

/**
 * Logging listener for Chunk.
 *
 * @since 5.0.0
 */
@Component
public class ChunkAroundListener implements ChunkListener {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(ChunkAroundListener.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeChunk(ChunkContext context) {
        logger.info("before chunk. [context:{}]", context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterChunk(ChunkContext context) {
        logger.info("after chunk. [context:{}]", context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterChunkError(ChunkContext context) {
        logger.error("Exception occurred while chunk. [context:{}]", context, context
                .getAttribute(ChunkListener.ROLLBACK_EXCEPTION_KEY));
    }
}
