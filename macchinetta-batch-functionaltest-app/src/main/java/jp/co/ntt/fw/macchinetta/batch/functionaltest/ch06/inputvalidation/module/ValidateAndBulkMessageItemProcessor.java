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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.inputvalidation.module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.validator.ValidationException;
import org.springframework.batch.item.validator.Validator;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.inputvalidation.model.plan.VerificationSalesPlanDetail;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Item processor that perform input check and if an error occurs, continue processing and message and log are output at the end.
 *
 * @since 2.1.0
 */
@Component
@Scope("step")
public class ValidateAndBulkMessageItemProcessor implements ItemProcessor<VerificationSalesPlanDetail, SalesPlanDetail> {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(ValidateAndBulkMessageItemProcessor.class);

    private static final String ERROR_MESSAGE_KEY = "errorMessageList";

    private StepExecution stepExecution;

    @Inject
    Validator<VerificationSalesPlanDetail> validator;

    @Inject
    MessageSource messageSource;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

    @Override
    public SalesPlanDetail process(VerificationSalesPlanDetail item) throws Exception {
        try {
            validator.validate(item);
        } catch (ValidationException e) {

            List<String> errorMessageList = new ArrayList<>();
            if (stepExecution.getExecutionContext().containsKey(ERROR_MESSAGE_KEY)) {
                errorMessageList = (List<String>) stepExecution.getExecutionContext().get(ERROR_MESSAGE_KEY);
                stepExecution.getExecutionContext().remove(ERROR_MESSAGE_KEY);
            }

            BindException errors = (BindException) e.getCause();

            for (FieldError fieldError : errors.getFieldErrors()) {
                String message = MessageFormat
                        .format("{0} Skipping item because exception occurred in input validation at the {1}. [message:{2}]",
                                messageSource.getMessage(fieldError, null), item.getCount() + " th item", e.getMessage());

                errorMessageList.add(message);
            }

            stepExecution.getExecutionContext().put(ERROR_MESSAGE_KEY, errorMessageList);

            return null; // skipping item
        }

        logger.info("Processing on ValidateAndBulkMessageItemProcessor.");

        return convert(item);
    }

    @AfterStep
    public void afterStep(StepExecution stepExecution) {
        ExecutionContext executionContext = stepExecution.getExecutionContext();

        List<String> errorMessageList = (List<String>) executionContext.get(ERROR_MESSAGE_KEY);
        for (String errorMessage : errorMessageList) {
            logger.warn(errorMessage);
        }
    }

    private SalesPlanDetail convert(VerificationSalesPlanDetail item) {
        SalesPlanDetail salesPlanDetail = new SalesPlanDetail();
        salesPlanDetail.setBranchId(item.getBranchId());
        salesPlanDetail.setYear(item.getYear() + 1000); // edit item
        salesPlanDetail.setMonth(item.getMonth());
        salesPlanDetail.setCustomerId(item.getCustomerId());
        salesPlanDetail.setAmount(item.getAmount());
        return salesPlanDetail;
    }
}
