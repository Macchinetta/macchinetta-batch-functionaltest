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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.jobs.ch06.reprocessing;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.JobExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.LoggingItemReaderListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for RestartOnConditionBasisJob.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan(value = { "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.reprocessing" }, scopedProxy = ScopedProxyMode.TARGET_CLASS)
@MapperScan(value = "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.reprocessing.repository", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class RestartOnConditionBasisJobConfig {

    @Bean
    public MyBatisCursorItemReader<SalesPlanDetail> reader(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory) {
        return new MyBatisCursorItemReaderBuilder<SalesPlanDetail>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .queryId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.reprocessing.repository.RestartOnConditionRepository.findByZeroOrLessAmount")
                .build();
    }
    
    @Bean
    public MyBatisBatchItemWriter<SalesPlanDetail> dbWriter(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory,
            SqlSessionTemplate batchModeSqlSessionTemplate) {
        return new MyBatisBatchItemWriterBuilder<SalesPlanDetail>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .statementId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.reprocessing.repository.RestartOnConditionRepository.update")
                .sqlSessionTemplate(batchModeSqlSessionTemplate)
                .build();
    }
    
    @Bean
    @StepScope
    public FlatFileItemWriter<SalesPlanDetail> fileWriter(
            @Value("#{jobParameters['outputFile']}") File outputFile) {
        BeanWrapperFieldExtractor<SalesPlanDetail> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[] {"branchId", "year", "month", "customerId", "amount"});
        DelimitedLineAggregator<SalesPlanDetail> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setFieldExtractor(fieldExtractor);
        return new FlatFileItemWriterBuilder<SalesPlanDetail>()
                .name(ClassUtils.getShortName(FlatFileItemWriter.class))
                .resource(new FileSystemResource(outputFile))
                .lineAggregator(lineAggregator)
                .append(true)
                .build();
    }
    
    @Bean(destroyMethod="")
    public CompositeItemWriter<SalesPlanDetail> compositeWriter(
            @Qualifier("fileWriter") FlatFileItemWriter<SalesPlanDetail> fileWriter,
            @Qualifier("dbWriter") MyBatisBatchItemWriter<SalesPlanDetail> dbWriter) throws Exception {
        
        List<ItemWriter<? super SalesPlanDetail>> list = new ArrayList<>();
        list.add(fileWriter);
        list.add(dbWriter);
        
        return new CompositeItemWriterBuilder<SalesPlanDetail>()
                .delegates(list)
                .build();
    }
    
    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       MyBatisCursorItemReader<SalesPlanDetail> reader,
                       @Qualifier("amountUpdateItemProcessor") ItemProcessor<SalesPlanDetail, SalesPlanDetail> processor,
                       @Qualifier("compositeWriter") ItemWriter<SalesPlanDetail> compositeWriter,
                       LoggingItemReaderListener listener) {
        return new StepBuilder("restartOnConditionBasisJob.step01",
                jobRepository)
                .<SalesPlanDetail, SalesPlanDetail> chunk(10, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(compositeWriter)
                .listener(listener)
                .build();
    }

    @Bean
    public Job restartOnConditionBasisJob(JobRepository jobRepository,
                                            Step step01,
                                            JobExecutionLoggingListener listener) {
        return new JobBuilder("restartOnConditionBasisJob",
                jobRepository)
                .start(step01)
                .listener(listener)
                .build();
    }
}
