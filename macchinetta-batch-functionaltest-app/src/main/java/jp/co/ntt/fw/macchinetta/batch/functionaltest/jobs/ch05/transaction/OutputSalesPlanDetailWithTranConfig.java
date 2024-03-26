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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.jobs.ch05.transaction;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.component.CausingErrorItemWriter;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for OutputSalesPlanDetailWithTran job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan("jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common")
@MapperScan(basePackages = "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.performance", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class OutputSalesPlanDetailWithTranConfig {

    @Bean
    @StepScope
    public MyBatisCursorItemReader<SalesPerformanceDetail> summarizeDetails(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory,
            @Value("#{jobParameters['branchId']}") String branchId) {
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("branchId", branchId);
        return new MyBatisCursorItemReaderBuilder<SalesPerformanceDetail>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .parameterValues(parameterValues)
                .queryId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.performance.InvoiceRepository.summarizeInvoice")
                .build();
    }
    
    @Bean
    @StepScope
    public FlatFileItemWriter<SalesPerformanceDetail> summaryWriter(
            @Value("#{jobParameters['outputFile']}") File outputFile) {
        BeanWrapperFieldExtractor<SalesPerformanceDetail> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[] {"branchId", "year", "month", "customerId", "amount"});
        DelimitedLineAggregator<SalesPerformanceDetail> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setFieldExtractor(fieldExtractor);
        return new FlatFileItemWriterBuilder<SalesPerformanceDetail>()
                .name(ClassUtils.getShortName(FlatFileItemWriter.class))
                .resource(new FileSystemResource(outputFile))
                .lineAggregator(lineAggregator)
                .build();
    }
    
    @Bean
    @StepScope
    public CausingErrorItemWriter causingErrorItemWriter() {
        CausingErrorItemWriter causingErrorItemWriter = new CausingErrorItemWriter();
        causingErrorItemWriter.setErrorOccurNumber(25);
        return causingErrorItemWriter;
    }
    
    @Bean
    @StepScope
    public CompositeItemWriter<SalesPerformanceDetail> compositeWriter(
            FlatFileItemWriter<SalesPerformanceDetail> summaryWriter,
            CausingErrorItemWriter causingErrorItemWriter) {
        List<ItemWriter<? super SalesPerformanceDetail>> delegates = new ArrayList<>();
        delegates.add(summaryWriter);
        delegates.add(causingErrorItemWriter);
        return new CompositeItemWriterBuilder<SalesPerformanceDetail>()
                .delegates(delegates)
                .build();
    }
    
    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       MyBatisCursorItemReader<SalesPerformanceDetail> summarizeDetails,
                       CompositeItemWriter<SalesPerformanceDetail> compositeWriter) {
        return new StepBuilder("outputSalesPlanDetailWithTran.step01",
                jobRepository)
                .<SalesPerformanceDetail, SalesPerformanceDetail> chunk(10, transactionManager)
                .reader(summarizeDetails)
                .writer(compositeWriter)
                .build();
    }
    
    @Bean
    public Job outputSalesPlanDetailWithTran(JobRepository jobRepository,
                                            Step step01) {
        return new JobBuilder("outputSalesPlanDetailWithTran",jobRepository)
                .start(step01)
                .build();
    }
}
