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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch07.jobmanagement;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Tasklet to check amount.
 * <p>
 * Check the amount, skip the data executing in case of an error.
 * </p>
 *
 * @since 5.1.0
 */
@Component
@Scope("step")
public class CheckAmountTasklet implements Tasklet {

    /**
     * Chunk size.
     */
    private static final int CHUNK_SIZE = 10;

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(CheckAmountTasklet.class);

    /**
     * Reader.
     */
    @Inject
    ItemStreamReader<SalesPlanSummary> reader;

    /**
     * Writer.
     */
    @Inject
    ItemWriter<SalesPlanSummary> writer;

    /**
     * Check the amount, skip the data executing in case of an error.
     * @param contribution Step contribution.
     * @param chunkContext Chunk context.
     * @return RepeatStatus (FINISHED).
     * @throws Exception Exception that occurred.
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        SalesPlanSummary item = null;

        List<SalesPlanSummary> items = new ArrayList<>(CHUNK_SIZE);
        int errorCount = 0;

        try {
            reader.open(chunkContext.getStepContext().getStepExecution().getExecutionContext());
            while ((item = reader.read()) != null) {
                if (item.getAmount().signum() == -1) {
                    logger.warn("amount is negative. skip item [item: {}]", item);
                    errorCount++;
                    continue;
                }

                items.add(item);

                if (items.size() == CHUNK_SIZE) {
                    writer.write(items);
                    items.clear();
                }
            }

            writer.write(items);
        } finally {
            reader.close();
        }

        if (errorCount > 0) {
            logger.info("Change status 'STEP COMPLETED WITH SKIPS'");
            contribution.setExitStatus(new ExitStatus("STEP COMPLETED WITH SKIPS"));
        }
        return RepeatStatus.FINISHED;
    }
}
