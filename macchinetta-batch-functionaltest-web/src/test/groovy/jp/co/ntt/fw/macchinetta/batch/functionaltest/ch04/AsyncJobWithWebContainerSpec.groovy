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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04

import groovy.util.logging.Slf4j
import org.junit.Rule
import org.junit.rules.TestName
import org.springframework.batch.core.launch.support.SimpleJobLauncher
import org.springframework.batch.core.launch.support.SimpleJobOperator
import org.springframework.core.task.TaskRejectedException
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.asyncjobwithweb.EmulatedLongBatchTask
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.DBUnitUtil
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.LogCondition
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.LogCursor
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.MongoUtil
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.RestJobExecution
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.RestSubmitResult
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.RestUtil
import spock.lang.Narrative
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import java.util.concurrent.TimeUnit

/**
 * Function test of asynchronous execution (Web container).
 *
 * @since 5.0.0
 */

@Slf4j
@Narrative("""
For confirm concurrency job execution, using ThreadPoolTaskExecutor(pool-size: 3, and queue-capacity: 10).

A list of test cases on web container is shown below.
1-1. Submit asynchronous job once and confirm job execution status by REST APIs.
1-2-(1). Submit asynchronous job 3 times, and confirm to execute all jobs concurrently.
1-2-(2). Submit asynchronous job twice, and confirm queued.
1-2-(3). Send signal to release lock from threads.
1-2-(4). Obtain jobExecution using jobExecutionId by 2-1, and confirm jobs completion.
1-3. Submit asynchronous job 14 times, and confirm to throw TaskRejectException at 14th job submit.
2-1. Submit asynchronous job and send request for stop and restart.
""")
@Stepwise
class AsyncJobWithWebContainerSpec extends Specification {

    @Rule
    TestName testName = new TestName()

    @Shared
            restUtil = new RestUtil()

    @Shared
            mongoUtil = new MongoUtil()

    @Shared
            jobExecutionIds = []

    @Shared
            dbUnitUtil = new DBUnitUtil('admin')

    def setupSpec() {
        mongoUtil.deleteAll()
        dbUnitUtil.dropAndCreateTable()

        def result = restUtil.get('signal')

        assert result.status == 200
        assert result.data == 'sent signal.'
    }

    def setup() {
        log.debug("### Spec case of [{}]", testName.methodName)
    }

    def cleanupSpec() {
        mongoUtil.deleteAll()

        def result = restUtil.get('signal')

        assert result.status == 200
        assert result.data == 'sent signal.'
    }


    // Testcase 1 No.1
    def "Submit asynchronous job once and confirm job execution status by REST APIs."() {
        when:
        // Send signal to reset.
        def resetResult = restUtil.get('reset')

        then:
        resetResult.status == 200
        resetResult.data == 'sent reset.'

        when:
        // Submit new asynchronous job with REST API.
        String jobParameter = "xxx=${System.currentTimeMillis()}"
        RestSubmitResult restSubmitResult = restUtil.submitJob('jobEmulatedLongBatchTask', "${jobParameter}")
        Long jobExecutionId = restSubmitResult?.data?.jobExecutionId

        then:
        restSubmitResult?.status == 200
        jobExecutionId != null
        jobExecutionId > 0

        when:
        mongoUtil.waitForOutputLog(new LogCondition(level: 'INFO', logger: "${EmulatedLongBatchTask.class.name}",
                message: "start tasklet. [jobExecutionId=${jobExecutionId}]"))

        // Retrieve jobExecution in RUNNING status.
        RestJobExecution restJobExecution = restUtil.getJobExecution(jobExecutionId)

        then:
        restJobExecution.status == 200
        restJobExecution.jobExecution.jobExecutionId == jobExecutionId
        restJobExecution.jobExecution.status == 'STARTED' // Confirm still running job.

        restJobExecution.jobExecution.stepExecutions.size() == 1
        restJobExecution.jobExecution.stepExecutions[0].stepName == 'jobEmulatedLongBatchTask.step01'
        restJobExecution.jobExecution.stepExecutions[0].status == 'STARTED'  // Confirm still running step.

        when:
        // Send signal to release awaiting threads.
        def signalResult = restUtil.get("signal")

        then:
        signalResult.status == 200
        signalResult.data == 'sent signal.'

        when:
        // Wait for a while till the job complete.
        mongoUtil.waitForOutputLog(new LogCondition(
                level: 'INFO',
                logger: "${SimpleJobLauncher.class.name}",
                message: ~/completed with the following parameters:.*${jobParameter}/))
        restJobExecution = restUtil.getJobExecution(jobExecutionId)

        then:
        restJobExecution.status == 200
        restJobExecution.jobExecution.jobExecutionId == jobExecutionId
        restJobExecution.jobExecution.jobName == 'jobEmulatedLongBatchTask'
        restJobExecution.jobExecution.status == 'COMPLETED'

        restJobExecution.jobExecution.stepExecutions.size() == 1
        restJobExecution.jobExecution.stepExecutions[0].stepName == 'jobEmulatedLongBatchTask.step01'
        restJobExecution.jobExecution.stepExecutions[0].status == 'COMPLETED'
    }

