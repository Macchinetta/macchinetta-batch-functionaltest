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
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Clock;

/**
 * Item processor of editing branch master data.
 *
 * @since 2.0.1
 */
@Component
@Scope("step")
public class BranchEditItemProcessor implements ItemProcessor<Branch, ExclusiveBranch> {

    /**
     * Process identifier.
     */
    private String identifier;

    /**
     * Clock.
     */
    private Clock clock = Clock.systemDefaultZone();

    /**
     * Edit branch master data.
     *
     * @param item Branch master data before edit.
     * @return Branch master data after edit.
     * @throws Exception {@inheritDoc}
     */
    @Override
    public ExclusiveBranch process(Branch item) throws Exception {
        ExclusiveBranch branch = new ExclusiveBranch();

        branch.setBranchId(item.getBranchId());
        branch.setBranchName(item.getBranchName() + " - " + identifier);
        branch.setBranchAddress(item.getBranchAddress() + " - " + identifier);
        branch.setBranchTel(item.getBranchTel());
        branch.setCreateDate(item.getUpdateDate());
        branch.setUpdateDate(new Timestamp(clock.millis()));
        branch.setOldBranchName(item.getBranchName());

        return branch;
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
