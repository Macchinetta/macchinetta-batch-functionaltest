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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.jobs.ch08.parallelandmultiple;

import java.io.File;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.mybatis.spring.batch.builder.MyBatisBatchItemWriterBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
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
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.listener.StepExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.parallel.DeleteDetailTasklet;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for ParallelRegisterJob.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan({ "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.listener",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.parallel" })
@MapperScan(value = {
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.plan",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.performance" }, sqlSessionFactoryRef = "jobSqlSessionFactory")
public class ParallelRegisterJobConfig {

    @Bean
    @StepScope
    public FlatFileItemReader<SalesPlanDetail> planReader(
            @Value("#{jobParameters['planInputFile']}") File inputFile) {
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
    @StepScope
    public FlatFileItemReader<SalesPerformanceDetail> performanceReader(
            @Value("#{jobParameters['performanceInputFile']}") File inputFile) {
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setNames("branchId", "year", "month", "customerId", "amount");
        BeanWrapperFieldSetMapper<SalesPerformanceDetail> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(SalesPerformanceDetail.class);
        DefaultLineMapper<SalesPerformanceDetail> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(lineTokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        return new FlatFileItemReaderBuilder<SalesPerformanceDetail>()
                .name(ClassUtils.getShortName(FlatFileItemReader.class))
                .lineMapper(lineMapper)
                .resource(new FileSystemResource(inputFile))
                .build();
    }
    
    @Bean
    public MyBatisBatchItemWriter<SalesPlanDetail> planWriter(
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
    public MyBatisBatchItemWriter<SalesPerformanceDetail> performanceWriter(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory,
            SqlSessionTemplate batchModeSqlSessionTemplate) {
        return new MyBatisBatchItemWriterBuilder<SalesPerformanceDetail>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .statementId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.performance.SalesPerformanceDetailRepository.create")
                .sqlSessionTemplate(batchModeSqlSessionTemplate)
                .build();
    }
    
    @Bean
    public Step stepPreprocess(JobRepository jobRepository,
                            StepExecutionLoggingListener listener,
                            DeleteDetailTasklet deleteDetailTasklet,
                            @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("parallelRegisterJob.step.preprocess", jobRepository)
                .tasklet(deleteDetailTasklet, transactionManager)
                .listener(listener)
                .build();
    }
    
    @Bean
    public Step stepPlan(JobRepository jobRepository,
                      StepExecutionLoggingListener listener,
                      @Qualifier("planReader") ItemReader<SalesPlanDetail> reader,
                      @Qualifier("planWriter") ItemWriter<SalesPlanDetail> writer,
                      @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("parallelRegisterJob.step.plan", jobRepository)
                .<SalesPlanDetail, SalesPlanDetail> chunk(1,
                        transactionManager)
                .listener(listener)
                .reader(reader)
                .writer(writer)
                .build();
    }

    @Bean
    public Step stepPerformance(JobRepository jobRepository,
                      StepExecutionLoggingListener listener,
                      @Qualifier("performanceReader") ItemReader<SalesPerformanceDetail> reader,
                      @Qualifier("performanceWriter") ItemWriter<SalesPerformanceDetail> writer,
                      @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("parallelRegisterJob.step.performance",
                jobRepository)
                .<SalesPerformanceDetail, SalesPerformanceDetail> chunk(1,
                        transactionManager)
                .listener(listener)
                .reader(reader)
                .writer(writer)
                .build();
    }

    @Bean
    public Flow flow1(@Qualifier("stepPlan") Step stepPlan) {
        return new FlowBuilder<SimpleFlow>("flow1")
                .start(stepPlan)
                .build();
    }

    @Bean
    public Flow flow2(@Qualifier("stepPerformance") Step stepPerformance) {
        return new FlowBuilder<SimpleFlow>("flow2")
                .start(stepPerformance)
                .build();
    }

    @Bean
    public Flow splitFlow(
            @Qualifier("parallelTaskExecutor") TaskExecutor taskExecutor,
            @Qualifier("flow1") Flow flow1,
            @Qualifier("flow2") Flow flow2) {
        return new FlowBuilder<SimpleFlow>("parallelRegisterJob.split")
                .split(taskExecutor)
                .add(flow1, flow2)
                .build();
    }

    @Bean
    public Job parallelRegisterJob(JobRepository jobRepository,
                                   @Qualifier("splitFlow") Flow splitFlow,
                                   @Qualifier("stepPreprocess") Step stepPreprocess) {
        return new JobBuilder("parallelRegisterJob", jobRepository)
                .start(stepPreprocess)
                .on("*")
                .to(splitFlow)
                .end()
                .build();
    }
    
    @Bean
    public TaskExecutor parallelTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setQueueCapacity(10);
        return executor;
    }
}
