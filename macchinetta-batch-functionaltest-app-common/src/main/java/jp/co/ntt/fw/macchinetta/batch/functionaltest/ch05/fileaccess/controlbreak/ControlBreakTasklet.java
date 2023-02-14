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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.controlbreak;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.batch.item.support.SingleItemPeekableItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Control break tasklet.
 *
 * @since 2.0.1
 */
@Component
@Scope("step")
public class ControlBreakTasklet implements Tasklet {

    /**
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(ControlBreakTasklet.class);

    /**
     * Reader.
     */
    @Inject
    SingleItemPeekableItemReader<SalesPerformanceDetail> reader;

    /**
     * Writer.
     */
    @Inject
    ItemStreamWriter<SalesPerformanceDetail> writer;

    /**
     * Control break.
     * <p>
     * Realize headlines and summaries with a control break.
     * </p>
     *
     * @param contribution Step contribution.
     * @param chunkContext Chunk context.
     * @return RepeatStatus.FINISHED.
     * @throws Exception Exception that occurred.
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        ExecutionContext executionContext = chunkContext.getStepContext().getStepExecution().getExecutionContext();

        SalesPerformanceDetail previousData = null;
        BigDecimal[] summaries = { new BigDecimal(0), new BigDecimal(0), new BigDecimal(0) };

        List<SalesPerformanceDetail> items = new ArrayList<>();

        try {
            reader.open(executionContext);
            writer.open(executionContext);

            while (reader.peek() != null) {
                SalesPerformanceDetail data = reader.read();
                // Create a heading with a control break
                addData(items, beforeBerakByBranchId(previousData, data));
                addData(items, beforeBerakByYear(previousData, data));
                addData(items, beforeBerakByMonth(previousData, data));

                items.add(data);

                // Create summary with a control break
                SalesPerformanceDetail nextData = reader.peek();
                summaries[2] = summaries[2].add(data.getAmount());
                SalesPerformanceDetail monthSummary = afterBerakByMonth(nextData, data, summaries[2]);
                if (monthSummary != null) {
                    items.add(monthSummary);
                    summaries[1] = summaries[1].add(summaries[2]);
                    summaries[2] = new BigDecimal(0);
                }
                SalesPerformanceDetail YearSummary = afterBerakByYear(nextData, data, summaries[1]);
                if (YearSummary != null) {
                    items.add(YearSummary);
                    summaries[0] = summaries[0].add(summaries[1]);
                    summaries[2] = new BigDecimal(0);
                    summaries[1] = new BigDecimal(0);
                }
                SalesPerformanceDetail branchSumarry = afterBerakByBranchId(nextData, data, summaries[0]);
                if (branchSumarry != null) {
                    items.add(branchSumarry);
                    summaries[2] = new BigDecimal(0);
                    summaries[1] = new BigDecimal(0);
                    summaries[0] = new BigDecimal(0);
                    writer.write(new Chunk(items));
                    items.clear();
                }
                previousData = data;
            }

        } finally {
            try {
                reader.close();
            } catch (ItemStreamException e) {
                logger.error("An error occurred when closing the reader.", e);
            }
            try {
                writer.close();
            } catch (ItemStreamException e) {
                logger.error("An error occurred when closing the writer.", e);
            }
        }

        return RepeatStatus.FINISHED;
    }

    /**
     * Perform control break processing with branch id before processing target data.
     *
     * @param previousData Previous data.
     * @param currentData Data to be processed.
     * @return When a control break is made, return a headline, otherwise return null.
     */
    private SalesPerformanceDetail beforeBerakByBranchId(SalesPerformanceDetail previousData,
            SalesPerformanceDetail currentData) {
        SalesPerformanceDetail writeData = null;
        if (isBreakByBranchId(previousData, currentData)) {
            writeData = new SalesPerformanceDetail();
            writeData.setBranchId("Header Branch Id : " + currentData.getBranchId());
        }
        return writeData;
    }

    /**
     * Perform control break processing with year before processing target data.
     *
     * @param previousData Previous data.
     * @param currentData Data to be processed.
     * @return When a control break is made, return a headline, otherwise return null.
     */
    private SalesPerformanceDetail beforeBerakByYear(SalesPerformanceDetail previousData,
            SalesPerformanceDetail currentData) {
        SalesPerformanceDetail writeData = null;
        if (isBreakByYear(previousData, currentData)) {
            writeData = new SalesPerformanceDetail();
            writeData.setBranchId("Header Year : " + currentData.getYear());
        }
        return writeData;
    }

