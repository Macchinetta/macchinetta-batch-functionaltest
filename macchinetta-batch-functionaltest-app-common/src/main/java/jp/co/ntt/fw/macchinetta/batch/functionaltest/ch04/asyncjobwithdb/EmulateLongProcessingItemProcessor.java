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
import org.springframework.batch.item.ItemProcessor;

import java.util.concurrent.TimeUnit;

/**
 * Emulate long processing.
 *
 * @since 2.0.1
 */
public class EmulateLongProcessingItemProcessor implements ItemProcessor<Object, Object> {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(EmulateLongProcessingItemProcessor.class);

    /**
     * Sleeping time (milliseconds).
     */
    private long sleepTime = 500L;

    /**
     * Emulate long processing.
     * <p>
     * Emulate long-time processing by executing sleep processing.
     * </p>
     *
     * @param item {@inheritDoc}
     * @return {@inheritDoc}
     * @throws Exception {@inheritDoc}
     */
    @Override
    public Object process(Object item) throws Exception {

        TimeUnit.MILLISECONDS.sleep(sleepTime);

        logger.info("Long Process emulated.");

        return item;
    }

    /**
     * Sleeping time (milliseconds).
     *
     * @param sleepTime New sleeping time.
     */
    public void setSleepTime(long sleepTime) {
        this.sleepTime = sleepTime;
    }

}
