/*
 * Copyright (C) 2017 NTT Corporation
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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.asyncjobwithdb.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Log output of job activation with multiple daemon startup.
 * <p>
 * Log daemon identifier set in environment variable or system property.
 * </p>
 *
 * @since 5.0.0
 */
@Component("dbPollingJobListener")
public class DBPollingJobListener implements JobExecutionListener {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(DBPollingJobListener.class);

    /**
     * Daemon identifier
     */
    @Value("${app.daemonName:No-Name}")
    private String daemonName;

    /**
     * Logging job started messages.
     *
     * @param jobExecution jobExecution Job execution information.
     */
    @Override
    public void beforeJob(JobExecution jobExecution) {
        logger.info("Job started on daemon [daemonName:{}]", daemonName);
    }

    /**
     * Logging job finished messages.
     *
     * @param jobExecution jobExecution Job execution information.
     */
    @Override
    public void afterJob(JobExecution jobExecution) {
        logger.info("Job finished on daemon [daemonName:{}]", daemonName);
    }
}
