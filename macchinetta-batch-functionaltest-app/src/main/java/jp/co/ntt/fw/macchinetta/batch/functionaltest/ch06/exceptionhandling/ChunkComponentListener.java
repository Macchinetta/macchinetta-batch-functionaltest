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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.exceptionhandling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.ItemListenerSupport;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Logging listener for ItemReader / ItemProcessor / ItemWriter.
 *
 * @since 5.0.0
 */
@Component
public class ChunkComponentListener extends ItemListenerSupport {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(ChunkComponentListener.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeRead() {
        // no operation
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterRead(Object item) {
        logger.info("Read item: " + item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReadError(Exception ex) {
        logger.error("Exception occurred while reading.", ex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeProcess(Object item) {
        logger.info("before process. [item:{}]", item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterProcess(Object item, Object result) {
        logger.info("after process. [item:{}][result:{}]", item, result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onProcessError(Object item, Exception e) {
        logger.error("Exception occurred while processing. [item:{}]", item, e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeWrite(List item) {
        logger.info("before write. [item:{}]", item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterWrite(List item) {
        logger.info("after write. [item:{}]", item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onWriteError(Exception ex, List item) {
        logger.error("Exception occurred while processing. [items:{}]", item, ex);
    }
}
