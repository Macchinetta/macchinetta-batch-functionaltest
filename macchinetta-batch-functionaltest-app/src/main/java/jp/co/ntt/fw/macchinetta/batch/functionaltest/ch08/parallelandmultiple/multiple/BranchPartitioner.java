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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.multiple;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Branch;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.repository.BranchRepository;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Branch partitioner.
 *
 * @since 5.0.0
 */
@Component
public class BranchPartitioner implements Partitioner {

    /**
     * Repository of branch master.
     */
    @Inject
    BranchRepository branchRepository;

    /**
     * Create partition to divide at branch office.
     *
     * @param gridSize grid size
     * @return Partitioner for division at branch office.
     */
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        Map<String, ExecutionContext> map = new HashMap<>();
        List<Branch> branches = branchRepository.findAll();

        int index = 0;
        for (Branch branch : branches) {
            ExecutionContext context = new ExecutionContext();
            context.putString("branchId", branch.getBranchId());
            map.put("partition" + index, context);
            index++;
        }

        return map;
    }
}
