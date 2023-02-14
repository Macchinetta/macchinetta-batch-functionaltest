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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.exceptionhandling;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Sales performance import job by tasklet.
 */
@Component
@Scope("step")
public class SalesPerformanceWithSkipTasklet implements Tasklet {

    /**
     * logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(SalesPerformanceWithSkipTasklet.class);

    /**
     * ItemReader.
     */
    @Inject
    ItemStreamReader<SalesPerformanceDetail> reader;

    /**
     * ItemWriter.
     */
    @Inject
    ItemWriter<SalesPerformanceDetail> writer;

    /**
     * chink-size.
     */
    int chunkSize = 10;

    /**
     * limit for amount, it is specified config.
     */
    BigDecimal amountLimit = new BigDecimal(10000);

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        try {
            reader.open(chunkContext.getStepContext().getStepExecution().getExecutionContext());

            List<SalesPerformanceDetail> items = new ArrayList<>(10);
            SalesPerformanceDetail item = null;
            do {
                // Pseudo operation of ItemReader
                for (int i = 0; i < chunkSize; i++) {
                    item = reader.read();
                    if (item == null) {
                        break;
                    }
                    // Pseudo operation of ItemProcessor
                    try {
                        checkAmount(item.getAmount(), amountLimit);
                    } catch (Exception e) {
                        logger.warn("exception in tasklet, but process-continue.", e);
                        continue;
                    }
                    items.add(item);
                }
                // Pseudo operation of ItemWriter
                if (!items.isEmpty()) {
                    writer.write(new Chunk(items));
                    items.clear();
                }
            } while (item != null);
        } finally {
            try {
                reader.close();
            } catch (Exception e) {
                // do nothing.
            }
        }

        return RepeatStatus.FINISHED;
    }

    /**
     * do check.
     *
     * @param target check target
     * @param limit check limit
     */
    private void checkAmount(BigDecimal target, BigDecimal limit) {
        if (target.compareTo(limit) > 0) {
            throw new ArithmeticException("must be less than " + amountLimit);
        }
    }
}
