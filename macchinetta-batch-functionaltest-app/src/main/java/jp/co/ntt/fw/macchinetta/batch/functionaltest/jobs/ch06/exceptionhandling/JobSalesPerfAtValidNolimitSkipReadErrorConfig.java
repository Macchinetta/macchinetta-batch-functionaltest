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
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.core.step.skip.ExceptionClassifierSkipPolicy;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemReaderException;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.JobExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.exceptionhandling.AmountCheckProcessor;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.exceptionhandling.SkipLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for JobSalesPerfAtValidNolimitSkipReadError job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan({ "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.exceptionhandling" })
@MapperScan(value = "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.performance", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class JobSalesPerfAtValidNolimitSkipReadErrorConfig {

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
    public AmountCheckProcessor amountCheckProcessor() {
        AmountCheckProcessor processor = new AmountCheckProcessor();
        processor.setAmountLimit(BigDecimal.valueOf(10000));
        return processor;
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
    public ExceptionClassifierSkipPolicy skipPolicy(
            AlwaysSkipItemSkipPolicy alwaysSkip) {
        ExceptionClassifierSkipPolicy skipPolicy = new ExceptionClassifierSkipPolicy();
        Map<Class<? extends Throwable>, SkipPolicy> policyMap = new HashMap<>();
        policyMap.put(ItemReaderException.class, alwaysSkip);
        skipPolicy.setPolicyMap(policyMap);
        return skipPolicy;
    }
    
    @Bean
    public AlwaysSkipItemSkipPolicy alwaysSkip() {
        return new AlwaysSkipItemSkipPolicy();
    }
    
    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       @Qualifier("detailCSVReader") ItemReader<SalesPerformanceDetail> reader,
                       @Qualifier("amountCheckProcessor") ItemProcessor<SalesPerformanceDetail, SalesPerformanceDetail> processor,
                       @Qualifier("detailWriter") ItemWriter<SalesPerformanceDetail> writer,
                       SkipLoggingListener skipLoggingListener,
                       ExceptionClassifierSkipPolicy skipPolicy) {
        return new StepBuilder("jobSalesPerfAtValidNolimitSkipReadError.step01",
                jobRepository)
                .<SalesPerformanceDetail, SalesPerformanceDetail> chunk(10,
                        transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skipPolicy(skipPolicy)
                .listener(skipLoggingListener)
                .build();
    }

    @Bean
    public Job jobSalesPerfAtValidNolimitSkipReadError(JobRepository jobRepository,
                                              Step step01,
                                              JobExecutionLoggingListener listener) {
        return new JobBuilder("jobSalesPerfAtValidNolimitSkipReadError", jobRepository)
                .start(step01)
                .listener(listener)
                .build();
    }
}
