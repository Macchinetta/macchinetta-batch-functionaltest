/*
 * Copyright (C) 2024 NTT Corporation
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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.jobs.ch08.flowcontrol;

import java.util.Arrays;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.transaction.PlatformTransactionManager;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for JobChunkPromotionalFlow job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan(value = "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.flowcontrol", scopedProxy = ScopedProxyMode.TARGET_CLASS)
public class JobChunkPromotionalFlowConfig {

    @Bean
    @StepScope
    public ListItemReader<String> listItemReader() {
        return new ListItemReader<>(Arrays.asList("#{'item1'}"));
    }

    @Bean
    ExecutionContextPromotionListener executionContextPromotionListener() {
        ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
        listener.setKeys(new String[] { "promotion" });
        listener.setStrict(true);
        return listener;
    }

    @Bean
    public Step step1(JobRepository jobRepository,
                      @Qualifier("executionContextPromotionListener") ExecutionContextPromotionListener listener,
                      @Qualifier("listItemReader") ListItemReader<String> reader,
                      @Qualifier("promotionSourceItemProcessor") ItemProcessor<String, String> processor,
                      @Qualifier("promotionLogItemWriter") ItemWriter<String> writer,
                      @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        SimpleStepBuilder<String, String> builder
                = parentStepBuilder("jobChunkPromotionalFlow.step1",
                jobRepository, reader, processor, writer, transactionManager);
        builder.listener(listener);
        return builder.build();
    }

    @Bean
    public Step step2(JobRepository jobRepository,
                      @Qualifier("listItemReader") ListItemReader<String> reader,
                      @Qualifier("promotionTargetItemProcessor") ItemProcessor<String, String> processor,
                      @Qualifier("promotionLogItemWriter") ItemWriter<String> writer,
                      @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        SimpleStepBuilder<String, String> builder
                = parentStepBuilder("jobChunkPromotionalFlow.step2",
                jobRepository, reader, processor, writer, transactionManager);
        return builder.build();
    }

    @Bean
    public Job jobChunkPromotionalFlow(JobRepository jobRepository,
                                       @Qualifier("step1") Step step1,
                                       @Qualifier("step2") Step step2) {
        return new JobBuilder("jobChunkPromotionalFlow", jobRepository)
                .start(step1)
                .next(step2)
                .build();
    }

    SimpleStepBuilder<String, String> parentStepBuilder(String stepName,
                                                        JobRepository jobRepository,
                                                        ListItemReader<String> reader,
                                                        ItemProcessor<String, String> processor,
                                                        ItemWriter<String> writer,
                                                        PlatformTransactionManager transactionManager) {
        return new StepBuilder(stepName, jobRepository)
                .<String, String> chunk(1, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer);
    }
}