    // Testcase 1 No.2 (1)
    def "Submit asynchronous job 3 times, and confirm to execute 3 jobs concurrently."() {

        setup:
        mongoUtil.deleteAll()

        when:
        // Send signal to reset.
        def resetResult = restUtil.get('reset')

        then:
        resetResult.status == 200
        resetResult.data == 'sent reset.'

        when: "Submit new asynchronous job concurrently."
        RestSubmitResult submitResult = restUtil.submitJob(jobName)
        mongoUtil.waitForOutputLog(new LogCondition(level: 'INFO', logger: "${EmulatedLongBatchTask.class.name}",
                message: "start tasklet. [jobExecutionId=${submitResult.data.jobExecutionId}]"))

        then: "Validate REST status and jobExecutionId."
        submitResult.status == sts
        def jobExecutionId = submitResult.data.jobExecutionId
        jobExecutionIds << jobExecutionId
        submitResult.data.jobName == jobName


        when: "Obtain jobExecution using jobExecutionId from REST API."
        RestJobExecution jobExecution = restUtil.getJobExecution(jobExecutionId)

        then: "Confirm job status(NOT COMPLETED)"
        jobExecution.status == sts
        jobExecution.jobExecution.jobName == jobName
        jobExecution.jobExecution.jobExecutionId == jobExecutionId
        jobExecution.jobExecution.status == jobStatus

        where:
        jobName                    || sts | jobStatus
        'jobEmulatedLongBatchTask' || 200 | 'STARTED'
        'jobEmulatedLongBatchTask' || 200 | 'STARTED'
        'jobEmulatedLongBatchTask' || 200 | 'STARTED'
    }

    // Testcase 1 No.2 (2)
    def "Submit asynchronous job twice, and confirm queued."() {

        setup:
        mongoUtil.deleteAll()

        when:
        // Send signal to reset.
        def resetResult = restUtil.get('reset')

        then:
        resetResult.status == 200
        resetResult.data == 'sent reset.'

        when: "Submit new asynchronous job concurrently."
        RestSubmitResult submitResult = restUtil.submitJob(jobName)
        mongoUtil.waitForOutputLog(new LogCondition(level: 'INFO',
                logger: "${SimpleJobOperator.class.name}",
                message: ~/Attempting to launch job with name=jobEmulatedLongBatchTask/))

        then: "Validate REST status and jobExecutionId."
        submitResult.status == sts
        def jobExecutionId = submitResult.data.jobExecutionId
        jobExecutionIds << jobExecutionId
        submitResult.data.jobName == jobName


        when: "Obtain jobExecution using jobExecutionId from REST API."
        RestJobExecution jobExecution = restUtil.getJobExecution(jobExecutionId)

        then: "Confirm job status(NOT COMPLETED)"
        jobExecution.status == sts
        jobExecution.jobExecution.jobName == jobName
        jobExecution.jobExecution.jobExecutionId == jobExecutionId
        jobExecution.jobExecution.status == jobStatus

        where:
        jobName                    || sts | jobStatus
        'jobEmulatedLongBatchTask' || 200 | 'STARTING'
        'jobEmulatedLongBatchTask' || 200 | 'STARTING'
    }

    // Testcase 1 No.2 (3)
    def "Send signal to release lock from threads."() {

        setup:
        mongoUtil.deleteAll()

        when:
        def result = restUtil.get("signal")
        while (true) {
            List<LogCursor> logs = mongoUtil.find(new LogCondition(level: 'INFO',
                    logger: "${SimpleJobLauncher.class.name}", message: ~/Job: \[FlowJob: \[name=jobEmulatedLongBatchTask]] completed/))
            if (logs?.size() == 5) {
                break
            }
            Thread.sleep(500L)
        }

        then:
        result.status == 200
        result.data == 'sent signal.'
    }

