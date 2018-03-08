/*
 * Copyright (C) 2018 NTT Corporation
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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch07.jobmanagement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Date;

/**
 * Job Monitor.
 *
 * @since 5.0.1
 */
public class JobMonitor {

    private static final Logger logger = LoggerFactory.getLogger(JobMonitor.class);

    public static void main(String[] args) throws Exception {

        if (args.length != 1) {
            System.out.println("Usage: JobMonitor JobExecutionId");
            logger.error("Job Parameter is not specific.");
            System.exit(100);
        }
        long jobExecutionId = -1;
        try {
            jobExecutionId = Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("JobExecutionId msut be long.");
            logger.error("JobExecutionId msut be long.", e);
            System.exit(100);
        }

        JobMonitor jobMonitor = new JobMonitor();
        System.exit(jobMonitor.monitor(jobExecutionId));

    }

    /**
     * Monitor a specific job..
     *
     * @param jobExecutionId Job execution id.
     */
    private int monitor(long jobExecutionId) {
        try (ConfigurableApplicationContext context = new ClassPathXmlApplicationContext("META-INF/jobs/common/jobSalesPlan02.xml")) {

            JobExplorer jobExplorer = (JobExplorer) context.getBean("jobExplorer");

            JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);
            if (jobExecution == null) {
                logger.error("Job Instance not found. [JobExecutionId : {}]", jobExecutionId);
                return 255;
            }

            String jobName = jobExecution.getJobInstance().getJobName();
            Date jobStartTime = jobExecution.getStartTime();
            Date jobEndTime = jobExecution.getEndTime();
            BatchStatus jobBatchStatus = jobExecution.getStatus();
            String jobExitCode = jobExecution.getExitStatus().getExitCode();

            logger.info("Job Name : {}", jobName);
            logger.info("Job Start Time : {}", jobStartTime);
            logger.info("Job End   Time : {}", jobEndTime);
            logger.info("Job Status : {}", jobBatchStatus);
            logger.info("Job Exit Code : {}", jobExitCode);

            for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
                String stepName = stepExecution.getStepName();
                Date stepStartTime = stepExecution.getStartTime();
                Date stepEndTime = stepExecution.getEndTime();
                BatchStatus stepStatus = stepExecution.getStatus();
                String stepExitCode = stepExecution.getExitStatus().getExitCode();

                logger.info("Step Name : {}", stepName);
                logger.info("Step Start Time : {}", stepStartTime);
                logger.info("Step End   Time : {}", stepEndTime);
                logger.info("Step Status : {}", stepStatus);
                logger.info("Step Exit Code : {}", stepExitCode);
            }
        }

        return 0;
    }

}
