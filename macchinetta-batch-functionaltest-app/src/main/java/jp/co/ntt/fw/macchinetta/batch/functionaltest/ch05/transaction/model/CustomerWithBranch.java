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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.model;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Branch;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Customer;

/**
 * Customer model with branch information.
 *
 * @since 5.0.0
 */
public class CustomerWithBranch extends Customer {

    /**
     * Constructor.
     *
     * @param customer Customer.
     */
    public CustomerWithBranch(Customer customer) {
        this.setCustomerId(customer.getCustomerId());
        this.setCustomerName(customer.getCustomerName());
        this.setCustomerAddress(customer.getCustomerAddress());
        this.setCustomerTel(customer.getCustomerTel());
        this.setChargeBranchId(customer.getChargeBranchId());
        this.setCreateDate(customer.getCreateDate());
        this.setUpdateDate(customer.getUpdateDate());
    }

    /**
     * Information of the branch in charge of the customer.
     */
    private Branch branch;

    /**
     * Information of the branch in charge of the customer..
     *
     * @return The current of branch.
     */
    public Branch getBranch() {
        return branch;
    }

    /**
     * Information of the branch in charge of the customer..
     *
     * @param branch New branch.
     */
    public void setBranch(Branch branch) {
        this.branch = branch;
    }
}
