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

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Customer;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.mst.CustomerRepository;

import javax.inject.Inject;

/**
 * Processor that update item from cache value.
 *
 * @since 5.0.0
 */
@Component
public class UpdateItemFromCacheProcessor implements ItemProcessor<SalesPerformanceDetail, SalesPlanDetail> {

    /**
     * Customer repository.
     */
    @Inject
    CustomerRepository customerRepository;

    /**
     * Customer cache
     */
    @Inject
    CustomerCache cache;

    /**
     * Item is updated by acquired value from cache.
     *
     * @param readItem that passed from reader.
     * @return writeItem that branch id is changed.
     * @throws Exception Exception that occurred.
     */
    @Override
    public SalesPlanDetail process(SalesPerformanceDetail readItem) throws Exception {
        Customer customer = cache.getCustomer(readItem.getCustomerId());

        SalesPlanDetail writeItem = new SalesPlanDetail();
        writeItem.setBranchId(customer.getChargeBranchId());
        writeItem.setYear(readItem.getYear());
        writeItem.setMonth(readItem.getMonth());
        writeItem.setCustomerId(readItem.getCustomerId());
        writeItem.setAmount(readItem.getAmount());
        return writeItem;
    }
}
