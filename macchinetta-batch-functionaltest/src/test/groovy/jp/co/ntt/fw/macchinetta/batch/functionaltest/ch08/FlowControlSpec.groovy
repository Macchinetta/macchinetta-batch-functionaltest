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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08

import groovy.util.logging.Slf4j
import org.springframework.batch.core.job.AbstractJob
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.flowcontrol.ConfirmPromotionalTasklet
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.flowcontrol.PromotionTargetItemProcessor
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.DBUnitUtil
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.JobLauncher
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.JobRequest
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.LogCondition
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.MongoUtil
import spock.lang.Narrative
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Function test of flow control in job transition.
 *
 * @since 2.0.1
 */
@Slf4j
@Narrative("""
A list of test cases is shown below.
1. Test of sequential job flow using 'step' and 'next' attribute.
2. Test of conditional flow using 'next' and 'on' attribute.
3. Test of stop flow using 'stop' and 'fail' attribute.
4. Test of taking over context from previous step.
""")
class FlowControlSpec extends Specification {

    @Shared
            jobLauncher = new JobLauncher()

    @Shared
            adminDBUnitUtil = new DBUnitUtil("admin")

    @Shared
            mongoUtil = new MongoUtil()

    def setup() {
        log.debug("### Spec case of [{}]", this.specificationContext.currentIteration.displayName)
        mongoUtil.deleteAll()
    }

    def cleanupSpec() {
        adminDBUnitUtil.close()
        mongoUtil.close()
    }

    static final String JOBFILE_ROOT = 'META-INF/jobs/ch08/flowcontrol'

    // Testcase 1.
    @Unroll
    def "Test of sequential job flow using 'step' and 'next' attribute. jobName:#jobName fail step target:#failTarget"() {

        setup:
        adminDBUnitUtil.dropAndCreateTable()
        def expectTables = DBUnitUtil.createDataSet {
            batch_job_execution {
                job_execution_id | status      | exit_code
                1                | jobStatus   | jobStatus
            }
            batch_step_execution {
                step_execution_id | step_name          | job_execution_id | status         | exit_code
                1                 | "${jobName}.step1" | 1                | stepStatus[0]  | stepStatus[0]
                2                 | "${jobName}.step2" | 1                | stepStatus[1]  | stepStatus[1]
                3                 | "${jobName}.step3" | 1                | stepStatus[2]  | stepStatus[2]
            }
        }
        def expectJob = expectTables.getTable('batch_job_execution')
        def expectStep = expectTables.getTable('batch_step_execution')

        when:
        int actualExitValue = jobLauncher.syncJob(new JobRequest(
                jobName: jobName,
                jobFilePath: "${JOBFILE_ROOT}/jobSequentialFlow.xml",
                jobParameter: "${jobParameter}"))

        def actualJob = adminDBUnitUtil.getIncludedColumnsTable('batch_job_execution',
                expectJob.tableMetaData.columns)
        def actualStep = adminDBUnitUtil.getIncludedColumnsTable('batch_step_execution',
                expectStep.tableMetaData.columns)
        then:
        exitValue == actualExitValue
        DBUnitUtil.assertEquals(expectJob, actualJob)

        DBUnitUtil.assertEquals(DBUnitUtil.createDynamicTable(expectStep, 0, stepTableRowCount), actualStep)

        where: "Test no.1 - no.3"
        jobName                  | failTarget     | jobParameter                                || jobStatus   | exitValue | stepTableRowCount | stepStatus
        'jobSequentialFlow'      | 'none(normal)' | ''                                          || 'COMPLETED' | 0         | 3                 | ['COMPLETED', 'COMPLETED', 'COMPLETED']
        'jobSequentialFlow'      | 'step1'        | 'failExecutionStep=jobSequentialFlow.step1' || 'FAILED'    | 255       | 1                 | ['FAILED'   , ''         , '']
        'jobSequentialFlow'      | 'step2'        | 'failExecutionStep=jobSequentialFlow.step2' || 'FAILED'    | 255       | 2                 | ['COMPLETED', 'FAILED'   , '']
        'jobSequentialFlow'      | 'step3'        | 'failExecutionStep=jobSequentialFlow.step3' || 'FAILED'    | 255       | 3                 | ['COMPLETED', 'COMPLETED', 'FAILED'   ]
        'jobSequentialOuterFlow' | 'none(normal)' | ''                                          || 'COMPLETED' | 0         | 3                 | ['COMPLETED', 'COMPLETED', 'COMPLETED']
    }

