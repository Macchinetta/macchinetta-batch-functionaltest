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
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * A item value checking and error items skip.
 */
@Component
public class AmountCheckWithSkipProcessor implements ItemProcessor<SalesPerformanceDetail, SalesPerformanceDetail> {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(AmountCheckWithSkipProcessor.class);

    /**
     * limit for amount, it is specified config.
     */
    BigDecimal amountLimit;

    /**
     * check item only.
     *
     * @param item {@link SalesPerformanceDetail}
     * @return same input item
     * @throws Exception {@inheritDoc}
     */
    @Override
    public SalesPerformanceDetail process(SalesPerformanceDetail item) throws Exception {

        logger.debug("processing. {}", item);

        try {
            checkAmount(item.getAmount(), amountLimit);
        } catch (ArithmeticException ae) {
            logger.warn("skipped a record in processor. [item:{}]", item, ae);
            return null;
        }
        return item;
    }

    /**
     * Setter for amount-limit
     *
     * @param amountLimit amount limit value.
     */
    public void setAmountLimit(BigDecimal amountLimit) {
        this.amountLimit = amountLimit;
    }

    /**
     * do check.
     *
     * @param target check target
     * @param limit check limit
     */
    private void checkAmount(BigDecimal target, BigDecimal limit) {
        if (target.compareTo(limit) > 0) {
            throw new ArithmeticException("must be less than " + amountLimit);
        }
    }
}
