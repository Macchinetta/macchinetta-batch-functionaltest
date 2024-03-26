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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.jobs.ch07.jobmanagement;

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
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.SimpleJvmExitCodeMapper;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.transaction.PlatformTransactionManager;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.JobExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanSummary;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch07.jobmanagement.CheckAmountItemProcessor;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch07.jobmanagement.JobExitCodeChangeListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for CustomizedJobExitCodeChunkJob.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan(value = {"jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
    "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch07.jobmanagement"}, scopedProxy = ScopedProxyMode.TARGET_CLASS)
@MapperScan(basePackages = "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.plan", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class CustomizedJobExitCodeChunkJobConfig {

    @Bean
    public MyBatisCursorItemReader<SalesPlanSummary> summarizeDetails(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory) {
        return new MyBatisCursorItemReaderBuilder<SalesPlanSummary>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .queryId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.plan.SalesPlanDetailRepository.summarizeDetails")
                .build();
    }
    
    @Bean
    public MyBatisBatchItemWriter<SalesPlanSummary> summaryWriter(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory,
            SqlSessionTemplate batchModeSqlSessionTemplate) {
        return new MyBatisBatchItemWriterBuilder<SalesPlanSummary>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .statementId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.plan.SalesPlanSummaryRepository.create")
                .sqlSessionTemplate(batchModeSqlSessionTemplate)
                .build();
    }
    
    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       MyBatisCursorItemReader<SalesPlanSummary> reader,
                       MyBatisBatchItemWriter<SalesPlanSummary> writer,
                       CheckAmountItemProcessor processor) {
        return new StepBuilder("customizedJobExitCodeChunkJob.step01",
                jobRepository)
                .<SalesPlanSummary, SalesPlanSummary>chunk(10,
                        transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job customizedJobExitCodeChunkJob(JobRepository jobRepository,
                                                 Step step01,
                                                 JobExecutionLoggingListener jobExecutionLoggingListener,
                                                 JobExitCodeChangeListener jobExitCodeChangeListener) {
        return new JobBuilder("customizedJobExitCodeChunkJob",
                jobRepository)
                .start(step01)
                .listener(jobExecutionLoggingListener)
                .listener(jobExitCodeChangeListener)
                .build();
    }
    
    @Bean
    public SimpleJvmExitCodeMapper exitCodeMapper() {
        SimpleJvmExitCodeMapper exitCodeMapper = new SimpleJvmExitCodeMapper();
        Map<String, Integer> mapping = new HashMap<>();
        mapping.put("NOOP", 0);
        mapping.put("COMPLETED", 0);
        mapping.put("STOPPED", 255);
        mapping.put("FAILED", 255);
        mapping.put("UNKNOWN", 255);
        mapping.put("JOB COMPLETED WITH SKIPS", 100);
        exitCodeMapper.setMapping(mapping);
        
        return exitCodeMapper;
    }
}
