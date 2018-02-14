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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.util

class RestJobExecution {

    int status
    JobExecution jobExecution

    static class JobExecution {
        Long jobExecutionId
        String jobName
        String status
        String exitStatus
        String createTime
        String startTime
        String endTime
        String lastUpdated
        String errorMessage
        List failureExceptions
        String jobConfigurationName
        Object jobParameters
        Object executionContext
        List<StepExecution> stepExecutions
    }

    static class StepExecution {
        Long stepExecutionId
        String stepName
        String exitStatus

        String startTime
        String lastUpdated
        String endTime

        int commitCount
        int rollbackCount
        int filterCount
        int processSkipCount
        int readCount
        int readSkipCount
        int writeCount
        int writeSkipCount
        boolean terminateOnly
        String status
        List failureExceptions
        Object executionContext
    }
}
