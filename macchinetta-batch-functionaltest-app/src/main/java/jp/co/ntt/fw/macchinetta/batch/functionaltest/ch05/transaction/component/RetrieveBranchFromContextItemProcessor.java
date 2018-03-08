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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.component;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Branch;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.model.CustomerWithBranch;

import java.util.Map;

/**
 * Add incidental data acquired before step execution.
 *
 * @since 5.0.0
 */
@Component
@Scope("step")
public class RetrieveBranchFromContextItemProcessor implements ItemProcessor<Customer, CustomerWithBranch> {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(RetrieveBranchFromContextItemProcessor.class);

    /**
     * Branch master.
     */
    private Map<String, Branch> branches;

    /**
     * Get branch master form step execution context.
     *
     * @param stepExecution Step execution
     */
    @BeforeStep
    @SuppressWarnings("unchecked")
    public void beforeStep(StepExecution stepExecution) {

        logger.info("Get branch master form step execution context.");

        branches = (Map<String, Branch>) stepExecution.getExecutionContext().get("branches");
    }

    /**
     * Add incidental data acquired before step execution.
     *
     * @param item Customer form database.
     * @return Customer with branch.
     * @throws Exception {@inheritDoc}
     */
    @Override
    public CustomerWithBranch process(Customer item) throws Exception {
        CustomerWithBranch newItem = new CustomerWithBranch(item);
        newItem.setBranch(branches.get(item.getChargeBranchId()));
        return newItem;
    }
}
