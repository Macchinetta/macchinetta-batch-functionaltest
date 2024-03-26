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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.jobs.ch05.exclusivecontrol;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.mybatis.spring.batch.builder.MyBatisBatchItemWriterBuilder;
import org.mybatis.spring.batch.builder.MyBatisCursorItemReaderBuilder;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.transaction.PlatformTransactionManager;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.JobExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.BranchEditWithkPessimisticLockItemProcessor;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.model.ExclusiveBranch;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for ChunkPessimisticLockCheckJob.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan(value = { "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
"jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol"}, scopedProxy = ScopedProxyMode.TARGET_CLASS)
@MapperScan(basePackages = "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.repository", sqlSessionTemplateRef = "batchModeSqlSessionTemplate")
public class ChunkPessimisticLockCheckJobConfig {

    @Bean
    @StepScope
    public MyBatisCursorItemReader<String> reader(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory,
            @Value("#{jobParameters['branchName']}") String branchName) {
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("branchName", branchName);
        return new MyBatisCursorItemReaderBuilder<String>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .parameterValues(parameterValues)
                .queryId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.repository.ExclusiveControlRepository.branchIdFindByName")
                .build();
    }
    
    @Bean
    @StepScope
    public MyBatisBatchItemWriter<ExclusiveBranch> writer(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory,
            SqlSessionTemplate batchModeSqlSessionTemplate,
            @Value("#{new Boolean(jobParameters['assertUpdates'])}") boolean assertUpdates) {
        return new MyBatisBatchItemWriterBuilder<ExclusiveBranch>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .statementId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.repository.ExclusiveControlRepository.branchUpdate")
                .sqlSessionTemplate(batchModeSqlSessionTemplate)
                .assertUpdates(assertUpdates)
                .build();
    }
    
    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       MyBatisCursorItemReader<String> reader,
                       MyBatisBatchItemWriter<ExclusiveBranch> writer,
                       BranchEditWithkPessimisticLockItemProcessor processor) {
        return new StepBuilder("chunkPessimisticLockCheckJob.step01",
                jobRepository)
                .<String, ExclusiveBranch> chunk(10, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job chunkPessimisticLockCheckJob(JobRepository jobRepository,
                                            Step step01,
                                            JobExecutionLoggingListener jobExecutionLoggingListener) {
        return new JobBuilder("chunkPessimisticLockCheckJob",jobRepository)
                .start(step01)
                .listener(jobExecutionLoggingListener)
                .build();
    }
}
