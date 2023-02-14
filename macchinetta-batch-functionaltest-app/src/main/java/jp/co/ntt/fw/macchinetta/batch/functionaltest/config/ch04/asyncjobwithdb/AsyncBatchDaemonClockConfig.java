/*
 * Copyright (C) 2023 NTT Corporation
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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.config.ch04.asyncjobwithdb;

import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.batch.core.configuration.support.*;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.terasoluna.batch.async.db.JobRequestPollTask;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.config.AsyncBatchDaemonConfig;
import org.terasoluna.batch.async.db.repository.BatchJobRequestRepository;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Customized Acync Batch Daemon Configuration.
 * (clock for timestamp)
 *
 * @since 2.4.0
 */
@Configuration
@Import(AsyncBatchDaemonConfig.class)
public class AsyncBatchDaemonClockConfig {

    @Bean
    public JobRequestPollTask jobRequestPollTask(@Qualifier("adminTransactionManager") DataSourceTransactionManager adminTransactionManager,
                                                 JobOperator jobOperator,
                                                 MapperFactoryBean<BatchJobRequestRepository> batchJobRequestRepository,
                                                 @Qualifier("daemonTaskExecutor") ThreadPoolTaskExecutor daemonTaskExecutor,
                                                 AutomaticJobRegistrar automaticJobRegistrar)  throws Exception {

        JobRequestPollTask jobRequestPollTask = new JobRequestPollTask(batchJobRequestRepository.getObject(), adminTransactionManager, daemonTaskExecutor, jobOperator,
                automaticJobRegistrar);
        jobRequestPollTask.setClock(Clock.fixed(ZonedDateTime.parse("2016-12-31T16:00-08:00[America/Los_Angeles]").toInstant(), ZoneId.of("PST", ZoneId.SHORT_IDS)));
        return jobRequestPollTask;
    }

}
