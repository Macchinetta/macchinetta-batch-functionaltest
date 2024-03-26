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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.jobs.ch07.jobmanagement;

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.SimpleJvmExitCodeMapper;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.transaction.PlatformTransactionManager;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.JobExecutionLoggingListener;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch07.jobmanagement.EmulateLongTaskTasklet;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.JobBaseContextConfig;

/**
 * JavaConfig class for EmulateLongTaskJob.
 *
 * @since 2.5.0
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan(value = {"jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common",
    "jp.co.ntt.fw.macchinetta.batch.functionaltest.ch07.jobmanagement"}, scopedProxy = ScopedProxyMode.TARGET_CLASS)
public class EmulateLongTaskJobConfig {

    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       EmulateLongTaskTasklet tasklet) {
        return new StepBuilder("emulateLongTaskJob.step01",
                jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Job emulateLongTaskJob(JobRepository jobRepository,
                                                 Step step01,
                                                 JobExecutionLoggingListener jobExecutionLoggingListener) {
        return new JobBuilder("emulateLongTaskJob",
                jobRepository)
                .start(step01)
                .listener(jobExecutionLoggingListener)
                .build();
    }
    
    @Bean
    public SimpleJvmExitCodeMapper exitCodeMapper() {
        SimpleJvmExitCodeMapper exitCodeMapper = new SimpleJvmExitCodeMapper();
        Map<String, Integer> mapping = new HashMap<>();
        mapping.put("NOOP", 0);
        mapping.put("COMPLETED", 0);
        mapping.put("STOPPED", 200);
        mapping.put("FAILED", 250);
        mapping.put("UNKNOWN", 255);
        exitCodeMapper.setMapping(mapping);
        
        return exitCodeMapper;
    }
}
