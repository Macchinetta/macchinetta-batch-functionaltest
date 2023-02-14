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
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.validator.ValidationException;
import org.springframework.batch.item.validator.Validator;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.plan.SalesPlanDetailRepository;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.inputvalidation.model.plan.VerificationSalesPlanDetail;

import jakarta.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Tasklet that perform input check and if an error occurs, continue processing and message and log are output at the end.
 *
 * @since 2.1.0
 */
@Component
@Scope("step")
public class ValidateAndBulkMessageTasklet implements Tasklet {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(ValidateAndBulkMessageTasklet.class);

    @Inject
    ItemStreamReader<VerificationSalesPlanDetail> reader;

    @Inject
    Validator<VerificationSalesPlanDetail> validator;

    @Inject
    SalesPlanDetailRepository repository;

    @Inject
    MessageSource messageSource;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        logger.info("Start ValidateAndBulkMessageTasklet.");

        List<String> errorMessageList = new ArrayList<>();

        try {
            reader.open(chunkContext.getStepContext().getStepExecution().getExecutionContext());

            VerificationSalesPlanDetail item;
            while ((item = reader.read()) != null) {
                try {
                    validator.validate(item);
                } catch (ValidationException e) {

                    BindException errors = (BindException) e.getCause();

                    for (FieldError fieldError : errors.getFieldErrors()) {
                        errorMessageList.add(messageSource.getMessage(fieldError, null) + MessageFormat
                                .format(" Skipping item because exception occurred in input validation at the {0} th item. [message:{1}]",
                                        item.getCount(), e.getMessage()));
                    }

                    continue; // skipping item
                }

                logger.info("Processing on ValidateAndBulkMessageTasklet.");

                repository.create(convert(item));
            }
        } finally {
            reader.close();
        }

        for (String errorMessage : errorMessageList) {
            logger.warn(errorMessage);
        }

        logger.info("Finish ValidateAndBulkMessageTasklet.");
        return RepeatStatus.FINISHED;
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
