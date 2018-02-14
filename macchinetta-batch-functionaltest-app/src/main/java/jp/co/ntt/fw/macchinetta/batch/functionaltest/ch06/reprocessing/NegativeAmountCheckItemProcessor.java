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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.reprocessing;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Check if it is a negative amount.
 *
 * @since 5.0.0
 */
@Component
public class NegativeAmountCheckItemProcessor implements ItemProcessor<SalesPlanDetail, SalesPlanDetail> {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(NegativeAmountCheckItemProcessor.class);

    /**
     * Check if it is a negative amount.
     *
     * @param item Check target.
     * @return Item that passed check.
     * @throws Exception When it is a negative amount.
     */
    @Override
    public SalesPlanDetail process(SalesPlanDetail item) throws Exception {

        logger.info("Checking item is [{}]", item);

        if (item.getAmount().signum() == -1) {
            throw new IllegalStateException("amount is negative.");
        }

        return item;
    }
}
