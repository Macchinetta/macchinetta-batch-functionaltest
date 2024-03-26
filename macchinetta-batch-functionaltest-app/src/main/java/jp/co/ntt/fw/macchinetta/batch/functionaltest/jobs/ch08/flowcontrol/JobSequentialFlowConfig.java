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
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.builder.TaskletStepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.flowcontrol.SequentialFlowTasklet;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for JobSequentialFlow job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
public class JobSequentialFlowConfig {

    @Bean
    @StepScope
    public SequentialFlowTasklet sequentialFlowTasklet(
            @Value("#{jobParameters['failExecutionStep']}") String failExecutionStep) {
        SequentialFlowTasklet tasklet = new SequentialFlowTasklet();
        tasklet.setFailExecutionStep(failExecutionStep);
        return tasklet;
    }

    TaskletStepBuilder parentStepBuilder(String stepName,
                                         JobRepository jobRepository,
                                         SequentialFlowTasklet tasklet,
                                         PlatformTransactionManager transactionManager) {
        return new StepBuilder(stepName, jobRepository)
                .tasklet(tasklet, transactionManager);
    }

    @Bean
    public Step step01(JobRepository jobRepository,
                       SequentialFlowTasklet tasklet,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return parentStepBuilder("jobSequentialFlow.step1", jobRepository,
                tasklet, transactionManager)
                .build();
    }

    @Bean
    public Step step02(JobRepository jobRepository,
                       SequentialFlowTasklet tasklet,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return parentStepBuilder("jobSequentialFlow.step2", jobRepository,
                tasklet, transactionManager)
                .build();
    }

    @Bean
    public Step step03(JobRepository jobRepository,
                       SequentialFlowTasklet tasklet,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return parentStepBuilder("jobSequentialFlow.step3", jobRepository,
                tasklet, transactionManager)
                .build();
    }

    @Bean
    public Step step01outer(JobRepository jobRepository,
                            SequentialFlowTasklet tasklet,
                            @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return parentStepBuilder("jobSequentialOuterFlow.step1", jobRepository,
                tasklet, transactionManager)
                .build();
    }

    @Bean
    public Step step02outer(JobRepository jobRepository,
                            SequentialFlowTasklet tasklet,
                            @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return parentStepBuilder("jobSequentialOuterFlow.step2", jobRepository,
                tasklet, transactionManager)
                .build();
    }

    @Bean
    public Step step03outer(JobRepository jobRepository,
                            SequentialFlowTasklet tasklet,
                            @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return parentStepBuilder("jobSequentialOuterFlow.step3", jobRepository,
                tasklet, transactionManager)
                .build();
    }

    @Bean
    public Job jobSequentialFlow(JobRepository jobRepository,
                                 @Qualifier("step01") Step step01,
                                 @Qualifier("step02") Step step02,
                                 @Qualifier("step03") Step step03) {
        Flow flow = new FlowBuilder<Flow>("jobSequentialFlow")
                .from(step01)
                .next(step02)
                .next(step03)
                .build();

        return new JobBuilder("jobSequentialFlow", jobRepository)
                .start(flow)
                .end()
                .build();
    }

    @Bean
    public Job jobSequentialOuterFlow(JobRepository jobRepository,
                                      @Qualifier("step01outer") Step step01outer,
                                      @Qualifier("step02outer") Step step02outer,
                                      @Qualifier("step03outer") Step step03outer) {
        Flow flow = new FlowBuilder<Flow>("jobSequentialOuterFlow")
                .from(step01outer)
                .next(step02outer)
                .next(step03outer)
                .build();
        return new JobBuilder("jobSequentialOuterFlow", jobRepository)
                .start(flow)
                .end()
                .build();
    }
}
