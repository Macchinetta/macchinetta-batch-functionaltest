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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.mybatis.spring.batch.builder.MyBatisBatchItemWriterBuilder;
import org.mybatis.spring.batch.builder.MyBatisCursorItemReaderBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.JobExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.listener.StepExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.multiple.BranchPartitioner;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for MultipleInvoiceSummarizeJob.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan({ "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.listener",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.multiple" })
@MapperScan(value = {
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.repository",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.performance" }, sqlSessionFactoryRef = "jobSqlSessionFactory")
public class MultipleInvoiceSummarizeJobConfig {

    @Bean
    @StepScope
    public MyBatisCursorItemReader<SalesPerformanceDetail> reader(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory,
            @Value("#{stepExecutionContext['branchId']}") String branchId) {
        final Map<String, Object> parameterValues = new LinkedHashMap<>();
        parameterValues.put("branchId", branchId);
        return new MyBatisCursorItemReaderBuilder<SalesPerformanceDetail>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .queryId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.performance.InvoiceRepository.summarizeInvoice")
                .parameterValues(parameterValues)
                .build();
    }

    @Bean
    public MyBatisBatchItemWriter<SalesPerformanceDetail> writer(
            SqlSessionTemplate batchModeSqlSessionTemplate) {
        return new MyBatisBatchItemWriterBuilder<SalesPerformanceDetail>()
                .statementId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.performance.SalesPerformanceDetailRepository.create")
                .sqlSessionTemplate(batchModeSqlSessionTemplate)
                .build();
    }

    @Bean
    public Step step1(JobRepository jobRepository,
                      StepExecutionLoggingListener listener,
                      @Qualifier("reader") ItemReader<SalesPerformanceDetail> reader,
                      @Qualifier("writer") ItemWriter<SalesPerformanceDetail> writer,
                      @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("multipleInvoiceSummarizeJob.worker",
                jobRepository)
                .<SalesPerformanceDetail, SalesPerformanceDetail> chunk(1,
                        transactionManager)
                .listener(listener)
                .reader(reader)
                .writer(writer)
                .build();
    }

    @Bean
    public PartitionHandler partitionHandler(
            @Qualifier("parallelTaskExecutor") TaskExecutor taskExecutor,
            @Value("${grid.size}") int gridSize,
            @Qualifier("step1") Step step1) {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setTaskExecutor(taskExecutor);
        handler.setStep(step1);
        handler.setGridSize(gridSize);
        return handler;
    }

    @Bean
    public Step step1Manager(JobRepository jobRepository,
                             BranchPartitioner partitioner,
                             PartitionHandler partitionHandlerandler) {
        return new StepBuilder("multipleInvoiceSummarizeJob.manager",
                jobRepository)
                .partitioner("multipleInvoiceSummarizeJob.worker", partitioner)
                .partitionHandler(partitionHandlerandler)
                .build();
    }

    @Bean
    public Job multipleInvoiceSummarizeJob(JobRepository jobRepository,
                                           JobExecutionLoggingListener listener,
                                           @Qualifier("step1Manager") Step step1Manager) {
        return new JobBuilder("multipleInvoiceSummarizeJob", jobRepository)
                .start(step1Manager)
                .listener(listener)
                .build();
    }

    @Bean
    public TaskExecutor parallelTaskExecutor(
            @Value("${thread.size}") int threadSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadSize);
        executor.setQueueCapacity(10);
        return executor;
    }
}
