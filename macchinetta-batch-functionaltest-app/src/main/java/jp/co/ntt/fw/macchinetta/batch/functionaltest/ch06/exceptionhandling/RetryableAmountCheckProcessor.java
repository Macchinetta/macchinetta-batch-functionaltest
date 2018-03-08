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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.exceptionhandling;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.math.BigDecimal;

/**
 * A item value checking only with retry.
 */
public class RetryableAmountCheckProcessor implements ItemProcessor<SalesPerformanceDetail, SalesPerformanceDetail> {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(RetryableAmountCheckProcessor.class);

    /**
     * limit for amount, it is specified config.
     */
    private BigDecimal amountLimit;

    /**
     * Correct the value at the specified times. it's retry mocking.
     */
    private int adjustTimes;

    /**
     * RetryPolicy
     */
    private RetryPolicy retryPolicy;

    /**
     * check item only.
     *
     * @param item {@link SalesPerformanceDetail}
     * @return same input item
     * @throws Exception {@inheritDoc}
     */
    @Override
    public SalesPerformanceDetail process(SalesPerformanceDetail item) throws Exception {

        // default, retry to 3 times
        RetryTemplate rt = new RetryTemplate();
        if (retryPolicy != null) {
            rt.setRetryPolicy(retryPolicy);
        }

        try {
            rt.execute(new RetryCallback<SalesPerformanceDetail, Exception>() {
                @Override
                public SalesPerformanceDetail doWithRetry(RetryContext context) throws Exception {
                    logger.info("execute with retry. [retry-count:{}]", context.getRetryCount());
                    // retry mocking
                    if (context.getRetryCount() == adjustTimes) {
                        item.setAmount(item.getAmount().divide(new BigDecimal(10)));
                    }

                    checkAmount(item.getAmount(), amountLimit);
                    return null;
                }
            });
        } catch (ArithmeticException ae) {
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
     * set adjust times
     *
     * @param adjustTimes adjust value times
     */
    public void setAdjustTimes(int adjustTimes) {
        this.adjustTimes = adjustTimes;
    }

    /**
     * set RetryPolicy
     *
     * @param retryPolicy RetryPolicy
     */
    public void setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    /**
     * do check.
     *
     * @param target check target
     * @param limit  check limit
     */
    private void checkAmount(BigDecimal target, BigDecimal limit) {
        if (target.compareTo(limit) > 0) {
            throw new ArithmeticException("must be less than " + amountLimit);
        }
    }
}
