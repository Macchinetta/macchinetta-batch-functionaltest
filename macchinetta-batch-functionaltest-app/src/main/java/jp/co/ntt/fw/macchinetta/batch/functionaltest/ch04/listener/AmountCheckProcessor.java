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
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;

/**
 * ItemProcessor to check amount.
 *
 * @since 5.0.0
 */
@Component("amountCheckProcessor")
public class AmountCheckProcessor implements ItemProcessor<SalesPlanDetail, SalesPlanDetail> {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(AmountCheckProcessor.class);

    /**
     * Check that the amount is 0 or more.
     *
     * @param item Item to be checked.
     * @return Item passed the check.
     * @throws Exception {@link IllegalArgumentException} occurs if the check fails.
     */
    @Override
    public SalesPlanDetail process(SalesPlanDetail item) throws Exception {
        if (item.getAmount().signum() == -1) {
            throw new IllegalArgumentException("amount is negative.");
        }

        return item;
    }

}
