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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.dbaccess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Customer;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.mst.CustomerRepository;

import javax.inject.Inject;

/**
 * Processor that update item by acquired value from database.
 *
 * @since 2.0.1
 */
@Component
public class UpdateItemFromDBProcessor implements ItemProcessor<SalesPerformanceDetail, SalesPlanDetail> {

    /**
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(UpdateItemFromDBProcessor.class);

    /**
     * Customer repository.
     */
    @Inject
    CustomerRepository customerRepository;

    /**
     * Item is updated by acquired value from database.
     *
     * @param readItem that passed from reader.
     * @return writeItem that branch id is changed.
     * @throws Exception Exception that occurred.
     */
    @Override
    public SalesPlanDetail process(SalesPerformanceDetail readItem) throws Exception {
        logger.info("UpdateItemFromDBProcessor is called.");
        Customer customer = customerRepository.findOne(readItem.getCustomerId());

        SalesPlanDetail writeItem = new SalesPlanDetail();
        writeItem.setBranchId(customer.getChargeBranchId());
        writeItem.setYear(readItem.getYear());
        writeItem.setMonth(readItem.getMonth());
        writeItem.setCustomerId(readItem.getCustomerId());
        writeItem.setAmount(readItem.getAmount());
        return writeItem;
    }
}
