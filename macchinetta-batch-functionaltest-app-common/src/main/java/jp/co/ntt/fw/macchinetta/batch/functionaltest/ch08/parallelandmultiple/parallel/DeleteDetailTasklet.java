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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.parallel;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.plan.SalesPlanDetailRepository;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.performance.SalesPerformanceDetailRepository;

import jakarta.inject.Inject;

/**
 * Delete plan and performace details all data.
 *
 * @since 2.0.1
 */
@Component
public class DeleteDetailTasklet implements Tasklet {

    @Inject
    SalesPlanDetailRepository planDetailRepository;

    @Inject
    SalesPerformanceDetailRepository performanceDetailRepository;

    /**
     * Delete plan and performace details all data.
     *
     * @param contribution Step contribution.
     * @param chunkContext Chunk context.
     * @return RepeatStatus.FINISH
     * @throws Exception An error occurred.
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        planDetailRepository.deleteAll();
        performanceDetailRepository.deleteAll();

        return RepeatStatus.FINISHED;
    }
}
