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

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.mst.CustomerRepository;

import javax.inject.Inject;

/**
 * Listener that set customer data to cache from database.
 *
 * @since 2.0.1.
 */
public class CacheSetListener extends StepExecutionListenerSupport {

    /**
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(CacheSetListener.class);

    /**
     * Customer repository
     */
    @Inject
    CustomerRepository customerRepository;

    /**
     * Customer cache
     */
    @Inject
    CustomerCache cache;

    /**
     * Set customer data that branch id is "001" to cache.
     * 
     * @param stepExecution
     */
    @Override
    public void beforeStep(StepExecution stepExecution) {
        logger.info("CacheSetListener is called at before step.");
        for(Customer customer : customerRepository.findAllAtOnce()) {
            cache.addCustomer(customer.getCustomerId(), customer);
        }
    }
}
