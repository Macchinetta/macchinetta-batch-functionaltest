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

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Customer;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.model.CompositeMaster;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Branch;

import java.sql.Timestamp;
import java.time.Clock;

/**
 * Field set mapper for master composite model.
 *
 * @since 2.0.1
 */
@Component
public class CompositMasterFieldSetMapper implements FieldSetMapper<CompositeMaster> {

    /**
     * Clock.
     */
    private Clock clock = Clock.systemDefaultZone();

    /**
     * Distributes the read data to each master.
     *
     * @param fieldSet FieldSet of composite model of master
     * @return Composite model of master
     * @throws BindException {@inheritDoc}
     */
    @Override
    public CompositeMaster mapFieldSet(FieldSet fieldSet) throws BindException {
        CompositeMaster master = new CompositeMaster();
        Customer customer = new Customer();
        Branch branch = new Branch();

        Timestamp createDate = new Timestamp(clock.millis());

        customer.setCustomerId(fieldSet.readString("customerId"));
        customer.setChargeBranchId(fieldSet.readString("branchId"));
        customer.setCustomerName(fieldSet.readString("customerName"));
        customer.setCustomerAddress(fieldSet.readString("customerAddress"));
        customer.setCustomerTel(fieldSet.readString("customerTel"));
        customer.setCreateDate(createDate);

        branch.setBranchId(fieldSet.readString("branchId"));
        branch.setBranchName(fieldSet.readString("branchName"));
        branch.setBranchAddress(fieldSet.readString("branchAddress"));
        branch.setBranchTel(fieldSet.readString("branchTel"));
        branch.setCreateDate(createDate);

        master.setBranch(branch);
        master.setCustomer(customer);

        return master;
    }
}
