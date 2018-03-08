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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.dbaccess.repository;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Customer;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Repository of customer master.
 *
 * @since 5.0.0
 */
public interface DBAccessCustomerRepository {
    /**
     * Find one customer master data.
     *
     * @param customerId Customer id.
     * @return A Customer master data.
     */
    Customer findOne(@Param("customerId") String customerId);

    /**
     * Find All customer master data.
     *
     * @return All Customer master data.
     */
    List<Customer> findAll();

    /**
     * Find customer master data by branch.
     *
     * @param branchId Branch id.
     * @return Customer master data by branch.
     */
    List<Customer> findByBranch(@Param("branchId") String branchId);

    /**
     * Update customer name.
     *
     * @param customer Update customer.
     * @return Updated record count.
     */
    long updateName(Customer customer);

    /**
     * Update customer address.
     *
     * @param customer Update customer.
     * @return Updated record count.
     */
    long updateAddress(Customer customer);
}
