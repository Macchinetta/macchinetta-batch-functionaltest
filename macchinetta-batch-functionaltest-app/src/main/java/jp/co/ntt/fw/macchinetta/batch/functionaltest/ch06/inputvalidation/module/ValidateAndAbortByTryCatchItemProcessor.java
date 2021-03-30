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
 * Item processor that perform input check and if an error occurs, throw an input check exception and abort the process.
 *
 * @since 2.0.1
 */
@Component
public class ValidateAndAbortByTryCatchItemProcessor implements ItemProcessor<VerificationSalesPlanDetail, SalesPlanDetail> {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(ValidateAndAbortByTryCatchItemProcessor.class);

    @Inject
    Validator<VerificationSalesPlanDetail> validator;

    @Override
    public SalesPlanDetail process(VerificationSalesPlanDetail item) throws Exception {
        try{
            validator.validate(item);
            return convert(item);
        }catch (ValidationException e){
            logger.error("Exception occurred in input validation at the {} th item. [message:{}]",
                    item.getCount(), e.getMessage());
            throw e;
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
