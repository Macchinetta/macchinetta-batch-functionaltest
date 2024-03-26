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
import java.util.HashMap;
import java.util.Map;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.mapping.PatternMatchingCompositeLineMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
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
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.LoggingItemReaderListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.model.plan.SalesPlanDetailData;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.model.plan.SalesPlanDetailEnd;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.model.plan.SalesPlanDetailHeader;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.model.plan.SalesPlanDetailMultiFormatRecord;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.model.plan.SalesPlanDetailTrailer;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.module.MultiFormatItemProcessor;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for JobWriteMultiFormatCsv job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan({ "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.module"})
@MapperScan(basePackages = "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.plan", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class JobWriteMultiFormatCsvConfig {

    @Bean
    public DelimitedLineTokenizer headerDelimitedLineTokenizer() {
        DelimitedLineTokenizer headerDelimitedLineTokenizer = new DelimitedLineTokenizer();
        headerDelimitedLineTokenizer.setNames("record", "description");
        return headerDelimitedLineTokenizer;
    }
    
    @Bean
    public DelimitedLineTokenizer dataDelimitedLineTokenizer() {
        DelimitedLineTokenizer dataDelimitedLineTokenizer = new DelimitedLineTokenizer();
        dataDelimitedLineTokenizer.setNames("record", "branchId", "year", "month", "customerId", "amount");
        return dataDelimitedLineTokenizer;
    }
    
    @Bean
    public DelimitedLineTokenizer trailerDelimitedLineTokenizer() {
        DelimitedLineTokenizer trailerDelimitedLineTokenizer = new DelimitedLineTokenizer();
        trailerDelimitedLineTokenizer.setNames("record", "branchId", "number", "total");
        return trailerDelimitedLineTokenizer;
    }
    
    @Bean
    public DelimitedLineTokenizer endDelimitedLineTokenizer() {
        DelimitedLineTokenizer endDelimitedLineTokenizer = new DelimitedLineTokenizer();
        endDelimitedLineTokenizer.setNames("record", "headNum", "trailerNum", "total");
        return endDelimitedLineTokenizer;
    }
    
    @Bean
    public BeanWrapperFieldSetMapper<SalesPlanDetailHeader> headerBeanWrapperFieldSetMapper() {
        BeanWrapperFieldSetMapper<SalesPlanDetailHeader> headerBeanWrapperFieldSetMapper = new BeanWrapperFieldSetMapper<>();
        headerBeanWrapperFieldSetMapper.setTargetType(SalesPlanDetailHeader.class);
        return headerBeanWrapperFieldSetMapper;
    }
    
    @Bean
    public BeanWrapperFieldSetMapper<SalesPlanDetailData> dataBeanWrapperFieldSetMapper() {
        BeanWrapperFieldSetMapper<SalesPlanDetailData> dataBeanWrapperFieldSetMapper = new BeanWrapperFieldSetMapper<>();
        dataBeanWrapperFieldSetMapper.setTargetType(SalesPlanDetailData.class);
        return dataBeanWrapperFieldSetMapper;
    }
    
    @Bean
    public BeanWrapperFieldSetMapper<SalesPlanDetailTrailer> trailerBeanWrapperFieldSetMapper() {
        BeanWrapperFieldSetMapper<SalesPlanDetailTrailer> trailerBeanWrapperFieldSetMapper = new BeanWrapperFieldSetMapper<>();
        trailerBeanWrapperFieldSetMapper.setTargetType(SalesPlanDetailTrailer.class);
        return trailerBeanWrapperFieldSetMapper;
    }
    
    @Bean
    public BeanWrapperFieldSetMapper<SalesPlanDetailEnd> endBeanWrapperFieldSetMapper() {
        BeanWrapperFieldSetMapper<SalesPlanDetailEnd> endBeanWrapperFieldSetMapper = new BeanWrapperFieldSetMapper<>();
        endBeanWrapperFieldSetMapper.setTargetType(SalesPlanDetailEnd.class);
        return endBeanWrapperFieldSetMapper;
    }
    
    @Bean
    @StepScope
    public FlatFileItemReader<SalesPlanDetailMultiFormatRecord> reader(
            @Value("#{jobParameters['inputFile']}") File inputFile,
            @Qualifier("headerDelimitedLineTokenizer") LineTokenizer headerDelimitedLineTokenizer,
            @Qualifier("dataDelimitedLineTokenizer") LineTokenizer dataDelimitedLineTokenizer,
            @Qualifier("trailerDelimitedLineTokenizer") LineTokenizer trailerDelimitedLineTokenizer,
            @Qualifier("endDelimitedLineTokenizer") LineTokenizer endDelimitedLineTokenizer,
            @Qualifier("headerBeanWrapperFieldSetMapper") FieldSetMapper<SalesPlanDetailHeader> headerBeanWrapperFieldSetMapper,
            @Qualifier("dataBeanWrapperFieldSetMapper") FieldSetMapper<SalesPlanDetailData> dataBeanWrapperFieldSetMapper,
            @Qualifier("trailerBeanWrapperFieldSetMapper") FieldSetMapper<SalesPlanDetailTrailer> trailerBeanWrapperFieldSetMapper,
            @Qualifier("endBeanWrapperFieldSetMapper") FieldSetMapper<SalesPlanDetailEnd> endBeanWrapperFieldSetMapper) {
        Map<String, LineTokenizer> tokenizers = new HashMap<>();
        tokenizers.put("H*", headerDelimitedLineTokenizer);
        tokenizers.put("D*", dataDelimitedLineTokenizer);
        tokenizers.put("T*", trailerDelimitedLineTokenizer);
        tokenizers.put("E*", endDelimitedLineTokenizer);
        Map<String, FieldSetMapper> fieldSetMappers = new HashMap<>();
        fieldSetMappers.put("H*", headerBeanWrapperFieldSetMapper);
        fieldSetMappers.put("D*", dataBeanWrapperFieldSetMapper);
        fieldSetMappers.put("T*", trailerBeanWrapperFieldSetMapper);
        fieldSetMappers.put("E*", endBeanWrapperFieldSetMapper);
        final PatternMatchingCompositeLineMapper lineMapper = new PatternMatchingCompositeLineMapper();
        lineMapper.setTokenizers(tokenizers);
        lineMapper.setFieldSetMappers(fieldSetMappers);
        return new FlatFileItemReaderBuilder<SalesPlanDetailMultiFormatRecord>()
                .name(ClassUtils.getShortName(FlatFileItemReader.class))
                .lineMapper(lineMapper)
                .resource(new FileSystemResource(inputFile))
                .build();
    }

    
    @Bean
    public MultiFormatItemProcessor multiFormatItemProcessor() {
        return new MultiFormatItemProcessor();
    }
    
    @Bean
    public DelimitedLineAggregator<SalesPlanDetailMultiFormatRecord> headerDelimitedLineAggregator() {
        DelimitedLineAggregator<SalesPlanDetailMultiFormatRecord> headerDelimitedLineAggregator = new DelimitedLineAggregator<>();
        BeanWrapperFieldExtractor<SalesPlanDetailMultiFormatRecord> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[] {"record", "description"});
        headerDelimitedLineAggregator.setFieldExtractor(fieldExtractor);
        return headerDelimitedLineAggregator;
    }
    
    @Bean
    public DelimitedLineAggregator<SalesPlanDetailMultiFormatRecord> dataDelimitedLineAggregator() {
        DelimitedLineAggregator<SalesPlanDetailMultiFormatRecord> dataDelimitedLineAggregator = new DelimitedLineAggregator<>();
        BeanWrapperFieldExtractor<SalesPlanDetailMultiFormatRecord> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[] {"record", "branchId", "year", "month", "customerId", "amount"});
        dataDelimitedLineAggregator.setFieldExtractor(fieldExtractor);
        return dataDelimitedLineAggregator;
    }
    
    @Bean
    public DelimitedLineAggregator<SalesPlanDetailMultiFormatRecord> trailerDelimitedLineAggregator() {
        DelimitedLineAggregator<SalesPlanDetailMultiFormatRecord> trailerDelimitedLineAggregator = new DelimitedLineAggregator<>();
        BeanWrapperFieldExtractor<SalesPlanDetailMultiFormatRecord> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[] {"record", "branchId", "number", "total"});
        trailerDelimitedLineAggregator.setFieldExtractor(fieldExtractor);
        return trailerDelimitedLineAggregator;
    }
    
    @Bean
    public DelimitedLineAggregator<SalesPlanDetailMultiFormatRecord> endDelimitedLineAggregator() {
        DelimitedLineAggregator<SalesPlanDetailMultiFormatRecord> endDelimitedLineAggregator = new DelimitedLineAggregator<>();
        BeanWrapperFieldExtractor<SalesPlanDetailMultiFormatRecord> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[] {"record", "headNum", "trailerNum", "total"});
        endDelimitedLineAggregator.setFieldExtractor(fieldExtractor);
        return endDelimitedLineAggregator;
    }
    
    @Bean
    @StepScope
    public FlatFileItemWriter<String> writer(
            @Value("#{jobParameters['outputFile']}") File outputFile) {
        PassThroughLineAggregator<String> lineAggregator = new PassThroughLineAggregator<>();
        return new FlatFileItemWriterBuilder<String>()
                .name(ClassUtils.getShortName(FlatFileItemWriter.class))
                .resource(new FileSystemResource(outputFile))
                .lineAggregator(lineAggregator)
                .build();
    }
    
    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       FlatFileItemReader<SalesPlanDetailMultiFormatRecord> reader,
                       FlatFileItemWriter<String> writer,
                       LoggingItemReaderListener listener,
                       MultiFormatItemProcessor multiFormatItemProcessor) {
        return new StepBuilder("jobWriteMultiFormatCsv.step01",
                jobRepository)
                .<SalesPlanDetailMultiFormatRecord, String> chunk(10, transactionManager)
                .reader(reader)
                .listener(listener)
                .writer(writer)
                .processor(multiFormatItemProcessor)
                .build();
    }
    
    @Bean
    public Job jobWriteMultiFormatCsv(JobRepository jobRepository,
                                            Step step01,
                                            JobExecutionLoggingListener listener) {
        return new JobBuilder("jobWriteMultiFormatCsv",jobRepository)
                .start(step01)
                .listener(listener)
                .build();
    }
}