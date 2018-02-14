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

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

/**
 * Logging listener for SkipData.
 *
 * @since 5.0.0
 */
@Component
public class SkipLoggingListener implements SkipListener<SalesPerformanceDetail, SalesPerformanceDetail> {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(SkipLoggingListener.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSkipInRead(Throwable t) {
        logger.warn("skipped a record in read.", t);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSkipInWrite(SalesPerformanceDetail item, Throwable t) {
        logger.warn("skipped a record in write. [item:{}]", item, t);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSkipInProcess(SalesPerformanceDetail item, Throwable t) {
        logger.warn("skipped a record in processor. [item:{}]", item, t);
    }

}
