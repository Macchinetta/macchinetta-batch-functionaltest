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
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
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
public class CheckAmountItemProcessor implements ItemProcessor<SalesPlanSummary, SalesPlanSummary> {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(CheckAmountItemProcessor.class);

    /**
     * Key string of errorItems.
     */
    private static final String ERROR_ITEMS_KEY = "errorItems";

    /**
     * StepExecution.
     */
    private StepExecution stepExecution;

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

            if (!stepExecution.getExecutionContext().containsKey(ERROR_ITEMS_KEY)) {
                stepExecution.getExecutionContext().put(ERROR_ITEMS_KEY, new ArrayList<SalesPlanSummary>());
            }
            @SuppressWarnings("unchecked")
            List<SalesPlanSummary> errorItems = (List<SalesPlanSummary>) stepExecution.getExecutionContext().get(ERROR_ITEMS_KEY);
            errorItems.add(item);

            return null;
        }
        return item;
    }

    /**
     * Before step.
     *
     * @param stepExecution Step execution.
     */
    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

    /**
     * After step.
     * <p>
     * If there is error data even for one case, return "STEP COMPLETED WITH SKIPS" as ExitStatus. Otherwise, return null.
     * </p>
     *
     * @param stepExecution Step execution.
     * @return Exit status.
     */
    @AfterStep
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.getExecutionContext().containsKey(ERROR_ITEMS_KEY)) {
            logger.info("Change status 'STEP COMPLETED WITH SKIPS'");
            return new ExitStatus("STEP COMPLETED WITH SKIPS");
        }
        return stepExecution.getExitStatus();
    }
}
