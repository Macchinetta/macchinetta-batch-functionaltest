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

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Customer;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Branch;

/**
 * Composite model of master.
 *
 * @since 2.0.1
 */
public class CompositeMaster {

    /**
     * Branch master.
     */
    private Branch branch;

    /**
     * Customer master.
     */
    private Customer customer;

    /**
     * Branch master.
     *
     * @return The current of branch.
     */
    public Branch getBranch() {
        return branch;
    }

    /**
     * Branch master.
     *
     * @param branch New branch.
     */
    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    /**
     * Customer master.
     *
     * @return The current of customer.
     */
    public Customer getCustomer() {
        return customer;
    }

    /**
     * Customer master.
     *
     * @param customer New customer.
     */
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    /**
     * Represent a property of a bean.
     *
     * @return String of represented a property of a bean.
     */
    @Override
    public String toString() {
        return "CompositeMaster{" + "branch=" + branch + ", customer=" + customer + '}';
    }
}
