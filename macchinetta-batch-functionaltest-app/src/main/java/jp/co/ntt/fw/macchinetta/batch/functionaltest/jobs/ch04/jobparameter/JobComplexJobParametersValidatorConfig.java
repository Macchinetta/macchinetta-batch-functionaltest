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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.jobs.ch04.jobparameter;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.transaction.PlatformTransactionManager;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.jobparameter.ComplexJobParametersValidator;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.jobparameter.RefWithDefaultInJavaTasklet;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for JobComplexJobParametersValidator job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan(value = "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.jobparameter", scopedProxy = ScopedProxyMode.TARGET_CLASS)
public class JobComplexJobParametersValidatorConfig {
    
    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       RefWithDefaultInJavaTasklet refWithDefaultInJavaTasklet) {
        return new StepBuilder("jobComplexJobParametersValidator.step01", jobRepository)
                .tasklet(refWithDefaultInJavaTasklet, transactionManager)
                .build();
    }
    
    @Bean
    public Job jobComplexJobParametersValidator(JobRepository jobRepository,
                                       Step step01) {
        return new JobBuilder("jobComplexJobParametersValidator",
                jobRepository)
                .start(step01)
                .validator(new ComplexJobParametersValidator())
                .build();
    }

}
