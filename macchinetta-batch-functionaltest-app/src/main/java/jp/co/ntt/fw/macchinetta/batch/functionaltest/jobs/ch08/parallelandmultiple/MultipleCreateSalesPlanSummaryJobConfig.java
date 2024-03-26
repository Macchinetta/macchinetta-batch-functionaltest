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

import java.util.HashMap;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.JobExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceSummary;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanSummary;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.listener.StepExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.multiple.AddProfitsItemProcessor;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.multiple.SalesDataPartitioner;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for MultipleCreateSalesPlanSummaryJob.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan(value = { "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.listener",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.multiple"}, scopedProxy = ScopedProxyMode.TARGET_CLASS)
@MapperScan(value = "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.repository", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class MultipleCreateSalesPlanSummaryJobConfig {

    @Bean
    @StepScope
    public MyBatisCursorItemReader<SalesPerformanceSummary> reader(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory,
            @Value("#{jobParameters['year']}") Integer year,
            @Value("#{jobParameters['month']}") Integer month,
            @Value("#{stepExecutionContext['dataSize']}") Integer dataSize,
            @Value("#{stepExecutionContext['offset']}") Integer offset) {
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("year", year);
        parameterValues.put("month", month);
        parameterValues.put("dataSize", dataSize);
        parameterValues.put("offset", offset);
        return new MyBatisCursorItemReaderBuilder<SalesPerformanceSummary>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .queryId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.repository.SalesSummaryRepository.findByYearAndMonth")
                .parameterValues(parameterValues)
                .build();
    }
    
    @Bean
    public MyBatisBatchItemWriter<SalesPlanSummary> writer(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory,
            SqlSessionTemplate batchModeSqlSessionTemplate) {
        return new MyBatisBatchItemWriterBuilder<SalesPlanSummary>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .statementId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.repository.SalesSummaryRepository.create")
                .sqlSessionTemplate(batchModeSqlSessionTemplate)
                .build();
    }
    
    @Bean
    public Step stepWorker(JobRepository jobRepository,
                            StepExecutionLoggingListener listener,
                            MyBatisCursorItemReader<SalesPerformanceSummary> reader,
                            MyBatisBatchItemWriter<SalesPlanSummary> writer,
                            AddProfitsItemProcessor addProfitsItemProcessor,
                            @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("multipleCreateSalesPlanSummaryJob.worker", jobRepository)
                .<SalesPerformanceSummary, SalesPlanSummary> chunk(10,
                        transactionManager)
                .listener(listener)
                .reader(reader)
                .processor(addProfitsItemProcessor)
                .writer(writer)
                .build();
    }
    
    @Bean
    public PartitionHandler partitionHandler(
            TaskExecutor parallelTaskExecutor,
            @Value("${grid.size}") int gridSize,
            @Qualifier("stepWorker") Step stepWorker) {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setTaskExecutor(parallelTaskExecutor);
        handler.setStep(stepWorker);
        handler.setGridSize(gridSize);
        return handler;
    }
    
    @Bean
    public Step stepManager(JobRepository jobRepository,
                            SalesDataPartitioner salesDataPartitioner,
                            PartitionHandler partitionHandler) {
        return new StepBuilder("multipleCreateSalesPlanSummaryJob.manager", jobRepository)
                .partitioner("multipleCreateSalesPlanSummaryJob.worker", salesDataPartitioner)
                .partitionHandler(partitionHandler)
                .build();
    }
    
    @Bean
    public Job multipleCreateSalesPlanSummaryJob(JobRepository jobRepository,
                                           JobExecutionLoggingListener listener,
                                           @Qualifier("stepManager") Step stepManager) {
        return new JobBuilder("multipleCreateSalesPlanSummaryJob", jobRepository)
                .start(stepManager)
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
