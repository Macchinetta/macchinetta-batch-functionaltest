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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * Item processor to check amount.
 * <p>
 * Check the amount, skip the data processing in case of an error.
 * </p>
 *
 * @since 2.0.1
 */
@Component
@Scope("step")
public class CheckAmountItemProcessor implements ItemProcessor<SalesPlanSummary, SalesPlanSummary>,
                                     StepExecutionListener {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(CheckAmountItemProcessor.class);

    /**
     * Error items.
     */
    private List<SalesPlanSummary> errorItems = new ArrayList<>();

    /**
     * Check the amount, skip the data processing in case of an error.
     *
     * @param item Item to be checked.
     * @return In the case of a check error, it is null, otherwise the entered item.
     * @throws Exception {@inheritDoc}
     */
    @Override
    public SalesPlanSummary process(SalesPlanSummary item) throws Exception {

        if (item.getAmount().signum() == -1) {
            logger.warn("amount is negative. skip item [item: {}]", item);
            errorItems.add(item);
            return null;
        }

        return item;
    }

    /**
     * Before step.
     * <p>
     * Clear error item list.
     * </p>
     *
     * @param stepExecution Step execution.
     */
    @Override
    public void beforeStep(StepExecution stepExecution) {
        errorItems.clear();
    }

    /**
     * After step.
     * <p>
     * If there is error data even for one case, return "STEP COMPLETED WITH SKIPS" as ExitStatus. Otherwise, return null.
     * </p>
     *
     * @param stepExecution Step execution.
     * @return
     */
    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (errorItems.size() > 0) {
            logger.info("Change status 'STEP COMPLETED WITH SKIPS'");
            return new ExitStatus("STEP COMPLETED WITH SKIPS");
        }
        return null;
    }
}
