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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.jobs.ch05.dbaccess;

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
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.JobExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Customer;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for UpdateMapperAndItemWriterBatchModeJob job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan({ "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.dbaccess" })
@MapperScan(basePackages = {
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.dbaccess.repository" }, sqlSessionTemplateRef = "batchModeSqlSessionTemplate")
public class UpdateMapperAndItemWriterBatchModeJobConfig {
    
    @Bean
    public MyBatisCursorItemReader<Customer> reader(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory) {
        return new MyBatisCursorItemReaderBuilder<Customer>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .queryId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.dbaccess.repository.DBAccessCustomerRepository.findAll")
                .build();
    }

    @Bean
    public MyBatisBatchItemWriter<Customer> writer(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory,
            @Qualifier("batchModeSqlSessionTemplate") SqlSessionTemplate batchModeSqlSessionTemplate) {
        return new MyBatisBatchItemWriterBuilder<Customer>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .statementId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.dbaccess.repository.DBAccessCustomerRepository.updateAddress")
                .sqlSessionTemplate(batchModeSqlSessionTemplate)
                .build();
    }
    
    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       MyBatisCursorItemReader<Customer> reader,
                       @Qualifier("updateCustomerItemProcessor") ItemProcessor<Customer, Customer> processor,
                       MyBatisBatchItemWriter<Customer> writer) {
        return new StepBuilder("updateMapperAndItemWriterBatchModeJob.step01",
                jobRepository)
                .<Customer, Customer> chunk(10, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job updateMapperAndItemWriterBatchModeJob(JobRepository jobRepository,
                                            Step step01,
                                            JobExecutionLoggingListener listener) {
        return new JobBuilder("updateMapperAndItemWriterBatchModeJob",
                jobRepository)
                .start(step01)
                .listener(listener)
                .build();
    }
}
