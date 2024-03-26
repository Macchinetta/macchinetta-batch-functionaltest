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
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.builder.TaskletStepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch07.jobmanagement.JobExitCodeChangeListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.flowcontrol.ChangeExitCodeReturnListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.flowcontrol.SequentialFlowTasklet;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for JobStopFlow job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
public class JobStopFlowConfig {

    @Bean
    @StepScope
    public SequentialFlowTasklet stopFlowTasklet(
            @Value("#{jobParameters['failExecutionStep']}") String failExecutionStep) {
        SequentialFlowTasklet tasklet = new SequentialFlowTasklet();
        tasklet.setFailExecutionStep(failExecutionStep);
        return tasklet;
    }

    @Bean
    ChangeExitCodeReturnListener changeExitCodeReturnListener() {
        return new ChangeExitCodeReturnListener();
    }

    @Bean
    JobExitCodeChangeListener jobExitCodeChangeListener() {
        return new JobExitCodeChangeListener();
    }

    TaskletStepBuilder parentStepBuilder(String stepName,
                                         JobRepository jobRepository,
                                         SequentialFlowTasklet tasklet,
                                         ChangeExitCodeReturnListener listener,
                                         PlatformTransactionManager transactionManager) {
        return new StepBuilder(stepName, jobRepository)
                .listener(listener)
                .tasklet(tasklet, transactionManager);
    }

    @Bean
    public Step step1(JobRepository jobRepository,
                      @Qualifier("stopFlowTasklet") SequentialFlowTasklet tasklet,
                      @Qualifier("changeExitCodeReturnListener") ChangeExitCodeReturnListener listener,
                      @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return parentStepBuilder("jobStopFlow.step1", jobRepository, tasklet,
                listener, transactionManager)
                .build();
    }

    @Bean
    public Step step2(JobRepository jobRepository,
                      @Qualifier("stopFlowTasklet") SequentialFlowTasklet tasklet,
                      @Qualifier("changeExitCodeReturnListener") ChangeExitCodeReturnListener listener,
                      @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return parentStepBuilder("jobStopFlow.step2", jobRepository, tasklet,
                listener, transactionManager)
                .build();
    }

    @Bean
    public Step step3(JobRepository jobRepository,
                      @Qualifier("stopFlowTasklet") SequentialFlowTasklet tasklet,
                      @Qualifier("changeExitCodeReturnListener") ChangeExitCodeReturnListener listener,
                      @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return parentStepBuilder("jobStopFlow.step3", jobRepository, tasklet,
                listener, transactionManager)
                .build();
    }

    @Bean
    public Step step4(JobRepository jobRepository,
                      @Qualifier("stopFlowTasklet") SequentialFlowTasklet tasklet,
                      @Qualifier("changeExitCodeReturnListener") ChangeExitCodeReturnListener listener,
                      @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return parentStepBuilder("jobStopFlow.step4", jobRepository, tasklet,
                listener, transactionManager)
                .build();
    }

    @Bean
    public Job jobStopFlow(JobRepository jobRepository,
                           @Qualifier("jobExitCodeChangeListener") JobExitCodeChangeListener listener,
                           @Qualifier("step1") Step step1,
                           @Qualifier("step2") Step step2,
                           @Qualifier("step3") Step step3,
                           @Qualifier("step4") Step step4) {
        return new JobBuilder("jobStopFlow", jobRepository)
                .start(step1)
                .listener(listener)
                .on("END_WITH_NO_EXIT_CODE").end()
                .from(step1).on("END_WITH_EXIT_CODE").end("COMPLETED_CUSTOM")
                .from(step1).on("*").to(step2)
                .from(step2).on("FORCE_FAIL_WITH_NO_EXIT_CODE").fail()
                .from(step2).on("FORCE_FAIL_WITH_EXIT_CODE").fail()
                .from(step2).on("*").to(step3)
                .from(step3).on("FORCE_STOP").stopAndRestart(step4)
                .from(step3).on("FORCE_STOP_WITH_EXIT_CODE")
                .stopAndRestart(step4)
                .from(step3).on("*").to(step4)
                .end()
                .build();
    }
}