    // Testcase 2.
    @Unroll
    def "Test of conditional flow using 'next' and 'on' attribute. (#flowSequence)"() {

        setup:
        if (cleanupTable) {
            adminDBUnitUtil.dropAndCreateTable()
        }
        def expectTables = DBUnitUtil.createDataSet {
            batch_job_execution {
                job_execution_id | status       | exit_code
                1                | jobStatus[0] | jobStatus[0]
                2                | jobStatus[1] | jobStatus[1]
            }
            batch_step_execution {
                step_execution_id | step_name   | job_execution_id | status         | exit_code
                1                 | stepName[0] | 1                | stepStatus[0]  | exitStatus[0]
                2                 | stepName[1] | 1                | stepStatus[1]  | exitStatus[1]
                3                 | stepName[2] | 2                | stepStatus[2]  | exitStatus[2]
            }
        }
        def expectJob = expectTables.getTable('batch_job_execution')
        def expectStep = expectTables.getTable('batch_step_execution')

        when:
        int actualExitValue = jobLauncher.syncJob { JobLauncher.SyncJobArg arg ->
            arg.jobRequest = new JobRequest()
            arg.jobRequest.jobName = 'jobConditionalFlow'
            arg.jobRequest.jobFilePath = "${JOBFILE_ROOT}/jobConditionalFlow.xml"
            arg.jobRequest.jobParameter = "${jobParameter}"
            arg.sysprop = sysProp
        }

        def actualJob = adminDBUnitUtil.getIncludedColumnsTable('batch_job_execution',
                expectJob.tableMetaData.columns)
        def actualStep = adminDBUnitUtil.getIncludedColumnsTable('batch_step_execution',
                expectStep.tableMetaData.columns)

        then:
        actualExitValue == exitValue

        DBUnitUtil.assertEquals(DBUnitUtil.createDynamicTable(expectJob, 0, jobTableRowCount), actualJob)
        DBUnitUtil.assertEquals(DBUnitUtil.createDynamicTable(expectStep, 0, stepTableRowCount), actualStep)

        expectLog == mongoUtil.findOne(new LogCondition(level: 'ERROR', logger: AbstractJob.class.name, message: 'Encountered fatal error executing job'))?.throwable?.cause?.message

        where:
        flowSequence                                                        | jobParameter                                                            | sysProp            | cleanupTable || exitValue | jobStatus               |jobTableRowCount | stepTableRowCount | stepName                                                                             | stepStatus                            | exitStatus                              | expectLog
        'No.1 normal flow'                                                  | ''                                                                      | null               | true         || 0         | ['COMPLETED']           | 1               | 2                 | ['jobConditionalFlow.stepA', 'jobConditionalFlow.stepB']                             | ['COMPLETED', 'COMPLETED' ]           | ['COMPLETED', 'COMPLETED']              | null
        'No.2 step fail and alternative flow'                               | 'failExecutionStep=jobConditionalFlow.stepA'                            | null               | true         || 0         | ['COMPLETED']           | 1               | 2                 | ['jobConditionalFlow.stepA', 'jobConditionalFlow.stepC']                             | ['ABANDONED', 'COMPLETED' ]           | ['FAILED',    'COMPLETED']              | null
        'No.3 abort step because of exit code unmatched'                    | 'jobConditionalFlow.stepA=DEFAULT'                                      | null               | true         || 255       | ['FAILED']              | 1               | 1                 | ['jobConditionalFlow.stepA']                                                         | ['COMPLETED' ]                        | ['DEFAULT']                             | 'Next state not found in flow=jobConditionalFlow for state=jobConditionalFlow.stepA with exit status=DEFAULT'
        'No.4-1 step fail and alternative and fail'                         | 'failExecutionStep=jobConditionalFlow.stepA,jobConditionalFlow.stepC'   | null               | true         || 255       | ['FAILED']              | 1               | 2                 | ['jobConditionalFlow.stepA', 'jobConditionalFlow.stepC']                             | ['ABANDONED', 'FAILED' ]              | ['FAILED',    'FAILED']                 | null
        'No.4-2 restart and recovery failed step and ignore abandoned one'  | '-restart'                                                              | ['failSkip=true']  | false        || 0         | ['FAILED', 'COMPLETED'] | 2               | 3                 | ['jobConditionalFlow.stepA', 'jobConditionalFlow.stepC', 'jobConditionalFlow.stepC'] | ['ABANDONED', 'FAILED', 'COMPLETED' ] | ['FAILED',    'FAILED', 'COMPLETED']    | null
    }

