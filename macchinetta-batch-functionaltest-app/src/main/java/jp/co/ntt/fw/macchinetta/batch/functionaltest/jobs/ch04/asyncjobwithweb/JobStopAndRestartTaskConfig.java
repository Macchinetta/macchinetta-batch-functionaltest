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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.jobs.ch04.asyncjobwithweb;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.SimpleJobTasklet;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.asyncjobwithweb.EmulatedLongBatchTask;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for JobEmulatedLongBatchTask job.
 *
 * @since 2.6.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
public class JobStopAndRestartTaskConfig {

    @Bean
    public EmulatedLongBatchTask emulatedLongBatchTask() {
        return new EmulatedLongBatchTask();
    }
    
    @Bean
    public SimpleJobTasklet simpleJobTasklet() {
        return new SimpleJobTasklet();
    }
    
    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       EmulatedLongBatchTask emulatedLongBatchTask) {
        return new StepBuilder("jobStopAndRestartTask.step01",
                jobRepository)
                .tasklet(emulatedLongBatchTask, transactionManager)
                .build();
    }
    
    @Bean
    public Step step02(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       SimpleJobTasklet simpleJobTasklet) {
        return new StepBuilder("jobStopAndRestartTask.step02",
                jobRepository)
                .tasklet(simpleJobTasklet, transactionManager)
                .build();
    }
    
    @Bean
    public Job jobStopAndRestartTask(JobRepository jobRepository,
                                     @Qualifier("step01") Step step01,
                                     @Qualifier("step02") Step step02) {
        return new JobBuilder("jobStopAndRestartTask",jobRepository)
                .start(step01)
                .next(step02)
                .build();
    }
}
