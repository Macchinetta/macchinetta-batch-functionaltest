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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.jobs.ch05.fileaccess;

import java.io.File;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.mybatis.spring.batch.builder.MyBatisCursorItemReaderBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.MultiResourceItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.builder.MultiResourceItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
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
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Customer;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.module.CustomerListResourceSuffixCreator;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for JobWriteMultipleCsv job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan("jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common")
@MapperScan(basePackages = "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.mst", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class JobWriteMultipleCsvConfig {

    @Bean
    @StepScope
    public MyBatisCursorItemReader<Customer> reader(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory) {
        return new MyBatisCursorItemReaderBuilder<Customer>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .queryId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.mst.CustomerRepository.findAll")
                .build();
    }
    
    @Bean
    public FlatFileItemWriter<Customer> writer() {
        final BeanWrapperFieldExtractor<Customer> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(
                new String[] { "customerId", "customerName", "customerAddress", "customerTel", "chargeBranchId" });
        final DelimitedLineAggregator<Customer> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setFieldExtractor(fieldExtractor);
        return new FlatFileItemWriterBuilder<Customer>()
                .name(ClassUtils.getShortName(FlatFileItemWriter.class))
                .transactional(false)
                .lineAggregator(lineAggregator)
                .build();
    }
    
    @Bean
    public CustomerListResourceSuffixCreator customerListResourceSuffixCreator() {
        return new CustomerListResourceSuffixCreator();
    }
    
    @Bean
    @StepScope
    public MultiResourceItemWriter<Customer> multiResourceItemWriter(
            @Value("#{jobParameters['outputDir']}") File outputDir,
            CustomerListResourceSuffixCreator customerListResourceSuffixCreator,
            FlatFileItemWriter<Customer> writer) {
        return new MultiResourceItemWriterBuilder<Customer>()
                .name(ClassUtils.getShortName(MultiResourceItemWriter.class))
                .resource(new FileSystemResource(outputDir))
                .resourceSuffixCreator(customerListResourceSuffixCreator)
                .itemCountLimitPerResource(4)
                .delegate(writer)
                .build();
    }
    
    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       MyBatisCursorItemReader<Customer> reader,
                       MultiResourceItemWriter<Customer> multiResourceItemWriter) {
        return new StepBuilder("jobWriteMultipleCsv.step01",
                jobRepository)
                .<Customer, Customer> chunk(5, transactionManager)
                .reader(reader)
                .writer(multiResourceItemWriter)
                .build();
    }

    @Bean
    public Job jobWriteMultipleCsv(JobRepository jobRepository,
                                            Step step01,
                                            JobExecutionLoggingListener listener) {
        return new JobBuilder("jobWriteMultipleCsv",jobRepository)
                .start(step01)
                .listener(listener)
                .build();
    }
}
