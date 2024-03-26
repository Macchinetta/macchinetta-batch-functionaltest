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
import org.springframework.batch.item.ItemProcessor;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.JobExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Branch;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Customer;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.component.BranchItemProcessor;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for MasterUpdatePerStepJob.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan({"jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
    "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.component",
    "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.listener"})
@MapperScan(basePackages = "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.repository.admin", sqlSessionFactoryRef = "adminSqlSessionFactory")
@MapperScan(basePackages = "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.repository.job", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class MasterUpdatePerStepJobConfig {

    @Bean
    @StepScope
    public FlatFileItemReader<Branch> branchReader(
            @Value("#{jobParameters['branchInputFile']}") File inputFile) {
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setNames("branchId", "branchName", "branchAddress", "branchTel");
        BeanWrapperFieldSetMapper<Branch> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(Branch.class);
        DefaultLineMapper<Branch> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(lineTokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        return new FlatFileItemReaderBuilder<Branch>()
                .name(ClassUtils.getShortName(FlatFileItemReader.class))
                .lineMapper(lineMapper)
                .resource(new FileSystemResource(inputFile))
                .build();
    }
    
    @Bean
    @StepScope
    public FlatFileItemReader<Customer> customerReader(
            @Value("#{jobParameters['customerInputFile']}") File inputFile) {
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setNames("customerId", "customerName", "customerAddress", "customerTel", "chargeBranchId");
        BeanWrapperFieldSetMapper<Customer> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(Customer.class);
        DefaultLineMapper<Customer> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(lineTokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        return new FlatFileItemReaderBuilder<Customer>()
                .name(ClassUtils.getShortName(FlatFileItemReader.class))
                .lineMapper(lineMapper)
                .resource(new FileSystemResource(inputFile))
                .build();
    }
    
    @Bean
    public MyBatisBatchItemWriter<Branch> branchWriter(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory,
            @Qualifier("adminBatchModeSqlSessionTemplate") SqlSessionTemplate adminBatchModeSqlSessionTemplate) {
        return new MyBatisBatchItemWriterBuilder<Branch>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .statementId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.repository.admin.BranchRepository.create")
                .sqlSessionTemplate(adminBatchModeSqlSessionTemplate)
                .build();
    }
    
    @Bean
    public MyBatisBatchItemWriter<Customer> customerWriter(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory,
            @Qualifier("batchModeSqlSessionTemplate") SqlSessionTemplate batchModeSqlSessionTemplate) {
        return new MyBatisBatchItemWriterBuilder<Customer>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .statementId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.repository.job.CustomerRepository.create")
                .sqlSessionTemplate(batchModeSqlSessionTemplate)
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
    public SqlSessionTemplate adminBatchModeSqlSessionTemplate(
            @Qualifier("adminSqlSessionFactory") SqlSessionFactory adminSqlSessionFactory) {
        return new SqlSessionTemplate(adminSqlSessionFactory, ExecutorType.BATCH);
    }
    
    @Bean
    public Step admin(JobRepository jobRepository,
                       @Qualifier("adminTransactionManager") PlatformTransactionManager transactionManager,
                       @Qualifier("branchReader") FlatFileItemReader<Branch> reader,
                       @Qualifier("branchItemProcessor") BranchItemProcessor processor,
                       @Qualifier("branchWriter") MyBatisBatchItemWriter<Branch> writer) {
        return new StepBuilder("masterUpdatePerStepJob.admin",
                jobRepository)
                .<Branch, Branch> chunk(10, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
    
    @Bean
    public Step job(JobRepository jobRepository,
                    @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                    @Qualifier("customerReader") ItemReader<Customer> reader,
                    @Qualifier("customerItemProcessor") ItemProcessor<Customer, Customer> processor,
                    @Qualifier("customerWriter") ItemWriter<Customer> writer) {
        return new StepBuilder("masterUpdatePerStepJob.job",
                jobRepository)
                .<Customer, Customer> chunk(10, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job masterUpdatePerStepJob(JobRepository jobRepository,
                                            @Qualifier("admin") Step admin,
                                            @Qualifier("job") Step job,
                                            JobExecutionLoggingListener listener) {
        return new JobBuilder("masterUpdatePerStepJob", jobRepository)
                .start(admin)
                .next(job)
                .listener(listener)
                .build();
    }
}
