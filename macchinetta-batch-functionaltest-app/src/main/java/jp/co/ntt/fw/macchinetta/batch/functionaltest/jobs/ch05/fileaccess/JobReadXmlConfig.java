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
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
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
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.LoggingItemReaderListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.model.plan.SalesPlanDetailToJaxb;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for JobReadXml job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan({ "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
        "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.plan"})
@MapperScan(basePackages = "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.plan", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class JobReadXmlConfig {

    @Bean
    @StepScope
    public StaxEventItemReader<SalesPlanDetail> reader(
            @Value("#{jobParameters['inputFile']}") File inputFile) throws Exception {
        Jaxb2Marshaller unmarshaller = new Jaxb2Marshaller();
        unmarshaller.setClassesToBeBound(SalesPlanDetailToJaxb.class);
        unmarshaller.afterPropertiesSet();
        return new StaxEventItemReaderBuilder<SalesPlanDetail>()
                .name(ClassUtils.getShortName(StaxEventItemReader.class))
                .unmarshaller(unmarshaller)
                .resource(new FileSystemResource(inputFile))
                .encoding("UTF-8")
                .addFragmentRootElements("SalesPlanDetail")
                .strict(true)
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
                       StaxEventItemReader<SalesPlanDetail> reader,
                       MyBatisBatchItemWriter<SalesPlanDetail> writer,
                       LoggingItemReaderListener listener) {
        return new StepBuilder("jobReadXml.step01",
                jobRepository)
                .<SalesPlanDetail, SalesPlanDetail> chunk(10, transactionManager)
                .reader(reader)
                .listener(listener)
                .writer(writer)
                .build();
    }

    @Bean
    public Job jobReadXml(JobRepository jobRepository,
                                            Step step01,
                                            JobExecutionLoggingListener listener) {
        return new JobBuilder("jobReadXml",jobRepository)
                .start(step01)
                .listener(listener)
                .build();
    }
}
