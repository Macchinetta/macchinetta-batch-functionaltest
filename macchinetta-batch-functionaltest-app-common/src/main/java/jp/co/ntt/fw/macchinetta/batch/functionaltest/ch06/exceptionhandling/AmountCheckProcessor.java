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
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * A item value checking only.
 */
@Component
@Scope("step")
public class AmountCheckProcessor implements ItemProcessor<SalesPerformanceDetail, SalesPerformanceDetail> {

    private static final Logger logger = LoggerFactory.getLogger(AmountCheckProcessor.class);

    /**
     * limit for amount, it is specified config.
     */
    BigDecimal amountLimit;

    /**
     * {@link StepExecution} for notifying incorrect item.
     */
    private StepExecution stepExecution;

    /**
     * only set stepExecution instance.
     * 
     * @param stepExecution stepExecution
     */
    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

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
            stepExecution.getExecutionContext().put("ERROR_ITEM", item);
            throw new IllegalStateException("check error at processor.", ae);
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
