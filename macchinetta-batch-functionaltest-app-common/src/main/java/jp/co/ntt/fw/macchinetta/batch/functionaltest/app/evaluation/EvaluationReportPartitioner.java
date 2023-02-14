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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.app.evaluation;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Branch;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.evaluation.EvaluationRepository;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Partitioner for evaluation reports.
 *
 * @since 2.0.1
 */
@Component
public class EvaluationReportPartitioner implements Partitioner {

    /**
     * Key name of partitioner.
     */
    private static final String KEY_NAME = "branch";

    /**
     * Partition key.
     */
    private static final String PARTITION_KEY = "partition";

    /**
     * Repository of evaluation.
     */
    @Inject
    EvaluationRepository evaluationRepository;

    /**
     * {@inheritDoc}
     *
     * @param gridSize {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        Map<String, ExecutionContext> map = new HashMap<>(gridSize);
        List<Branch> branches = evaluationRepository.findAllBranch();

        int index = 0;
        for (Branch branch : branches) {
            ExecutionContext context = new ExecutionContext();
            context.put(KEY_NAME, branch);
            map.put(PARTITION_KEY + index, context);
            index++;
        }

        return map;
    }
}