    // Testcase 1 No.2 (4)
    def "Obtain jobExecution using jobExecutionId by 2-1, and confirm jobs completion."() {
        def jobExecutions = []
        when:
        jobExecutionIds.each { Long jobExecutionId ->
            jobExecutions << restUtil.getJobExecution(jobExecutionId)
        }

        then:
        jobExecutions.size() == 5
        jobExecutions.every{ RestJobExecution execution ->
            execution.status == 200  &&
            execution.jobExecution.jobName == 'jobEmulatedLongBatchTask'  &&
            execution.jobExecution.status == 'COMPLETED'  &&
            execution.jobExecution.stepExecutions.size() == 1  &&
            execution.jobExecution.stepExecutions[0].stepName == 'jobEmulatedLongBatchTask.step01'  &&
            execution.jobExecution.stepExecutions[0].status == 'COMPLETED'
        }
    }

    // Testcase 1 No.3
    def "Submit asynchronous job 14 times, and confirm to throw TaskRejectException at 14th job submit."() {
        def submitResults = []
        def executionResults = []

        when:
        def resetResult = restUtil.get("reset")

        then:
        resetResult.status == 200
        resetResult.data == 'sent reset.'

        when:
        13.times {
            submitResults << restUtil.submitJob('jobEmulatedLongBatchTask')
        }

        then:
        submitResults.size() == 13
        submitResults.every { RestSubmitResult result ->
            result.status == 200 &&
            result.data.jobName == 'jobEmulatedLongBatchTask'
        }

        when: "Starting jobs normal."
        submitResults.each { RestSubmitResult submitResult ->
            executionResults << restUtil.getJobExecution(submitResult.data.jobExecutionId)
        }

        then:
        executionResults.every { RestJobExecution execution ->
            execution.status == 200 &&
            execution.jobExecution.jobName == 'jobEmulatedLongBatchTask' &&
            ['STARTED', 'STARTING'].contains(execution.jobExecution.status) &&
            execution.jobExecution.errorMessage == null
        }

        when:
        mongoUtil.deleteAll()

        RestSubmitResult actual = restUtil.submitJob('jobEmulatedLongBatchTask')

        then:
        actual.status == 200
        actual.data.jobName == 'jobEmulatedLongBatchTask'
        actual.data.resultMessage == 'Job launching task is scheduled.'
        actual.data.errorMessage == null

        when:
        RestJobExecution actualExecution = restUtil.getJobExecution(actual.data.jobExecutionId)

        then:
        actualExecution.status == 200
        actualExecution.jobExecution.status == 'FAILED'
        actualExecution.jobExecution.exitStatus ==~ /(?s).*${TaskRejectedException.class.name}.*did not accept task: ${SimpleJobLauncher.class.name}.*/

        cleanup:
        def result = restUtil.get('signal')

        assert result.status == 200
        assert result.data == 'sent signal.'
        // Await jobs completion.
        mongoUtil.waitForOutputLog(new LogCondition(level: 'INFO', logger: "${SimpleJobLauncher.class.name}",
                message: ~/completed with the following parameters:.*and the following status: \[COMPLETED]/))
    }

    // Testcase 2 No.1
    def "Submit asynchronous job and send request for stop and restart."() {

        setup:
        mongoUtil.deleteAll()
        try {
            dbUnitUtil.dropAndCreateTable()
        } catch (Exception e) {
            // retry
            TimeUnit.SECONDS.sleep(5L)
            dbUnitUtil.dropAndCreateTable()
        }

        when:
        // Send signal to reset.
        def resetResult = restUtil.get('reset')

        then:
        resetResult.status == 200
        resetResult.data == 'sent reset.'

        when:
        // Submit new asynchronous job with REST API.
        RestSubmitResult restSubmitResult = restUtil.submitJob('jobStopAndRestartTask')
        Long jobExecutionId = restSubmitResult?.data?.jobExecutionId

        then:
        restSubmitResult?.status == 200
        jobExecutionId != null
        jobExecutionId > 0

        when:
        mongoUtil.waitForOutputLog(new LogCondition(level: 'INFO', logger: "${EmulatedLongBatchTask.class.name}",
                message: "start tasklet. [jobExecutionId=${jobExecutionId}]"))

        // Retrieve jobExecution in RUNNING status.
        RestJobExecution restJobExecution = restUtil.getJobExecution(jobExecutionId)

        then:
        restJobExecution.status == 200
        restJobExecution.jobExecution.jobExecutionId == jobExecutionId
        restJobExecution.jobExecution.status == 'STARTED' // Confirm still running job.

        restJobExecution.jobExecution.stepExecutions.size() == 1
        restJobExecution.jobExecution.stepExecutions[0].stepName == 'jobStopAndRestartTask.step01'
        restJobExecution.jobExecution.stepExecutions[0].status == 'STARTED'  // Confirm still running step.

        when:
        // send "stop" request.
        def stopExecution = restUtil.stopJob(jobExecutionId)

        def stoppingDataSet = DBUnitUtil.createDataSet {
            batch_job_execution {
                job_execution_id | status     | exit_code
                jobExecutionId   | 'STOPPING' | 'UNKNOWN'
            }
            batch_step_execution {
                job_execution_id | step_name                         | status     | exit_code
                jobExecutionId   | 'jobStopAndRestartTask.step01' | 'STARTED' | 'EXECUTING'
            }
        }

        then:
        stopExecution?.status == 200
        def expectStoppingJobExecution = stoppingDataSet.getTable('batch_job_execution')
        def actualStoppingJobExecution = dbUnitUtil.getIncludedColumnsTable('batch_job_execution',
                expectStoppingJobExecution.tableMetaData.columns)
        DBUnitUtil.assertEquals(expectStoppingJobExecution, actualStoppingJobExecution)

        def expectStoppingStepExecution = stoppingDataSet.getTable('batch_step_execution')
        def actualStoppingStepExecution = dbUnitUtil.getIncludedColumnsTable('batch_step_execution',
                expectStoppingStepExecution.tableMetaData.columns)
        DBUnitUtil.assertEquals(expectStoppingStepExecution, actualStoppingStepExecution)

        when:
        // send reset signal.
        def result = restUtil.get('signal')

        then:
        assert result.status == 200
        assert result.data == 'sent signal.'

        mongoUtil.waitForOutputLog(new LogCondition(message: ~/\[name=jobStopAndRestartTask]] completed with the following parameters: .*and the following status: \[STOPPED]/))

