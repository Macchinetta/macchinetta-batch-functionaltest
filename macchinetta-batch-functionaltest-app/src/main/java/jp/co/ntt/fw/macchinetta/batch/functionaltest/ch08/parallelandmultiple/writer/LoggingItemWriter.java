/*
 * Copyright (C) 2020 NTT Corporation
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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Logging item writer.
 *
 * @since 2.2.0
 */
@Component("loggingItemWriter")
@Scope("step")
public class LoggingItemWriter implements ItemWriter<Object> {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(LoggingItemWriter.class);

    /**
     * Write current thread and item to log.
     *
     * @param items items to be written
     * @throws Exception if there are errors. The framework will catch the
     * exception and convert or rethrow it as appropriate.
     */
    @Override
    public void write(List<? extends Object> items) throws Exception {

        String currentThreadName =  Thread.currentThread().getName();

        for (Object item : items) {
            logger.debug(String.format("Item Writer [%s] %s", currentThreadName, item));
        }

    }

}
