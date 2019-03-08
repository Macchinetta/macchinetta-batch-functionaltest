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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Branch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.model.ExclusiveBranch;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.repository.ExclusiveControlRepository;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Clock;

/**
 * Item processor of editing branch master data.
 *
 * @since 2.0.1
 */
@Component
@Scope("step")
public class BranchEditWithkPessimisticLockItemProcessor implements ItemProcessor<String, ExclusiveBranch> {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(BranchEditWithkPessimisticLockItemProcessor.class);

    /**
     * Clock.
     */
    private Clock clock = Clock.systemDefaultZone();

    /**
     * Exclusive Control Repository.
     */
    @Inject
    ExclusiveControlRepository exclusiveControlRepository;

    /**
     * Process identifier.
     */
    private String identifier;

    /**
     * Branch name.
     */
    @Value("#{jobParameters['branchName']}")
    private String branchName;

    /**
     * Edit branch master data.
     *
     * @param item Target branch id.
     * @return Branch master data after edit.
     * @throws Exception {@inheritDoc}
     */
    @Override
    public ExclusiveBranch process(String item) throws Exception {

        Branch branch = exclusiveControlRepository.branchFindOneByNameWithNowWaitLock(item, branchName);

        if (branch != null) {
            ExclusiveBranch updatedBranch = new ExclusiveBranch();

            updatedBranch.setBranchId(branch.getBranchId());
            updatedBranch.setBranchName(branch.getBranchName() + " - " + identifier);
            updatedBranch.setBranchAddress(branch.getBranchAddress() + " - " + identifier);
            updatedBranch.setBranchTel(branch.getBranchTel());
            updatedBranch.setCreateDate(branch.getUpdateDate());
            updatedBranch.setUpdateDate(new Timestamp(clock.millis()));
            updatedBranch.setOldBranchName(branch.getBranchName());

            return updatedBranch;
        } else {
            logger.warn("An update by another user occurred. [branchId: {}]", item);
            return null;
        }
    }

    /**
     * Process identifier.
     *
     * @param identifier New process identifier.
     */
    @Value("#{jobParameters['identifier']}")
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
