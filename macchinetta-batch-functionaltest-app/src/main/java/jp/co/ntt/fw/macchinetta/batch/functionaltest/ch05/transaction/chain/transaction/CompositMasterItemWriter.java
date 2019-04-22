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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.chain.transaction;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.model.CompositeMaster;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.repository.admin.BranchRepository;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.repository.job.CustomerRepository;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * Item writer for master composite model.
 *
 * @since 5.0.0
 */
@Component
public class CompositMasterItemWriter implements ItemWriter<CompositeMaster> {

    /**
     * Repository of branch for admin.
     */
    @Inject
    @Named("adminBranchRepository")
    BranchRepository branchRepository;

    /**
     * Repository of customer for job.
     */
    @Inject
    @Named("jobCustomerRepository")
    CustomerRepository customerRepository;

    /**
     * Register each master of the composite model of the master in the database.
     *
     * @param items master composite model
     * @throws Exception Exception that occurred
     */
    @Override
    public void write(List<? extends CompositeMaster> items) throws Exception {

        for (CompositeMaster master : items) {
            branchRepository.create(master.getBranch());
            customerRepository.create(master.getCustomer());
        }
    }
}
