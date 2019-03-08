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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.component;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Branch;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Clock;

/**
 * Process business logic for branch master.
 *
 * @since 2.0.1
 */
@Component
public class BranchItemProcessor implements ItemProcessor<Branch, Branch> {

    /**
     * Clock.
     */
    private Clock clock = Clock.systemDefaultZone();

    /**
     * Process business logic for branch master.
     *
     * @param item Item before business logic execution
     * @return Item after business logic execution
     * @throws Exception Exception that occurred
     */
    @Override
    public Branch process(Branch item) throws Exception {

        item.setCreateDate(new Timestamp(clock.millis()));

        return item;
    }
}
