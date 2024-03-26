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

import org.mybatis.spring.annotation.MapperScan;
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
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.component.SalesPlanChunkTranTask;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for CreateSalesPlanChunkTranTask job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan(value = "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.component", scopedProxy = ScopedProxyMode.TARGET_CLASS)
@MapperScan(basePackages = "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.plan", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class CreateSalesPlanChunkTranTaskConfig {

    @Bean
    @StepScope
    public FlatFileItemReader<SalesPlanDetail> detailCSVReader(
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
                .lineMapper(lineMapper)
                .resource(new FileSystemResource(inputFile))
                .build();
    }
    
    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobResourcelessTransactionManager") PlatformTransactionManager transactionManager,
                       SalesPlanChunkTranTask salesPlanChunkTranTask) {
        return new StepBuilder("createSalesPlanChunkTranTask.step01",
                jobRepository)
                .tasklet(salesPlanChunkTranTask, transactionManager)
                .build();
    }
    
    @Bean
    public Job createSalesPlanChunkTranTask(JobRepository jobRepository,
                                            Step step01) {
        return new JobBuilder("createSalesPlanChunkTranTask",jobRepository)
                .start(step01)
                .build();
    }
}