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
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.separator.DefaultRecordSeparatorPolicy;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
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
import org.terasoluna.batch.item.file.transform.EnclosableDelimitedLineAggregator;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.LoggingItemReaderListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for JobWriteTsv job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan("jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common")
public class JobWriteTsvConfig {
    
    @Bean
    public DefaultRecordSeparatorPolicy defaultRecordSeparatorPolicy() {
        DefaultRecordSeparatorPolicy defaultRecordSeparatorPolicy = new DefaultRecordSeparatorPolicy();
        defaultRecordSeparatorPolicy.setQuoteCharacter("'");
        return defaultRecordSeparatorPolicy;
    }
    
    @Bean
    @StepScope
    public FlatFileItemReader<SalesPlanDetail> reader(
            @Value("#{jobParameters['inputFile']}") File inputFile,
            DefaultRecordSeparatorPolicy defaultRecordSeparatorPolicy) {
        final DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("branchId", "year", "month", "customerId", "amount");
        tokenizer.setQuoteCharacter('\'');
        tokenizer.setDelimiter(DelimitedLineTokenizer.DELIMITER_TAB);
        final BeanWrapperFieldSetMapper<SalesPlanDetail> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(SalesPlanDetail.class);
        final DefaultLineMapper<SalesPlanDetail> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        return new FlatFileItemReaderBuilder<SalesPlanDetail>()
                .name(ClassUtils.getShortName(FlatFileItemReader.class))
                .lineMapper(lineMapper)
                .resource(new FileSystemResource(inputFile))
                .recordSeparatorPolicy(defaultRecordSeparatorPolicy)
                .build();
    }
    
    @Bean
    @StepScope
    public FlatFileItemWriter<SalesPlanDetail> writer(
            @Value("#{jobParameters['outputFile']}") File outputFile) {
        final BeanWrapperFieldExtractor<SalesPlanDetail> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(
                new String[] { "branchId", "year", "month", "customerId", "amount" });
        final EnclosableDelimitedLineAggregator<SalesPlanDetail> lineAggregator = new EnclosableDelimitedLineAggregator<>();
        lineAggregator.setEnclosure('\'');
        lineAggregator.setDelimiter('\t');
        lineAggregator.setFieldExtractor(fieldExtractor);
        return new FlatFileItemWriterBuilder<SalesPlanDetail>()
                .name(ClassUtils.getShortName(FlatFileItemWriter.class))
                .resource(new FileSystemResource(outputFile))
                .transactional(false)
                .lineAggregator(lineAggregator)
                .build();
    }
    
    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       FlatFileItemReader<SalesPlanDetail> reader,
                       FlatFileItemWriter<SalesPlanDetail> writer,
                       LoggingItemReaderListener listener) {
        return new StepBuilder("jobWriteTsv.step01",
                jobRepository)
                .<SalesPlanDetail, SalesPlanDetail> chunk(10, transactionManager)
                .reader(reader)
                .listener(listener)
                .writer(writer)
                .build();
    }

    @Bean
    public Job jobWriteTsv(JobRepository jobRepository,
                                            Step step01) {
        return new JobBuilder("jobWriteTsv",
                jobRepository)
                .start(step01)
                .build();
    }

}
