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
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Branch;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.BranchEditItemProcessor;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.model.ExclusiveBranch;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for ChunkOptimisticLockCheckJob.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan(value = { "jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
"jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol"}, scopedProxy = ScopedProxyMode.TARGET_CLASS)
@MapperScan(basePackages = "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.repository", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class ChunkOptimisticLockCheckJobConfig {

    @Bean
    @StepScope
    public MyBatisCursorItemReader<Branch> reader(
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory,
            @Value("#{jobParameters['branchId']}") String branchId) {
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("branchId", branchId);
        return new MyBatisCursorItemReaderBuilder<Branch>()
                .sqlSessionFactory(jobSqlSessionFactory)
                .parameterValues(parameterValues)
                .queryId(
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.repository.ExclusiveControlRepository.branchFindOne")
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
                        "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.repository.ExclusiveControlRepository.branchExclusiveUpdate")
                .sqlSessionTemplate(batchModeSqlSessionTemplate)
                .assertUpdates(assertUpdates)
                .build();
    }
    
    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       MyBatisCursorItemReader<Branch> reader,
                       MyBatisBatchItemWriter<ExclusiveBranch> writer,
                       BranchEditItemProcessor branchEditItemProcessor) {
        return new StepBuilder("chunkOptimisticLockCheckJob.step01",
                jobRepository)
                .<Branch, ExclusiveBranch> chunk(10, transactionManager)
                .reader(reader)
                .processor(branchEditItemProcessor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job chunkOptimisticLockCheckJob(JobRepository jobRepository,
                                            Step step01,
                                            JobExecutionLoggingListener jobExecutionLoggingListener) {
        return new JobBuilder("chunkOptimisticLockCheckJob",jobRepository)
                .start(step01)
                .listener(jobExecutionLoggingListener)
                .build();
    }
}
