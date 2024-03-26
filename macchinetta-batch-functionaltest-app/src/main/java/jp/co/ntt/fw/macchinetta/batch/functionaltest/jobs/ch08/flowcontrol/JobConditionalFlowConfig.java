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
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.builder.TaskletStepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.flowcontrol.ChangeExitCodeReturnListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.flowcontrol.SequentialFlowTasklet;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for JobConditionalFlow job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
public class JobConditionalFlowConfig {

    @Bean
    @StepScope
    public SequentialFlowTasklet conditionalFlowTasklet(
            @Value("#{jobParameters['failExecutionStep']}") String failExecutionStep) {
        SequentialFlowTasklet tasklet = new SequentialFlowTasklet();
        tasklet.setFailExecutionStep(failExecutionStep);
        return tasklet;
    }

    @Bean
    ChangeExitCodeReturnListener changeExitCodeReturnListener() {
        return new ChangeExitCodeReturnListener();
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
    public Step stepA(JobRepository jobRepository,
                      SequentialFlowTasklet tasklet,
                      ChangeExitCodeReturnListener listener,
                      @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return parentStepBuilder("jobConditionalFlow.stepA", jobRepository,
                tasklet, listener, transactionManager)
                .build();
    }

    @Bean
    public Step stepB(JobRepository jobRepository,
                      SequentialFlowTasklet tasklet,
                      ChangeExitCodeReturnListener listener,
                      @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return parentStepBuilder("jobConditionalFlow.stepB", jobRepository,
                tasklet, listener, transactionManager)
                .build();
    }

    @Bean
    public Step stepC(JobRepository jobRepository,
                      SequentialFlowTasklet tasklet,
                      ChangeExitCodeReturnListener listener,
                      @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return parentStepBuilder("jobConditionalFlow.stepC", jobRepository,
                tasklet, listener, transactionManager)
                .build();
    }

    @Bean
    public Job jobConditionalFlow(JobRepository jobRepository,
                                  @Qualifier("stepA") Step stepA,
                                  @Qualifier("stepB") Step stepB,
                                  @Qualifier("stepC") Step stepC) {
        return new JobBuilder("jobConditionalFlow", jobRepository)
                .start(stepA)
                .on(FlowExecutionStatus.COMPLETED.getName()).to(stepB)
                .from(stepA)
                .on(FlowExecutionStatus.FAILED.getName()).to(stepC)
                .end()
                .build();
    }
}
