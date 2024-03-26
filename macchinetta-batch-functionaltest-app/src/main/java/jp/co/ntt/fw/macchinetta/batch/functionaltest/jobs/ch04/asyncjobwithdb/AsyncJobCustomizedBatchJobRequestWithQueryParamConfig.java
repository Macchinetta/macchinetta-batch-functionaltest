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

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.JobExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.asyncjobwithdb.CustomizedBatchJobRequestWithQueryParamConfirmTask;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for AsyncJobCustomizedBatchJobRequestWithQueryParam job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan(value = { "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.asyncjobwithdb" }, scopedProxy = ScopedProxyMode.TARGET_CLASS)
@MapperScan(value = "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.performance", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class AsyncJobCustomizedBatchJobRequestWithQueryParamConfig {

    @Bean
    public Step step01(JobRepository jobRepository,
                       CustomizedBatchJobRequestWithQueryParamConfirmTask tasklet,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("asyncJobCustomizedBatchJobRequestWithQueryParam.step01",
                jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Job asyncJobCustomizedBatchJobRequestWithQueryParam(JobRepository jobRepository,
                                                 Step step01,
                                                 JobExecutionLoggingListener listener) {
        return new JobBuilder("asyncJobCustomizedBatchJobRequestWithQueryParam",
                jobRepository)
                .start(step01)
                .listener(listener)
                .build();
    }
}
