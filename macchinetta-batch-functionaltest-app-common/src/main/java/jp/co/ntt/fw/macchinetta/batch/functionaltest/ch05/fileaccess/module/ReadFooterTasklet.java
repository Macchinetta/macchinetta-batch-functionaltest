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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.module;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.model.plan.SalesPlanDetailFooter;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.repeat.RepeatStatus;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.ArrayList;

/**
 * Tasklet to read footer record file.
 *
 * @since 2.0.1
 */
public class ReadFooterTasklet implements Tasklet {
    @Inject
    @Named("footerReader")
    ItemStreamReader<SalesPlanDetailFooter> itemReader;

    private JobExecution jobExecution;

    @BeforeJob
    public void beforeJob(JobExecution jobExecution) {
        this.jobExecution = jobExecution;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        ArrayList<SalesPlanDetailFooter> footers = new ArrayList<>();

        itemReader.open(chunkContext.getStepContext().getStepExecution().getExecutionContext());

        SalesPlanDetailFooter footer;
        while ((footer = itemReader.read()) != null) {
            footers.add(footer);
        }

        jobExecution.getExecutionContext().put("footers", footers);

        return RepeatStatus.FINISHED;
    }
}
