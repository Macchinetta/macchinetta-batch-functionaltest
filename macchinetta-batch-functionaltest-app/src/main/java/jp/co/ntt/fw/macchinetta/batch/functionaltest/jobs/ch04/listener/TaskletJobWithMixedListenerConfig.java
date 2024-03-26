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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.jobs.ch04.listener;

import java.io.File;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.mybatis.spring.batch.builder.MyBatisBatchItemWriterBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
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

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.listener.AllProcessListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.listener.annotation.AnnotationSalesPlanDetailRegisterTasklet;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for TaskletJobWithMixedListener job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan(value = {"jp.co.ntt.fw.macchinetta.batch.functionaltest.app.plan",
    "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.listener"}, scopedProxy = ScopedProxyMode.TARGET_CLASS)
@MapperScan(basePackages = "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.plan", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class TaskletJobWithMixedListenerConfig {

    @Bean
    @StepScope
    public FlatFileItemReader<SalesPlanDetail> reader(
            @Value("#{jobParameters['inputFile']}") File inputFile) {
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setNames("branchId", "year", "month", "customerId", "amount");
        BeanWrapperFieldSetMapper<SalesPlanDetail> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(SalesPlanDetail.class);
        DefaultLineMapper<SalesPlanDetail> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(lineTokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        return new FlatFileItemReaderBuilder<SalesPlanDetail>()
                .name(ClassUtils.getShortName(FlatFileItemReader.class))
                .resource(new FileSystemResource(inputFile))
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
    public Step jobScopeStep01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       AnnotationSalesPlanDetailRegisterTasklet tasklet) {
        return new StepBuilder("taskletJobWithMixedListenerWithinJobScope.step01",
                jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Job taskletJobWithMixedListenerWithinJobScope(JobRepository jobRepository,
                                            @Qualifier("jobScopeStep01") Step jobScopeStep01,
                                            AllProcessListener listener) {
        return new JobBuilder("taskletJobWithMixedListenerWithinJobScope",
                jobRepository)
                .start(jobScopeStep01)
                .listener(listener)
                .build();
    }
    
    @Bean
    public Step stepScopeStep01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       AnnotationSalesPlanDetailRegisterTasklet tasklet,
                       @Qualifier("allProcessListener") StepExecutionListener listener) {
        return new StepBuilder("taskletJobWithMixedListenerWithinStepScope.step01",
                jobRepository)
                .listener(listener)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Job taskletJobWithMixedListenerWithinStepScope(JobRepository jobRepository,
                                            @Qualifier("stepScopeStep01") Step stepScopeStep01) {
        return new JobBuilder("taskletJobWithMixedListenerWithinStepScope",
                jobRepository)
                .start(stepScopeStep01)
                .build();
    }
    
    @Bean
    public Step taskletScopeStep01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       AnnotationSalesPlanDetailRegisterTasklet tasklet,
                       @Qualifier("allProcessListener") StepExecutionListener listener) {
        return new StepBuilder("taskletJobWithMixedListenerWithinTaskletScope.step01",
                jobRepository)
                .tasklet(tasklet, transactionManager)
                .listener(listener)
                .build();
    }

    @Bean
    public Job taskletJobWithMixedListenerWithinTaskletScope(JobRepository jobRepository,
                                            @Qualifier("taskletScopeStep01") Step taskletScopeStep01) {
        return new JobBuilder("taskletJobWithMixedListenerWithinTaskletScope",
                jobRepository)
                .start(taskletScopeStep01)
                .build();
    }
    
    @Bean
    public Step allScopeStep01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       AnnotationSalesPlanDetailRegisterTasklet tasklet,
                       @Qualifier("allProcessListener") StepExecutionListener listener) {
        return new StepBuilder("taskletJobWithMixedListenerWithinAllScope.step01",
                jobRepository)
                .listener(listener)
                .tasklet(tasklet, transactionManager)
                .listener(listener)
                .build();
    }

    @Bean
    public Job taskletJobWithMixedListenerWithinAllScope(JobRepository jobRepository,
                                            @Qualifier("allScopeStep01") Step allScopeStep01,
                                            AnnotationSalesPlanDetailRegisterTasklet tasklet,
                                            @Qualifier("allProcessListener") JobExecutionListener listener) {
        return new JobBuilder("taskletJobWithMixedListenerWithinAllScope",
                jobRepository)
                .start(allScopeStep01)
                .listener(listener)
                .build();
    }
}
