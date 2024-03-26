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

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.flowcontrol.ConfirmPromotionalTasklet;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.flowcontrol.SavePromotionalTasklet;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for JobPromotionalFlow job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
public class JobPromotionalFlowConfig {

    @Bean
    public SavePromotionalTasklet savePromotionalTasklet() {
        return new SavePromotionalTasklet();
    }

    @Bean
    public ConfirmPromotionalTasklet confirmPromotionalTasklet() {
        return new ConfirmPromotionalTasklet();
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
                      @Qualifier("savePromotionalTasklet") SavePromotionalTasklet tasklet,
                      @Qualifier("executionContextPromotionListener") ExecutionContextPromotionListener listener,
                      @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("jobPromotionalFlow.step1", jobRepository)
                .tasklet(tasklet, transactionManager)
                .listener(listener)
                .build();
    }

    @Bean
    public Step step2(JobRepository jobRepository,
                      @Qualifier("confirmPromotionalTasklet") ConfirmPromotionalTasklet tasklet,
                      @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("jobPromotionalFlow.step2", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Job jobPromotionalFlow(JobRepository jobRepository,
                                  @Qualifier("step1") Step step1,
                                  @Qualifier("step2") Step step2) {
        return new JobBuilder("jobPromotionalFlow", jobRepository)
                .start(step1)
                .next(step2)
                .build();
    }
}
