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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.jobs.ch06.exceptionhandling;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.mybatis.spring.batch.builder.MyBatisBatchItemWriterBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.JobExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.exceptionhandling.RetryableAmountCheckProcessor;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for JobSalesPerfWithRetryPolicy job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan({ "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.exceptionhandling" })
@MapperScan(value = "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.performance", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class JobSalesPerfWithRetryPolicyConfig {

    @Bean
    @StepScope
    public FlatFileItemReader<SalesPerformanceDetail> detailCSVReader(
            @Value("#{jobParameters['inputFile']}") File inputFile) {
        final DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("branchId", "year", "month", "customerId", "amount");
        final BeanWrapperFieldSetMapper<SalesPerformanceDetail> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(SalesPerformanceDetail.class);
        final DefaultLineMapper<SalesPerformanceDetail> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        return new FlatFileItemReaderBuilder<SalesPerformanceDetail>()
                .name(ClassUtils.getShortName(FlatFileItemReader.class))
                .resource(new FileSystemResource(inputFile))
                .lineMapper(lineMapper)
                .build();
    }
    
    @Bean
    @StepScope
    public RetryableAmountCheckProcessor amountCheckProcessor(
            @Value("#{jobParameters['adjust-times']}") int adjustTimes,
            SimpleRetryPolicy retryPolicy) {
        RetryableAmountCheckProcessor processor = new RetryableAmountCheckProcessor();
        processor.setAmountLimit(BigDecimal.valueOf(10000));
        processor.setAdjustTimes(adjustTimes);
        processor.setRetryPolicy(retryPolicy);
        return processor;
    }
    
    @Bean
    public SimpleRetryPolicy retryPolicy(
            Map<Class<? extends Throwable>, Boolean> exceptionMap) {
        return new SimpleRetryPolicy(3, exceptionMap);
    }
    
    @Bean
    public Map<Class<? extends Throwable>, Boolean> exceptionMap() {
        Map<Class<? extends Throwable>, Boolean> exceptionMap = new HashMap<>();
        exceptionMap.put(UnsupportedOperationException.class, true);
        return exceptionMap;
    }
    
    @Bean
    public MyBatisBatchItemWriter<SalesPerformanceDetail> detailWriter(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory,
            SqlSessionTemplate batchModeSqlSessionTemplate) {
        return new MyBatisBatchItemWriterBuilder<SalesPerformanceDetail>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .statementId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.performance.SalesPerformanceDetailRepository.create")
                .sqlSessionTemplate(batchModeSqlSessionTemplate)
                .build();
    }
    
    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       @Qualifier("detailCSVReader") ItemReader<SalesPerformanceDetail> reader,
                       @Qualifier("amountCheckProcessor") ItemProcessor<SalesPerformanceDetail, SalesPerformanceDetail> processor,
                       @Qualifier("detailWriter") ItemWriter<SalesPerformanceDetail> writer) {
        return new StepBuilder("jobSalesPerfWithRetryPolicy.step01",
                jobRepository)
                .<SalesPerformanceDetail, SalesPerformanceDetail> chunk(10,
                        transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job jobSalesPerfWithRetryPolicy(JobRepository jobRepository,
                                              Step step01,
                                              JobExecutionLoggingListener listener) {
        return new JobBuilder("jobSalesPerfWithRetryPolicy", jobRepository)
                .start(step01)
                .listener(listener)
                .build();
    }
}