    /**
     * Perform control break processing with month before processing target data.
     *
     * @param previousData Previous data.
     * @param currentData Data to be processed.
     * @return When a control break is made, return a headline, otherwise return null.
     */
    private SalesPerformanceDetail beforeBerakByMonth(SalesPerformanceDetail previousData,
            SalesPerformanceDetail currentData) {
        SalesPerformanceDetail writeData = null;
        if (isBreakByMonth(previousData, currentData)) {
            writeData = new SalesPerformanceDetail();
            writeData.setBranchId("Header Month : " + currentData.getMonth());
        }
        return writeData;
    }

    /**
     * Perform control break processing with branch id after processing target data.
     *
     * @param nextData Next data.
     * @param currentData Data to be processed.
     * @return When a control break is made, return a headline, otherwise return null.
     */
    private SalesPerformanceDetail afterBerakByBranchId(SalesPerformanceDetail nextData,
            SalesPerformanceDetail currentData, BigDecimal summary) {
        SalesPerformanceDetail writeData = null;
        if (isBreakByBranchId(nextData, currentData)) {
            writeData = new SalesPerformanceDetail();
            writeData.setBranchId("Summary Branch Id : " + currentData.getBranchId());
            writeData.setAmount(summary);
        }
        return writeData;
    }

    /**
     * Perform control break processing with year after processing target data.
     *
     * @param nextData Next data.
     * @param currentData Data to be processed.
     * @return When a control break is made, return a headline, otherwise return null.
     */
    private SalesPerformanceDetail afterBerakByYear(SalesPerformanceDetail nextData,
            SalesPerformanceDetail currentData, BigDecimal summary) {
        SalesPerformanceDetail writeData = null;
        if (isBreakByYear(nextData, currentData)) {
            writeData = new SalesPerformanceDetail();
            writeData.setBranchId("Summary Year : " + currentData.getYear());
            writeData.setAmount(summary);
        }
        return writeData;
    }

    /**
     * Perform control break processing with month after processing target data.
     *
     * @param nextData Next data.
     * @param currentData Data to be processed.
     * @return When a control break is made, return a headline, otherwise return null.
     */
    private SalesPerformanceDetail afterBerakByMonth(SalesPerformanceDetail nextData,
            SalesPerformanceDetail currentData, BigDecimal summary) {
        SalesPerformanceDetail writeData = null;
        if (isBreakByMonth(nextData, currentData)) {
            writeData = new SalesPerformanceDetail();
            writeData.setBranchId("Summary Month : " + currentData.getMonth());
            writeData.setAmount(summary);
        }
        return writeData;
    }

    /**
     * Determine whether can control break by branch id.
     *
     * @param o1 Comparison target.
     * @param o2 The other side of the comparison target.
     * @return Returns true if control break is available, and returns false otherwise.
     */
    private boolean isBreakByBranchId(SalesPerformanceDetail o1, SalesPerformanceDetail o2) {
        return (o1 == null || !o1.getBranchId().equals(o2.getBranchId()));
    }

    /**
     * Determine whether can control break by year.
     *
     * @param o1 Comparison target.
     * @param o2 The other side of the comparison target.
     * @return Returns true if control break is available, and returns false otherwise.
     */
    private boolean isBreakByYear(SalesPerformanceDetail o1, SalesPerformanceDetail o2) {
        if (isBreakByBranchId(o1, o2)) {
            return true;
        } else {
            return (o1 == null || o1.getYear() != o2.getYear());
        }
    }

    /**
     * Determine whether can control break by month.
     *
     * @param o1 Comparison target.
     * @param o2 The other side of the comparison target.
     * @return Returns true if control break is available, and returns false otherwise.
     */
    private boolean isBreakByMonth(SalesPerformanceDetail o1, SalesPerformanceDetail o2) {
        if (isBreakByBranchId(o1, o2) || isBreakByYear(o1, o2)) {
            return true;
        } else {
            return (o1 == null || o1.getMonth() != o2.getMonth());
        }
    }

    private void addData(List<SalesPerformanceDetail> items, SalesPerformanceDetail data) {
        if (data != null) {
            items.add(data);
        }
    }
}
