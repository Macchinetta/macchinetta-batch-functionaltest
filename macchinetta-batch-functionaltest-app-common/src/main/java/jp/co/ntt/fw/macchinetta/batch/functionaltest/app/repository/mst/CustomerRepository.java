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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.mst;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.cursor.Cursor;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Customer;

import java.util.List;

/**
 * Repository of customer master.
 *
 * @since 2.0.1
 */
public interface CustomerRepository {
    /**
     * Find one customer master data.
     *
     * @param customerId Customer id.
     * @return A Customer master data.
     */
    Customer findOne(@Param("customerId") String customerId);

    /**
     * Get cursor for customer master data.
     *
     * @return Cursor for customer master data.
     */
    Cursor<Customer> findAll();

    /**
     * Find all customer master data.
     *
     * @return All Customer master data.
     */
    List<Customer> findAllAtOnce();

    /**
     * Get cursor for customer master data by branch.
     *
     * @param branchId Branch id.
     * @return Cursor for customer master data by branch.
     */
    Cursor<Customer> findByBranch(@Param("branchId") String branchId);
}
