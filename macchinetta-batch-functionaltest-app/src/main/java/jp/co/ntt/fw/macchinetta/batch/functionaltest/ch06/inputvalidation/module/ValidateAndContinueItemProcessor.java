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
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.validator.ValidationException;
import org.springframework.batch.item.validator.Validator;
import org.springframework.stereotype.Component;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.inputvalidation.model.plan.VerificationSalesPlanDetail;

import javax.inject.Inject;

/**
 * Item processror that perform input check and if an error occurs, log output and continue processing.
 *
 * @since 5.0.0
 */
@Component
public class ValidateAndContinueItemProcessor implements ItemProcessor<VerificationSalesPlanDetail, SalesPlanDetail> {
    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(ValidateAndContinueItemProcessor.class);

    @Inject
    Validator<VerificationSalesPlanDetail> validator;

    @Override
    public SalesPlanDetail process(VerificationSalesPlanDetail item) throws Exception {
        try {
            validator.validate(item);
        } catch (ValidationException e) {
            logger.warn("Skipping item because exception occurred in input validation at the {} th item. [message:{}]",
                    item.getCount(), e.getMessage());
            return null; // skipping item
        }

        return convert(item);
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
