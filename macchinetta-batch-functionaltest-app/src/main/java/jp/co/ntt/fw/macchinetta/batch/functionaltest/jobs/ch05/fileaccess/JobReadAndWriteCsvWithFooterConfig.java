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

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
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
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.model.plan.SalesPlanDetailFooter;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.module.ReadFooterTasklet;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.module.WriteFooterFlatFileFooterCallback;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for JobReadAndWriteCsvWithFooter job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan(value = { "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.module"}, scopedProxy = ScopedProxyMode.TARGET_CLASS)
@MapperScan(basePackages = "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.plan", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class JobReadAndWriteCsvWithFooterConfig {

    @Bean
    @StepScope
    public FlatFileItemReader<SalesPlanDetailFooter> footerReader(
            @Value("#{jobParameters['footerInputFile']}") File inputFile) {
        final DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("name", "value");
        final BeanWrapperFieldSetMapper<SalesPlanDetailFooter> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(SalesPlanDetailFooter.class);
        final DefaultLineMapper<SalesPlanDetailFooter> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        return new FlatFileItemReaderBuilder<SalesPlanDetailFooter>()
                .name(ClassUtils.getShortName(FlatFileItemReader.class))
                .lineMapper(lineMapper)
                .resource(new FileSystemResource(inputFile))
                .build();
    }
    
    @Bean
    @StepScope
    public FlatFileItemReader<SalesPlanDetail> dataReader(
            @Value("#{jobParameters['dataInputFile']}") File inputFile) {
        final DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("branchId", "year", "month", "customerId", "amount");
        final BeanWrapperFieldSetMapper<SalesPlanDetail> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(SalesPlanDetail.class);
        final DefaultLineMapper<SalesPlanDetail> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        return new FlatFileItemReaderBuilder<SalesPlanDetail>()
                .name(ClassUtils.getShortName(FlatFileItemReader.class))
                .lineMapper(lineMapper)
                .resource(new FileSystemResource(inputFile))
                .build();
    }
    
    @Bean
    @StepScope
    public FlatFileItemWriter<SalesPlanDetail> writer(
            @Value("#{jobParameters['outputFile']}") File outputFile,
            WriteFooterFlatFileFooterCallback writeFooterFlatFileFooterCallback) {
        final BeanWrapperFieldExtractor<SalesPlanDetail> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(
                new String[] { "branchId", "year", "month", "customerId", "amount" });
        final DelimitedLineAggregator<SalesPlanDetail> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setFieldExtractor(fieldExtractor);
        return new FlatFileItemWriterBuilder<SalesPlanDetail>()
                .name(ClassUtils.getShortName(FlatFileItemWriter.class))
                .resource(new FileSystemResource(outputFile))
                .transactional(false)
                .lineAggregator(lineAggregator)
                .footerCallback(writeFooterFlatFileFooterCallback)
                .build();
    }
    
    @Bean
    @JobScope
    public ReadFooterTasklet readFooterTasklet() {
        return new ReadFooterTasklet();
    }
    
    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       ReadFooterTasklet readFooterTasklet) {
        return new StepBuilder("jobReadAndWriteCsvWithFooter.step01",
                jobRepository)
                .tasklet(readFooterTasklet, transactionManager)
                .build();
    }
    
    @Bean
    public Step step02(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       FlatFileItemReader<SalesPlanDetail> dataReader,
                       FlatFileItemWriter<SalesPlanDetail> writer,
                       LoggingItemReaderListener loggingItemReaderListener) {
        return new StepBuilder("jobReadAndWriteCsvWithFooter.step02",
                jobRepository)
                .<SalesPlanDetail, SalesPlanDetail> chunk(10, transactionManager)
                .reader(dataReader)
                .listener(loggingItemReaderListener)
                .writer(writer)
                .build();
    }

    @Bean
    public Job jobReadAndWriteCsvWithFooter(JobRepository jobRepository,
                                            @Qualifier("step01") Step step01,
                                            @Qualifier("step02") Step step02,
                                            JobExecutionLoggingListener jobExecutionLoggingListener,
                                            ReadFooterTasklet readFooterTasklet,
                                            WriteFooterFlatFileFooterCallback writeFooterFlatFileFooterCallback) {
        return new JobBuilder("jobReadAndWriteCsvWithFooter",jobRepository)
                .start(step01)
                .next(step02)
                .listener(jobExecutionLoggingListener)
                .listener(readFooterTasklet)
                .listener(writeFooterFlatFileFooterCallback)
                .build();
    }
}
