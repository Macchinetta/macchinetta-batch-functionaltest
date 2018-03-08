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
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Reader to get log.
 *
 * @since 5.1.0
 */
@Component
public class LoggingReader implements ItemStreamReader {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(LoggingReader.class);

    /**
     * Resource.
     */
    private Resource resource;

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        logger.info("open method is called.");
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        // do nothing.
    }

    @Override
    public void close() throws ItemStreamException {
        logger.info("close method is called.");
    }

    @Override
    public Object read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        // do nothing.
        return null;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }
}
