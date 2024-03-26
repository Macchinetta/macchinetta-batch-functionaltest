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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.jobs.ch08.parallelandmultiple;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.mybatis.spring.batch.builder.MyBatisBatchItemWriterBuilder;
import org.mybatis.spring.batch.builder.MyBatisCursorItemReaderBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceSummary;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanSummary;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.listener.StepExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for ParallelSummarizeJob.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan({ "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.listener" })
@MapperScan(value = { "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.plan",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.performance" }, sqlSessionFactoryRef = "jobSqlSessionFactory")
public class ParallelSummarizeJobConfig {

    @Bean
    public MyBatisCursorItemReader<SalesPlanDetail> planReader(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory) {
        return new MyBatisCursorItemReaderBuilder<SalesPlanDetail>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .queryId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.plan.SalesPlanDetailRepository.summarizeDetails")
                .build();
    }

    @Bean
    public MyBatisBatchItemWriter<SalesPlanSummary> planWriter(
            SqlSessionTemplate batchModeSqlSessionTemplate) {
        return new MyBatisBatchItemWriterBuilder<SalesPlanSummary>()
                .statementId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.plan.SalesPlanSummaryRepository.create")
                .sqlSessionTemplate(batchModeSqlSessionTemplate).build();
    }

    @Bean
    public MyBatisCursorItemReader<SalesPerformanceDetail> performanceReader(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory) {
        return new MyBatisCursorItemReaderBuilder<SalesPerformanceDetail>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .queryId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.performance.SalesPerformanceDetailRepository.summarizeDetails")
                .build();
    }

    @Bean
    public MyBatisBatchItemWriter<SalesPerformanceSummary> performanceWriter(
            SqlSessionTemplate batchModeSqlSessionTemplate) {
        return new MyBatisBatchItemWriterBuilder<SalesPerformanceSummary>()
                .statementId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.performance.SalesPerformanceSummaryRepository.create")
                .sqlSessionTemplate(batchModeSqlSessionTemplate)
                .build();
    }

    @Bean
    public Step step1(JobRepository jobRepository,
                      StepExecutionLoggingListener listener,
                      @Qualifier("planReader") ItemReader<SalesPlanDetail> reader,
                      @Qualifier("planWriter") ItemWriter<SalesPlanSummary> writer,
                      @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("parallelSummarizeJob.step.plan", jobRepository)
                .<SalesPlanDetail, SalesPlanSummary> chunk(1,
                        transactionManager)
                .listener(listener)
                .reader(reader)
                .writer(writer)
                .build();
    }

    @Bean
    public Step step2(JobRepository jobRepository,
                      StepExecutionLoggingListener listener,
                      @Qualifier("performanceReader") ItemReader<SalesPerformanceDetail> reader,
                      @Qualifier("performanceWriter") ItemWriter<SalesPerformanceSummary> writer,
                      @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("parallelSummarizeJob.step.performance",
                jobRepository)
                .<SalesPerformanceDetail, SalesPerformanceSummary> chunk(1,
                        transactionManager)
                .listener(listener)
                .reader(reader)
                .writer(writer)
                .build();
    }

    @Bean
    public Flow flow1(@Qualifier("step1") Step step1) {
        return new FlowBuilder<SimpleFlow>("flow1")
                .start(step1)
                .build();
    }

    @Bean
    public Flow flow2(@Qualifier("step2") Step step2) {
        return new FlowBuilder<SimpleFlow>("flow2")
                .start(step2)
                .build();
    }

    @Bean
    public Flow splitFlow(
            @Qualifier("parallelTaskExecutor") TaskExecutor taskExecutor,
            @Qualifier("flow1") Flow flow1,
            @Qualifier("flow2") Flow flow2) {
        return new FlowBuilder<SimpleFlow>("parallelSummarizeJob.split")
                .split(taskExecutor)
                .add(flow1, flow2)
                .build();
    }

    @Bean
    public Job parallelSummarizeJob(JobRepository jobRepository,
                                    @Qualifier("splitFlow") Flow splitFlow) {
        return new JobBuilder("parallelSummarizeJob", jobRepository)
                .start(splitFlow)
                .end()
                .build();
    }

    @Bean
    public TaskExecutor parallelTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setQueueCapacity(10);
        return executor;
    }
}
