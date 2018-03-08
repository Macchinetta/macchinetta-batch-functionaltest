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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.dbaccess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Customer;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.dbaccess.repository.DBAccessCustomerRepository;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Clock;

/**
 * Update customer info processor.
 *
 * @since 5.0.0
 */
@Component
public class UpdateCustomerItemProcessor implements ItemProcessor<Customer, Customer> {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(UpdateCustomerItemProcessor.class);

    /**
     * Clock.
     */
    private Clock clock = Clock.systemDefaultZone();

    /**
     * Customer repository.
     */
    @Inject
    DBAccessCustomerRepository customerRepository;

    /**
     * Update customer info.
     * <p>
     * The customer name is updated with the Mapper interface, and the customer address is updated with ItemWriter
     * </p>
     *
     * @param item Customer
     * @return Updated customer.
     * @throws Exception Exception that occurred.
     */
    @Override
    public Customer process(Customer item) throws Exception {
        item.setCustomerName(String.format("%s updated by mapper if", item.getCustomerName()));
        item.setCustomerAddress(String.format("%s updated by item writer", item.getCustomerAddress()));
        item.setUpdateDate(new Timestamp(clock.millis()));

        long cnt = customerRepository.updateName(item);

        logger.info("item updated. [count:{}]", cnt);

        return item;
    }
}
