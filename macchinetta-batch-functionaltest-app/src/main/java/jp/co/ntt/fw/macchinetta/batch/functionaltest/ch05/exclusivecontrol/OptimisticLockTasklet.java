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
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.model.ExclusiveBranch;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.repository.ExclusiveControlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Clock;

/**
 * Tasket to check DB optimistic lock.
 *
 * @since 2.0.1
 */
@Component
@Scope("step")
public class OptimisticLockTasklet implements Tasklet {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(OptimisticLockTasklet.class);

    /**
     * Clock.
     */
    private Clock clock = Clock.systemDefaultZone();

    /**
     * Repository of exclusive repository.
     */
    @Inject
    ExclusiveControlRepository repository;

    /**
     * Branch id
     */
    private String branchId = null;

    /**
     * Identifier.
     */
    private String identifier;

    /**
     * Perform optimistic locks and update data.
     *
     * @param contribution Step contribution.
     * @param chunkContext Chunk context.
     * @return RepeatStatus.FINISHED
     * @throws Exception When a pessimistic lock occurs.
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        Branch branch = repository.branchFindOne(branchId);
        ExclusiveBranch exclusiveBranch = new ExclusiveBranch();

        exclusiveBranch.setBranchId(branch.getBranchId());
        exclusiveBranch.setBranchName(branch.getBranchName() + " - " + identifier);
        exclusiveBranch.setBranchAddress(branch.getBranchAddress() + " - " + identifier);
        exclusiveBranch.setBranchTel(branch.getBranchTel());
        exclusiveBranch.setCreateDate(branch.getUpdateDate());
        exclusiveBranch.setUpdateDate(new Timestamp(clock.millis()));
        exclusiveBranch.setOldBranchName(branch.getBranchName());

        int result = repository.branchExclusiveUpdate(exclusiveBranch);

        logger.info("update {} result = {}.", identifier, result);

        return RepeatStatus.FINISHED;
    }

    /**
     * branchId.
     *
     * @param branchId New branchId.
     */
    @Value("#{jobParameters['branchId']}")
    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    /**
     * Identifier.
     *
     * @param identifier New identifier.
     */
    @Value("#{jobParameters['identifier']}")
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
