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

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.plan.SalesPlanDetailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Create sales plan by single transaction of tasklet model.
 * <p>
 * Task to read data from file and register it in database.
 * </p>
 * <p>
 * Leave transaction control to the framework.
 * </p>
 *
 * @since 2.0.1
 */
@Component
@Scope("step")
public class SalesPlanSingleTranTask implements Tasklet {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(SalesPlanSingleTranTask.class);

    @Inject
    @Named("detailCSVReader")
    ItemStreamReader<SalesPlanDetail> itemReader;

    @Inject
    SalesPlanDetailRepository repository;

    /**
     * Task to read data from file and register it in database.
     *
     * @param contribution Step contribution.
     * @param chunkContext Chunk Context.
     * @return Repeat status (FINISHED).
     * @throws Exception Exception that occurred.
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        logger.info("Start Create sales plan by single transaction of tasklet model.");

        SalesPlanDetail item;

        try {
            itemReader.open(chunkContext.getStepContext().getStepExecution().getExecutionContext());

            while ((item = itemReader.read()) != null) {

                logger.info("Read item: {}", item);
                repository.create(item);
            }
        } catch (Exception e) {
            logger.error("Exception occurred while reading.", e);
            throw e;
        } finally {
            itemReader.close();
        }

        logger.info("Finish Create sales plan by single transaction of tasklet model.");
        return RepeatStatus.FINISHED;
    }
}
