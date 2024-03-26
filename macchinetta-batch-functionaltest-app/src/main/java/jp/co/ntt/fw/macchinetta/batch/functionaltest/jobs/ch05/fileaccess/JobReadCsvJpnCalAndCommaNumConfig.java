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
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.separator.DefaultRecordSeparatorPolicy;
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

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.LoggingItemReaderListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.model.plan.UseDateSalesPlanDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.module.LoggingItemWriter;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.plan.UseDateSalesPlanDetailFieldSetMapper;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for JobReadCsvJpnCalAndCommaNum job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan({ "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.plan",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.module"})
public class JobReadCsvJpnCalAndCommaNumConfig {

    @Bean
    public DefaultRecordSeparatorPolicy defaultRecordSeparatorPolicy() {
        return new DefaultRecordSeparatorPolicy();
    }
    
    @Bean
    @StepScope
    public FlatFileItemReader<UseDateSalesPlanDetail> reader(
            @Value("#{jobParameters['inputFile']}") File inputFile,
            DefaultRecordSeparatorPolicy defaultRecordSeparatorPolicy,
            UseDateSalesPlanDetailFieldSetMapper useDateSalesPlanDetailFieldSetMapper) {
        final DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("branchId", "date", "customerId", "amount");
        final DefaultLineMapper<UseDateSalesPlanDetail> lineMapper = new DefaultLineMapper<UseDateSalesPlanDetail>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(useDateSalesPlanDetailFieldSetMapper);
        return new FlatFileItemReaderBuilder<UseDateSalesPlanDetail>()
                .name(ClassUtils.getShortName(FlatFileItemReader.class))
                .recordSeparatorPolicy(defaultRecordSeparatorPolicy)
                .resource(new FileSystemResource(inputFile))
                .lineMapper(lineMapper)
                .encoding("UTF-8")
                .build();
    }
    
    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       FlatFileItemReader<UseDateSalesPlanDetail> reader,
                       LoggingItemWriter writer,
                       LoggingItemReaderListener listener) {
        return new StepBuilder("jobReadCsvJpnCalAndCommaNum.step01",
                jobRepository)
                .<UseDateSalesPlanDetail, UseDateSalesPlanDetail> chunk(10, transactionManager)
                .reader(reader)
                .listener(listener)
                .writer(writer)
                .build();
    }

    @Bean
    public Job jobReadCsvJpnCalAndCommaNum(JobRepository jobRepository,
                                            Step step01) {
        return new JobBuilder("jobReadCsvJpnCalAndCommaNum",
                jobRepository)
                .start(step01)
                .build();
    }
}