    // Testcase 3.
    @Unroll
    def "Test of stop flow using 'stop' and 'fail' attribute. (#confirm)"() {
        setup:
        if (creanupTables) {
            adminDBUnitUtil.dropAndCreateTable()
        }
        def expectTables = DBUnitUtil.createDataSet {
            batch_job_execution {
                job_execution_id | status         | exit_code
                1                | jobStatus[0]   | jobExitStatus[0]
                2                | jobStatus[1]   | jobExitStatus[1]
            }
            batch_step_execution {
                step_execution_id | step_name   | status         | exit_code
                1                 | stepName[0] | stepStatus[0]  | exitStatus[0]
                2                 | stepName[1] | stepStatus[1]  | exitStatus[1]
                3                 | stepName[2] | stepStatus[2]  | exitStatus[2]
                4                 | stepName[3] | stepStatus[3]  | exitStatus[3]
            }
        }
        def expectJob = expectTables.getTable('batch_job_execution')
        def expectStep = expectTables.getTable('batch_step_execution')

        when:
        int actualExitValue = jobLauncher.syncJob(new JobRequest(
                jobName: 'jobStopFlow',
                jobFilePath: "${JOBFILE_ROOT}/jobStopFlow.xml",
                jobParameter: "${jobParameter}"))

        def actualJob = adminDBUnitUtil.getIncludedColumnsTable('batch_job_execution',
                expectJob.tableMetaData.columns)
        def actualStep = adminDBUnitUtil.getIncludedColumnsTable('batch_step_execution',
                expectStep.tableMetaData.columns)
        then:
        actualExitValue == exitValue

        DBUnitUtil.assertEquals(DBUnitUtil.createDynamicTable(expectJob, 0, jobTableRowCount), actualJob)
        DBUnitUtil.assertEquals(DBUnitUtil.createDynamicTable(expectStep, 0, stepTableRowCount), actualStep)

        where:
        confirm                                                                  | jobParameter                                      | creanupTables || jobTableRowCount | jobStatus                | jobExitStatus                   | exitValue | stepTableRowCount | stepName                                                                             | stepStatus                                           | exitStatus
        'No.1. Using end element.(undefine exit-code)'                           | 'jobStopFlow.step1=END_WITH_NO_EXIT_CODE'         | true          || 1                | ['COMPLETED', '']        | ['COMPLETED', '']               | 0         | 1                 | ['jobStopFlow.step1', '', '', '']                                                    | ['COMPLETED', '', '', '']                            | ['END_WITH_NO_EXIT_CODE', '', '', '']
        'No.2. Using end element.(define exit-code)'                             | 'jobStopFlow.step1=END_WITH_EXIT_CODE'            | true          || 1                | ['COMPLETED', '']        | ['COMPLETED_CUSTOM', '']        | 200       | 1                 | ['jobStopFlow.step1', '', '', '']                                                    | ['COMPLETED', '', '', '']                            | ['END_WITH_EXIT_CODE'   , '', '', '']
        'No.3,6,8. Normal flow(define end, fail and stop element. but non-stop)' | ''                                                | true          || 1                | ['COMPLETED', '']        | ['COMPLETED', '' ]              | 0         | 4                 | ['jobStopFlow.step1', 'jobStopFlow.step2', 'jobStopFlow.step3', 'jobStopFlow.step4'] | ['COMPLETED', 'COMPLETED', 'COMPLETED', 'COMPLETED'] | ['COMPLETED' , 'COMPLETED', 'COMPLETED', 'COMPLETED']
        'No.4. Using fail element.(undefine exit-code)'                          | 'jobStopFlow.step2=FORCE_FAIL_WITH_NO_EXIT_CODE'  | true          || 1                | ['FAILED', '']           | ['FAILED', '' ]                 | 255       | 2                 | ['jobStopFlow.step1', 'jobStopFlow.step2', '', '']                                   | ['COMPLETED', 'COMPLETED', '', '']                   | ['COMPLETED' , 'FORCE_FAIL_WITH_NO_EXIT_CODE', '', '']
        'No.5. Using fail element.(define exit-code)'                            | 'jobStopFlow.step2=FORCE_FAIL_WITH_EXIT_CODE'     | true          || 1                | ['FAILED', '']           | ['FAILED_CUSTOM', '']           | 202       | 2                 | ['jobStopFlow.step1', 'jobStopFlow.step2', '', '']                                   | ['COMPLETED', 'COMPLETED', '', '']                   | ['COMPLETED' , 'FORCE_FAIL_WITH_EXIT_CODE', '', '']
        'No.7-1 Using stop element'                                              | 'jobStopFlow.step3=FORCE_STOP'                    | true          || 1                | ['STOPPED', '']          | ['STOPPED', '']                 | 255       | 3                 | ['jobStopFlow.step1', 'jobStopFlow.step2', 'jobStopFlow.step3', '']                  | ['COMPLETED', 'COMPLETED', 'COMPLETED', '']          | ['COMPLETED' , 'COMPLETED', 'FORCE_STOP', '']
        'No.7-2 Using stop element(restart)'                                     | '-restart'                                        | false         || 2                | ['STOPPED', 'COMPLETED'] | ['STOPPED', 'COMPLETED']        | 0         | 4                 | ['jobStopFlow.step1', 'jobStopFlow.step2', 'jobStopFlow.step3', 'jobStopFlow.step4'] | ['COMPLETED', 'COMPLETED', 'COMPLETED', 'COMPLETED'] | ['COMPLETED' , 'COMPLETED', 'FORCE_STOP', 'COMPLETED']
        'No.9-1 TODO can\'t restart @BATCH-2315'                                 | 'jobStopFlow.step3=FORCE_STOP_WITH_EXIT_CODE'     | true          || 1                | ['STOPPED', '']          | ['STOPPED_CUSTOM', '']          | 201       | 3                 | ['jobStopFlow.step1', 'jobStopFlow.step2', 'jobStopFlow.step3', '']                  | ['COMPLETED', 'COMPLETED', 'COMPLETED', '']          | ['COMPLETED' , 'COMPLETED', 'FORCE_STOP_WITH_EXIT_CODE', '']
        'No.9-2 TODO can\'t restart @BATCH-2315'                                 | '-restart'                                        | false         || 2                | ['STOPPED', 'COMPLETED'] | ['STOPPED_CUSTOM', 'NOOP']      | 0         | 3                 | ['jobStopFlow.step1', 'jobStopFlow.step2', 'jobStopFlow.step3', '']                  | ['COMPLETED', 'COMPLETED', 'COMPLETED', '']          | ['COMPLETED' , 'COMPLETED', 'FORCE_STOP_WITH_EXIT_CODE', '']
    }

    // Testcase 4.
    @Unroll
    def "Test of taking over context from previous step. (#oriented) (#promotionValue) "() {
        setup:
        adminDBUnitUtil.dropAndCreateTable()

        when:
        int actualExitValue = jobLauncher.syncJob(new JobRequest(
                jobName: jobName,
                jobFilePath: "${JOBFILE_ROOT}/${jobName}.xml"))

        then:
        actualExitValue == 0
        mongoUtil.findOne(new LogCondition(level: 'INFO', logger: logger)).message == "taking over [promotion=$promotionValue]".toString()

        where:
        oriented       | jobName                   || logger                                  | promotionValue
        'No.1 tasklet' | 'jobPromotionalFlow'      || ConfirmPromotionalTasklet.class.name    | 'value1'
        'No.2 chunk'   | 'jobChunkPromotionalFlow' || PromotionTargetItemProcessor.class.name | 'value2'
    }

}
