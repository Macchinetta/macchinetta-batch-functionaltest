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
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.FixedLengthTokenizer;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;
import org.terasoluna.batch.item.file.FixedByteLengthBufferedReaderFactory;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.JobExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.LoggingItemReaderListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for JobReadFixedLengthWithSurrogatePair job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan("jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common")
@MapperScan(basePackages = "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.plan", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class JobReadFixedLengthWithSurrogatePairConfig {

    @Bean
    @StepScope
    public FlatFileItemReader<SalesPlanDetail> reader(
            @Value("#{jobParameters['inputFile']}") String inputFile) {
        Range[] ranges = new Range[] {new Range(1, 3), new Range(4, 7), new Range(8, 9), new Range(10, 19), new Range(20, 29)};
        final FixedLengthTokenizer tokenizer = new FixedLengthTokenizer();
        tokenizer.setColumns(ranges);
        tokenizer.setNames("branchId", "year", "month", "customerId", "amount");
        final BeanWrapperFieldSetMapper<SalesPlanDetail> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(SalesPlanDetail.class);
        final DefaultLineMapper<SalesPlanDetail> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        FileSystemResourceLoader loader = new FileSystemResourceLoader();
        FixedByteLengthBufferedReaderFactory bufferedReaderFactory = new FixedByteLengthBufferedReaderFactory(32);
        return new FlatFileItemReaderBuilder<SalesPlanDetail>()
                .name(ClassUtils.getShortName(FlatFileItemReader.class))
                .bufferedReaderFactory(bufferedReaderFactory)
                .encoding("UTF-8")
                .resource(loader.getResource("file:" + inputFile))
                .lineMapper(lineMapper)
                .build();
    }
    
    @Bean
    public MyBatisBatchItemWriter<SalesPlanDetail> writer(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory,
            SqlSessionTemplate batchModeSqlSessionTemplate) {
        return new MyBatisBatchItemWriterBuilder<SalesPlanDetail>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .statementId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.plan.SalesPlanDetailRepository.create")
                .sqlSessionTemplate(batchModeSqlSessionTemplate)
                .build();
    }
    
    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       FlatFileItemReader<SalesPlanDetail> reader,
                       MyBatisBatchItemWriter<SalesPlanDetail> writer,
                       LoggingItemReaderListener listener) {
        return new StepBuilder("jobReadFixedLengthWithSurrogatePair.step01",
                jobRepository)
                .<SalesPlanDetail, SalesPlanDetail> chunk(10, transactionManager)
                .reader(reader)
                .listener(listener)
                .writer(writer)
                .build();
    }

    @Bean
    public Job jobReadFixedLengthWithSurrogatePair(JobRepository jobRepository,
                                            Step step01,
                                            JobExecutionLoggingListener listener) {
        return new JobBuilder("jobReadFixedLengthWithSurrogatePair",jobRepository)
                .start(step01)
                .listener(listener)
                .build();
    }
}
