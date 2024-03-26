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
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.mybatis.spring.batch.builder.MyBatisCursorItemReaderBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.JobExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.evaluation.EvaluationItemProcessor;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.evaluation.EvaluationReportPartitioner;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.evaluation.EvaluationReport;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.listener.StepExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for JobEvaluationReport job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan(value = { "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.evaluation",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.listener"}, scopedProxy = ScopedProxyMode.TARGET_CLASS)
@MapperScan(value = "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.evaluation", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class JobEvaluationReportConfig {

    @Bean
    @StepScope
    public MyBatisCursorItemReader<EvaluationReport> reader(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory,
            @Value("#{jobParameters['year']}") Integer year,
            @Value("#{jobParameters['month']}") Integer month,
            @Value("#{stepExecutionContext['branch'].branchId}") String branchId) {
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("year", year);
        parameterValues.put("month", month);
        parameterValues.put("branchId", branchId);
        return new MyBatisCursorItemReaderBuilder<EvaluationReport>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .queryId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.evaluation.EvaluationRepository.findEvaluationData")
                .parameterValues(parameterValues)
                .build();
    }
    
    @Bean
    @StepScope
    public FlatFileItemWriter<EvaluationReport> writer(
            @Value("#{jobParameters['outputPath']}") File outputPath,
            @Value("#{stepExecutionContext['branch'].branchId}") String branchId) {
        final BeanWrapperFieldExtractor<EvaluationReport> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(
                new String[] { "branchId", "branchName", "year", "month", "evaluation", "ratio" });
        final DelimitedLineAggregator<EvaluationReport> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setFieldExtractor(fieldExtractor);
        return new FlatFileItemWriterBuilder<EvaluationReport>()
                .name(ClassUtils.getShortName(FlatFileItemWriter.class))
                .resource(new FileSystemResource(outputPath + File.separator + "Evaluation_" + branchId + ".csv"))
                .transactional(false)
                .lineAggregator(lineAggregator)
                .build();
    }
    
    @Bean
    public Step stepWorker(JobRepository jobRepository,
                            StepExecutionLoggingListener listener,
                            MyBatisCursorItemReader<EvaluationReport> reader,
                            FlatFileItemWriter<EvaluationReport> writer,
                            EvaluationItemProcessor evaluationItemProcessor,
                            @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("jobEvaluationReport.step.worker", jobRepository)
                .<EvaluationReport, EvaluationReport> chunk(10,
                        transactionManager)
                .listener(listener)
                .reader(reader)
                .processor(evaluationItemProcessor)
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
        handler.setGridSize(3);
        return handler;
    }
    
    @Bean
    public Step stepManager(JobRepository jobRepository,
                            EvaluationReportPartitioner evaluationReportPartitioner,
                            PartitionHandler partitionHandler) {
        return new StepBuilder("jobEvaluationReport.step.manager", jobRepository)
                .partitioner("jobEvaluationReport.step.worker", evaluationReportPartitioner)
                .partitionHandler(partitionHandler)
                .build();
    }
    
    @Bean
    public Job jobEvaluationReport(JobRepository jobRepository,
                                           JobExecutionLoggingListener listener,
                                           @Qualifier("stepManager") Step stepManager) {
        return new JobBuilder("jobEvaluationReport", jobRepository)
                .start(stepManager)
                .listener(listener)
                .build();
    }
    
    @Bean
    public TaskExecutor parallelTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setQueueCapacity(10);
        return executor;
    }
}
