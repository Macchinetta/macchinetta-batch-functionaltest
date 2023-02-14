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

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * ItemProcessor that emulates a long task.
 *
 * @since 2.0.1
 */
@Component
@Scope("step")
public class EmulateLongTaskItemProcessor implements ItemProcessor<SalesPlanSummary, SalesPlanSummary> {

    /**
     * Logging.
     */
    private static final Logger logger = LoggerFactory.getLogger(EmulateLongTaskItemProcessor.class);

    /**
     * Interval.
     */
    private long interval = 1000L;

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
    public SalesPlanSummary process(SalesPlanSummary item) throws Exception {

        logger.info("Emulate long processing by sleeping for {} ms. [item: {}]", interval, item);

        TimeUnit.MILLISECONDS.sleep(interval);

        return item;
    }

    /**
     * Interval.
     *
     * @param interval New interval.
     */
    @Value("#{jobParameters['interval'] ?: 500L}")
    public void setInterval(long interval) {
        this.interval = interval;
    }

}
