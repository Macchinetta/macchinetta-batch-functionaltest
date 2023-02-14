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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.component;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Customer;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.repository.admin.BranchRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.model.CustomerWithBranch;

import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Attendant data is acquired every time by ItemProcessor.
 *
 * @since 2.0.1
 */
@Component
@Scope("step")
public class RetrieveBranchFromRepositoryItemProcessor implements ItemProcessor<Customer, CustomerWithBranch> {

    /**
     * Repository of Branch master.
     */
    @Inject
    @Named("adminBranchRepository")
    BranchRepository branchRepository;

    /**
     * Attendant data is acquired every time by ItemProcessor.
     *
     * @param item Customer form database.
     * @return Customer with branch.
     * @throws Exception {@inheritDoc}
     */
    @Override
    public CustomerWithBranch process(Customer item) throws Exception {
        CustomerWithBranch newItem = new CustomerWithBranch(item);
        newItem.setBranch(branchRepository.findOne(item.getChargeBranchId()));
        return newItem;
    }
}
