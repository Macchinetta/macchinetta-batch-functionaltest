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

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Branch;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.repository.ExclusiveControlRepository;

import javax.inject.Inject;

/**
 * Tasket to check DB pessimistic lock.
 *
 * @since 5.0.0
 */
@Component
@Scope("step")
public class PessimisticLockTasklet implements Tasklet {

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
     * Perform pessimistic locks and update data.
     *
     * @param contribution Step contribution.
     * @param chunkContext Chunk context.
     * @return RepeatStatus.FINISHED
     * @throws Exception When a pessimistic lock occurs.
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        Branch branch = repository.branchFindOneWithNowWaitLock(branchId);
        branch.setBranchAddress(branch.getBranchAddress() + " - updated.");
        repository.branchUpdate(branch);

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
}
