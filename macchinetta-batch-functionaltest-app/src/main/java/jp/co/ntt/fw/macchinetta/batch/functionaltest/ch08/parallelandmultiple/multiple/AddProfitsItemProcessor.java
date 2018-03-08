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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.multiple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceSummary;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanSummary;

import java.math.BigDecimal;

/**
 * Item processor to add profits.
 *
 * @Since 5.0.0
 */
@Component
public class AddProfitsItemProcessor implements ItemProcessor<SalesPerformanceSummary, SalesPlanSummary> {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(AddProfitsItemProcessor.class);

    /**
     * Profit.
     */
    private BigDecimal profits = new BigDecimal("500");

    /**
     * Add profits to the actual amount and make it the planned amount of the following year.
     *
     * @param item Sales performance.
     * @return Sales plan of the following year.
     * @throws Exception
     */
    @Override
    public SalesPlanSummary process(SalesPerformanceSummary item) throws Exception {

        logger.info("Sales performance. {}", item);

        SalesPlanSummary planSummary = new SalesPlanSummary();
        planSummary.setBranchId(item.getBranchId());
        planSummary.setYear(item.getYear() + 1);
        planSummary.setMonth(item.getMonth());
        planSummary.setAmount(profits.add(item.getAmount())); // Make a certain profit for simplicity

        logger.info("Sales plan with added profit. {}", planSummary);

        return planSummary;
    }
}
