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

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.mybatis.spring.batch.builder.MyBatisBatchItemWriterBuilder;
import org.mybatis.spring.batch.builder.MyBatisCursorItemReaderBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.JobExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceSummary;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.asyncjobwithdb.listener.DBPollingJobListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for AsyncJobMultipleStartupConfirmJob job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan({ "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.asyncjobwithdb.listener" })
@MapperScan(value = "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.performance", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class AsyncJobMultipleStartupConfirmJobConfig {

    @Bean
    public MyBatisCursorItemReader<SalesPerformanceDetail> summarizeDetails(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory) {
        return new MyBatisCursorItemReaderBuilder<SalesPerformanceDetail>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .queryId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.performance.SalesPerformanceDetailRepository.summarizeDetails")
                .build();
    }

    @Bean
    public MyBatisBatchItemWriter<SalesPerformanceSummary> summaryWriter(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory,
            SqlSessionTemplate batchModeSqlSessionTemplate) {
        return new MyBatisBatchItemWriterBuilder<SalesPerformanceSummary>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .statementId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.performance.SalesPerformanceSummaryRepository.create")
                .sqlSessionTemplate(batchModeSqlSessionTemplate)
                .build();
    }

    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       ItemReader<SalesPerformanceDetail> reader,
                       ItemWriter<SalesPerformanceSummary> writer) {
        return new StepBuilder("asyncJobMultipleStartupConfirmJob.step01",
                jobRepository)
                .<SalesPerformanceDetail, SalesPerformanceSummary>chunk(10,
                        transactionManager)
                .reader(reader)
                .writer(writer)
                .build();
    }

    @Bean
    public Job asyncJobMultipleStartupConfirmJob(JobRepository jobRepository,
                                                 Step step01,
                                                 DBPollingJobListener dbPollingJobListener,
                                                 JobExecutionLoggingListener jobExecutionLoggingListener) {
        return new JobBuilder("asyncJobMultipleStartupConfirmJob",
                jobRepository)
                .start(step01)
                .listener(dbPollingJobListener)
                .listener(jobExecutionLoggingListener)
                .build();
    }
}
