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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.plan.SalesPlanDetailRepository;

/**
 * Create sales plan by chunk transaction of tasklet model.
 * <p>
 * Task to read data from file and register it in database.
 * </p>
 * <p>
 * Control of transaction control by itself.
 * </p>
 *
 * @since 2.0.1
 */
@Component
@Scope("step")
public class SalesPlanChunkTranTask implements Tasklet {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(SalesPlanChunkTranTask.class);

    @Inject
    @Named("detailCSVReader")
    ItemStreamReader<SalesPlanDetail> itemReader;

    @Inject
    @Named("jobTransactionManager")
    PlatformTransactionManager transactionManager;

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
        logger.info("Start Create sales plan by chunk transaction of tasklet model.");

        SalesPlanDetail item;
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        TransactionStatus status = null;
        int count = 0;

        try {
            itemReader.open(chunkContext.getStepContext().getStepExecution().getExecutionContext());

            while ((item = itemReader.read()) != null) {

                logger.info("Read item: {}", item);
                if (count % 10 == 0) {
                    status = transactionManager.getTransaction(definition);
                }
                count++;
                repository.create(item);
                if (count % 10 == 0) {
                    transactionManager.commit(status);
                }
            }
            if (status != null && !status.isCompleted()) {
                transactionManager.commit(status);
            }
        } catch (Exception e) {
            logger.error("Exception occurred while reading.", e);
            if (status != null && !status.isCompleted()) {
            	transactionManager.rollback(status);
            }
            throw e;
        } finally {
        	if (status != null && !status.isCompleted()) {
                try {
                    transactionManager.rollback(status);
                } catch (TransactionException e2) {
                	logger.error("TransactionException occurred while rollback.", e2);
                }
            }
            itemReader.close();
        }

        logger.info("Finish Create sales plan by chunk transaction of tasklet model.");
        return RepeatStatus.FINISHED;
    }
}
