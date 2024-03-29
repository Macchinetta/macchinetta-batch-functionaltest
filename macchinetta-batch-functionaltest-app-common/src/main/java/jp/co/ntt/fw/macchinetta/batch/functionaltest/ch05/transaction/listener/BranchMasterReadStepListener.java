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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.listener;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Branch;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.repository.admin.BranchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Load branch master before step execution.
 *
 * @since 2.0.1
 */
@Component
public class BranchMasterReadStepListener implements StepExecutionListener {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(BranchMasterReadStepListener.class);

    /**
     * Repository of Branch master.
     */
    @Inject
    @Named("adminBranchRepository")
    BranchRepository branchRepository;

    /**
     * Load branch master.
     *
     * @param stepExecution Step Execution context.
     */
    @Override
    public void beforeStep(StepExecution stepExecution) {

        logger.info("Load branch master before step execution.");

        List<Branch> branches = branchRepository.findAll();

        Map<String, Branch> map = branches.stream()
                .collect(Collectors.toMap(Branch::getBranchId, UnaryOperator.identity()));

        stepExecution.getExecutionContext().put("branches", map);

    }
}
