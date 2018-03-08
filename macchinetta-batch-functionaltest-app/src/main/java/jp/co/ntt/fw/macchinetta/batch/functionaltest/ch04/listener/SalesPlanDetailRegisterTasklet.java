/*
 * Copyright (C) 2018 NTT Corporation
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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

/**
 * Tasklet to create sales plan details.
 * <p>
 * It is implemented so that it behaves like a chunk oriented job. However, the transaction is a single transaction.
 * </p>
 *
 * @since 5.0.0
 */
@Component
@Scope("step")
public class SalesPlanDetailRegisterTasklet implements Tasklet {

    /**
     * File reader.
     */
    @Inject
    ItemStreamReader<SalesPlanDetail> reader;

    /**
     * Database writer.
     */
    @Inject
    ItemWriter<SalesPlanDetail> writer;

    @Inject
    @Named("amountCheckProcessor")
    ItemProcessor<SalesPlanDetail, SalesPlanDetail> processor;

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(SalesPlanDetailRegisterTasklet.class);

    /**
     * Create sales plan details.
     *
     * @param contribution Step contribution.
     * @param chunkContext Chunk context.
     * @return RepeatStatus.FINISHED
     * @throws Exception {@link IllegalArgumentException} occurs if the check fails.
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        SalesPlanDetail item = null;

        try {
            reader.open(chunkContext.getStepContext().getStepExecution().getExecutionContext());

            List<SalesPlanDetail> items = new ArrayList<>();

            while ((item = reader.read()) != null) {

                items.add(processor.process(item));
                if (items.size() == 10) {
                    writer.write(items);
                    items.clear();
                }
            }
            if (!items.isEmpty()) {
                writer.write(items);
            }
        } catch (IllegalArgumentException e) {
            logger.error("Tasklet Logging: Item is negative. [Item: {}]", item);
            throw e;
        } catch (Exception e) {
            logger.error("Tasklet Logging: on tasklet error.", e);
            throw e;
        } finally {
            reader.close();
        }

        return RepeatStatus.FINISHED;
    }
}