        def stoppedDataSet = DBUnitUtil.createDataSet {
            batch_job_execution {
                job_execution_id | status     | exit_code
                jobExecutionId   | 'STOPPED' | 'STOPPED'
            }
            batch_step_execution {
                job_execution_id | step_name                      | status     | exit_code
                jobExecutionId   | 'jobStopAndRestartTask.step01' | 'STOPPED'  | 'STOPPED'
            }
        }

        def expectStoppedJobExecution = stoppedDataSet.getTable('batch_job_execution')
        def actualStoppedJobExecution = dbUnitUtil.getIncludedColumnsTable('batch_job_execution',
                expectStoppingJobExecution.tableMetaData.columns)
        DBUnitUtil.assertEquals(expectStoppedJobExecution, actualStoppedJobExecution)

        def expectStoppedStepExecution = stoppedDataSet.getTable('batch_step_execution')
        def actualStoppedStepExecution = dbUnitUtil.getIncludedColumnsTable('batch_step_execution',
                expectStoppingStepExecution.tableMetaData.columns)
        DBUnitUtil.assertEquals(expectStoppedStepExecution, actualStoppedStepExecution)

        when:
        mongoUtil.deleteAll()

        // send "restart" request.
        def restartExecution = restUtil.restartJob(jobExecutionId)
        def restartedDataSet = DBUnitUtil.createDataSet {
            batch_job_execution {
                job_execution_id    | status      | exit_code
                jobExecutionId      | 'STOPPED'   | 'STOPPED'
                jobExecutionId + 1L | 'COMPLETED' | 'COMPLETED'
            }
            batch_step_execution {
                job_execution_id    | step_name                         | status      | exit_code
                jobExecutionId      | 'jobStopAndRestartTask.step01'    | 'STOPPED'   | 'STOPPED'
                jobExecutionId + 1L | 'jobStopAndRestartTask.step01'    | 'COMPLETED' | 'COMPLETED'
                jobExecutionId + 1L | 'jobStopAndRestartTask.step02'    | 'COMPLETED' | 'COMPLETED'
            }
        }

        then:
        restartExecution?.status == 200
        mongoUtil.waitForOutputLog(new LogCondition(message: ~/\[name=jobStopAndRestartTask]] completed with the following parameters: .*and the following status: \[COMPLETED]/))

        def expectRestartedJobExecution = restartedDataSet.getTable('batch_job_execution')
        def actualRestartedJobExecution = dbUnitUtil.getIncludedColumnsTable('batch_job_execution',
                expectStoppingJobExecution.tableMetaData.columns)
        DBUnitUtil.assertEquals(expectRestartedJobExecution, actualRestartedJobExecution)

        def expectRestartedStepExecution = restartedDataSet.getTable('batch_step_execution')
        def actualRestartedStepExecution = dbUnitUtil.getIncludedColumnsTable('batch_step_execution',
                expectStoppingStepExecution.tableMetaData.columns)
        DBUnitUtil.assertEquals(expectRestartedStepExecution, actualRestartedStepExecution)

    }


}
