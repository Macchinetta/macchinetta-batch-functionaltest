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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.jobs.ch04.asyncjobwithdb;

import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.JobExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.asyncjobwithdb.module.LoggingItemWriter;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.asyncjobwithdb.module.ModularizationConfirmationItemProcessor02;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for AsyncJobModularizeConfirmJob02 job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan({ "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.asyncjobwithdb.module" })
public class AsyncJobModularizeConfirmJob02Config {

    @Bean
    public ListItemReader<String> jobDBPolling06Reader() {
        var list = List.of("Data0601-01");
        return new ListItemReader<String>(list);
    }

    @Bean
    public ModularizationConfirmationItemProcessor02 jobDBPolling06Processor() {
        return new ModularizationConfirmationItemProcessor02();
    }

    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       ListItemReader<String> reader,
                       ModularizationConfirmationItemProcessor02 processor,
                       @Qualifier("jobDBPolling06Writer") LoggingItemWriter writer) {
        return new StepBuilder("asyncJobModularizeConfirmJob02.step01",
                jobRepository)
                .chunk(10, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job asyncJobModularizeConfirmJob02(JobRepository jobRepository,
                                              Step step01,
                                              JobExecutionLoggingListener listener) {
        return new JobBuilder("asyncJobModularizeConfirmJob02", jobRepository)
                .start(step01)
                .listener(listener)
                .build();
    }
}
