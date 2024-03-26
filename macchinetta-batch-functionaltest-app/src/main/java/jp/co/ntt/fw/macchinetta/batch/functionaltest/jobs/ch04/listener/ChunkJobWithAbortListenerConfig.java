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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.jobs.ch04.listener;

import java.io.File;

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
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.listener.CheckingJobParameterErrorStepExecutionListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.listener.LoggingReader;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.listener.LoggingWriter;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for ChunkJobWithAbortListener job.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan(value = "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.listener", scopedProxy = ScopedProxyMode.TARGET_CLASS)
public class ChunkJobWithAbortListenerConfig {

    @Bean
    @StepScope
    public LoggingReader reader(
            @Value("#{jobParameters['inputFile']}") File inputFile) {
        LoggingReader reader = new LoggingReader();
        reader.setResource(new FileSystemResource(inputFile));
        return reader;
    }
    
    @Bean
    @StepScope
    public LoggingWriter writer(
            @Value("#{jobParameters['outputFile']}") File outputFile) {
        LoggingWriter writer = new LoggingWriter();
        writer.setResource(new FileSystemResource(outputFile));
        return writer;
    }
    
    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       @Qualifier("reader") LoggingReader reader,
                       @Qualifier("writer") LoggingWriter writer,
                       CheckingJobParameterErrorStepExecutionListener listener) {
        return new StepBuilder("chunkJobWithAbortListener.step01",
                jobRepository)
                .listener(listener)
                .<LoggingReader, LoggingWriter> chunk(10, transactionManager)
                .reader(reader)
                .writer(writer)
                .build();
    }

    @Bean
    public Job chunkJobWithAbortListener(JobRepository jobRepository,
                                            Step step01) {
        return new JobBuilder("chunkJobWithAbortListener",
                jobRepository)
                .start(step01)
                .build();
    }
}
