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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.stereotype.Component;

/**
 * ItemReader listener for logging read data and errors.
 *
 * @since 5.0.0
 */
@Component
public class LoggingItemReaderListener implements ItemReadListener<Object> {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(LoggingItemReaderListener.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeRead() {
        // no operation
    }

    /**
     * {@inheritDoc}
     *
     * @param item A read item.
     */
    @Override
    public void afterRead(Object item) {
        logger.info("Read item: " + item.toString());
    }

    /**
     * {@inheritDoc}
     *
     * @param ex An exception on reading data.
     */
    @Override
    public void onReadError(Exception ex) {
        logger.error("Exception occurred while reading.", ex);
    }
}
