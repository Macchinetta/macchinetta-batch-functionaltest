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

import javax.sql.DataSource;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.JobExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.chain.transaction.CompositMasterFieldSetMapper;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.chain.transaction.CompositMasterItemWriter;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.model.CompositeMaster;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for MasterUpdateChainedTransactionJob.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan({"jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
    "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.chain.transaction",
    "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.listener"})
@MapperScan(basePackages = "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.repository.admin", sqlSessionFactoryRef = "adminSqlSessionFactory")
@MapperScan(basePackages = "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.repository.job", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class MasterUpdateChainedTransactionJobConfig {

    @Bean
    @StepScope
    public FlatFileItemReader<CompositeMaster> reader(
            @Value("#{jobParameters['inputFile']}") File inputFile,
            CompositMasterFieldSetMapper compositMasterFieldSetMapper) {
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setNames("customerId", "customerName", "customerAddress", "customerTel", "branchId", "branchName", "branchAddress", "branchTel");
        DefaultLineMapper<CompositeMaster> lineMapper = new DefaultLineMapper<>();
        lineMapper.setFieldSetMapper(compositMasterFieldSetMapper);
        lineMapper.setLineTokenizer(lineTokenizer);
        return new FlatFileItemReaderBuilder<CompositeMaster>()
                .name(ClassUtils.getShortName(FlatFileItemReader.class))
                .lineMapper(lineMapper)
                .resource(new FileSystemResource(inputFile))
                .build();
    }
    
    @Bean
    public SqlSessionFactory adminSqlSessionFactory(
            @Qualifier("adminDataSource") DataSource adminDataSource) throws Exception {
        SqlSessionFactoryBean adminSqlSessionFactory = new SqlSessionFactoryBean();
        adminSqlSessionFactory.setDataSource(adminDataSource);
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setLocalCacheScope(LocalCacheScope.STATEMENT);
        configuration.setLazyLoadingEnabled(true);
        configuration.setAggressiveLazyLoading(false);
        configuration.setDefaultFetchSize(1000);
        configuration.setDefaultExecutorType(ExecutorType.REUSE);
        adminSqlSessionFactory.setConfiguration(configuration);
        return adminSqlSessionFactory.getObject();
    }
    
    @Bean
    public ChainedTransactionManager chainedTransactionManager(
            @Qualifier("adminTransactionManager") PlatformTransactionManager adminTransactionManager,
            @Qualifier("jobTransactionManager") PlatformTransactionManager jobTransactionManager) {
        return new ChainedTransactionManager(adminTransactionManager, jobTransactionManager);
    }
    
    @Bean
    public Step step01(JobRepository jobRepository,
                    @Qualifier("chainedTransactionManager") PlatformTransactionManager transactionManager,
                    FlatFileItemReader<CompositeMaster> reader,
                    CompositMasterItemWriter writer) {
        return new StepBuilder("masterUpdateChainedTransactionJob.step01",
                jobRepository)
                .<CompositeMaster, CompositeMaster> chunk(10, transactionManager)
                .reader(reader)
                .writer(writer)
                .build();
    }

    @Bean
    public Job masterUpdateChainedTransactionJob(JobRepository jobRepository,
                                            Step step01,
                                            JobExecutionLoggingListener listener) {
        return new JobBuilder("masterUpdateChainedTransactionJob", jobRepository)
                .start(step01)
                .listener(listener)
                .build();
    }
}
