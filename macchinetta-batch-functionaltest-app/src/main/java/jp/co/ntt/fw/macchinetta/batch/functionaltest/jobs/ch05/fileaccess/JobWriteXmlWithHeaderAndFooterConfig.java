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

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.mybatis.spring.batch.builder.MyBatisCursorItemReaderBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.batch.item.xml.builder.StaxEventItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.JobExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Customer;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.model.mst.CustomerToJaxb;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.module.WriteFooterStaxWriterCallback;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.module.WriteHeaderStaxWriterCallback;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for JobWriteXmlWithHeaderAndFooter job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan({ "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.module"})
@MapperScan(basePackages = "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.repository.mst", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class JobWriteXmlWithHeaderAndFooterConfig {

    @Bean
    @StepScope
    public MyBatisCursorItemReader<Customer> reader(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory) {
        return new MyBatisCursorItemReaderBuilder<Customer>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .queryId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.repository.mst.CustomerRepository.findAll")
                .build();
    }
    
    @Bean
    @StepScope
    public StaxEventItemWriter<Customer> writer(
            @Value("#{jobParameters['outputFile']}") File outputFile,
            WriteHeaderStaxWriterCallback writeHeaderStaxWriterCallback,
            WriteFooterStaxWriterCallback writeFooterStaxWriterCallback) throws Exception {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(CustomerToJaxb.class);
        marshaller.afterPropertiesSet();
        return new StaxEventItemWriterBuilder<Customer>()
                .name(ClassUtils.getShortName(StaxEventItemWriter.class))
                .resource(new FileSystemResource(outputFile))
                .encoding("UTF-8")
                .rootTagName("records")
                .overwriteOutput(true)
                .shouldDeleteIfEmpty(false)
                .transactional(true)
                .headerCallback(writeHeaderStaxWriterCallback)
                .footerCallback(writeFooterStaxWriterCallback)
                .marshaller(marshaller)
                .build();
    }
    
    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       MyBatisCursorItemReader<Customer> reader,
                       StaxEventItemWriter<Customer> writer) {
        return new StepBuilder("jobWriteXmlWithHeaderAndFooter.step01",
                jobRepository)
                .<Customer, Customer> chunk(10, transactionManager)
                .reader(reader)
                .writer(writer)
                .build();
    }

    @Bean
    public Job jobWriteXmlWithHeaderAndFooter(JobRepository jobRepository,
                                            Step step01,
                                            JobExecutionLoggingListener listener) {
        return new JobBuilder("jobWriteXmlWithHeaderAndFooter",jobRepository)
                .start(step01)
                .listener(listener)
                .build();
    }
}
