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

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.writer.LoggingItemWriter;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for MultiplePartitioninglStepFileJob.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan(value = { "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.writer"}, scopedProxy = ScopedProxyMode.TARGET_CLASS)
public class MultiplePartitioninglStepFileJobConfig {

    @Bean
    public TaskExecutor parallelTaskExecutor(
            @Value("${thread.size}") int threadSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadSize);
        executor.setQueueCapacity(200);
        return executor;
    }
    
    @Bean
    @StepScope
    public FlatFileItemReader<SalesPlanDetail> reader(
            @Value("#{stepExecutionContext['fileName']}") File fileName) {
        final DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("branchId", "year", "month", "customerId", "amount");
        final BeanWrapperFieldSetMapper<SalesPlanDetail> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(SalesPlanDetail.class);
        final DefaultLineMapper<SalesPlanDetail> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        return new FlatFileItemReaderBuilder<SalesPlanDetail>()
                .name(ClassUtils.getShortName(FlatFileItemReader.class))
                .resource(new FileSystemResource(fileName))
                .lineMapper(lineMapper)
                .build();
    }
    
    @Bean
    @StepScope
    public MultiResourcePartitioner partitioner(
            @Value("#{jobParameters['inputdir']}") File inputdir) throws Exception {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(
                new FileSystemResource(inputdir).getURL() + File.separator + "salesPlanDetail_*.csv");
        partitioner.setResources(resources);
        return partitioner;
    }
    
    @Bean
    public Step stepWorker(JobRepository jobRepository,
                            FlatFileItemReader<SalesPlanDetail> reader,
                            LoggingItemWriter writer,
                            @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("jobEvaluationReport.worker", jobRepository)
                .<SalesPlanDetail, SalesPlanDetail> chunk(20,
                        transactionManager)
                .reader(reader)
                .writer(writer)
                .build();
    }
    
    @Bean
    public PartitionHandler partitionHandler(
            TaskExecutor parallelTaskExecutor,
            @Qualifier("stepWorker") Step stepWorker) {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setTaskExecutor(parallelTaskExecutor);
        handler.setStep(stepWorker);
        handler.setGridSize(0);
        return handler;
    }
    
    @Bean
    public Step stepManager(JobRepository jobRepository,
                            MultiResourcePartitioner partitioner,
                            PartitionHandler partitionHandler) {
        return new StepBuilder("multiplePartitioninglStepFileJob.manager", jobRepository)
                .partitioner("multiplePartitioninglStepFileJob.worker", partitioner)
                .partitionHandler(partitionHandler)
                .build();
    }
    
    @Bean
    public Job multiplePartitioninglStepFileJob(JobRepository jobRepository,
                                                @Qualifier("stepManager") Step stepManager) {
        return new JobBuilder("multiplePartitioninglStepFileJob", jobRepository)
                .start(stepManager)
                .build();
    }
}
