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
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.transaction.PlatformTransactionManager;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.JobExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.dbaccess.SalesDTO;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.dbaccess.SalesItemProcessor;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for UseCompositeItemWriter job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan(value = { "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.dbaccess" }, scopedProxy = ScopedProxyMode.TARGET_CLASS)
@MapperScan(basePackages = {
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.dbaccess.repository" }, sqlSessionTemplateRef = "batchModeSqlSessionTemplate")
public class UseCompositeItemWriterConfig {
    
    @Bean
    public MyBatisCursorItemReader<SalesPlanDetail> reader(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory) {
        return new MyBatisCursorItemReaderBuilder<SalesPlanDetail>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .queryId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.dbaccess.repository.SalesRepository.findAll")
                .build();
    }

    @Bean
    public MyBatisBatchItemWriter<SalesDTO> planWriter(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory,
            @Qualifier("batchModeSqlSessionTemplate") SqlSessionTemplate batchModeSqlSessionTemplate) {
        return new MyBatisBatchItemWriterBuilder<SalesDTO>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .statementId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.dbaccess.repository.SalesRepository.update")
                .sqlSessionTemplate(batchModeSqlSessionTemplate)
                .build();
    }
    
    @Bean
    public MyBatisBatchItemWriter<SalesDTO> performanceWriter(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory,
            @Qualifier("batchModeSqlSessionTemplate") SqlSessionTemplate batchModeSqlSessionTemplate) {
        return new MyBatisBatchItemWriterBuilder<SalesDTO>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .statementId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.dbaccess.repository.SalesRepository.create")
                .sqlSessionTemplate(batchModeSqlSessionTemplate)
                .build();
    }
    
    @Bean
    public CompositeItemWriter<SalesDTO> writer(
            @Qualifier("performanceWriter") MyBatisBatchItemWriter<SalesDTO> performanceWriter,
            @Qualifier("planWriter") MyBatisBatchItemWriter<SalesDTO> planWriter) {
        return new CompositeItemWriter<SalesDTO>(performanceWriter, planWriter);
    }
    
    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       @Qualifier("reader") MyBatisCursorItemReader<SalesPlanDetail> reader,
                       @Qualifier("salesItemProcessor") SalesItemProcessor processor,
                       @Qualifier("writer") CompositeItemWriter<SalesDTO> writer) {
        return new StepBuilder("useCompositeItemWriter.step01",
                jobRepository)
                .<SalesPlanDetail, SalesDTO> chunk(3, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job useCompositeItemWriter(JobRepository jobRepository,
                                            Step step01,
                                            JobExecutionLoggingListener listener) {
        return new JobBuilder("useCompositeItemWriter",
                jobRepository)
                .start(step01)
                .listener(listener)
                .build();
    }

}
