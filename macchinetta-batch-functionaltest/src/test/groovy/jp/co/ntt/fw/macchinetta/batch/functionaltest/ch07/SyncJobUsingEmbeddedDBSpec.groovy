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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch07

import groovy.util.logging.Slf4j
import org.springframework.batch.core.job.AbstractJob
import org.springframework.dao.DuplicateKeyException
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.jobparameter.RefInXmlTasklet
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.flowcontrol.ConfirmPromotionalTasklet
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.DBUnitUtil
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.JobLauncher
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.JobRequest
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.LogCondition
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.MongoUtil
import spock.lang.Narrative
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * Function test of synchronous job using embedded database.
 *
 * @since 2.0.1
 */
@Slf4j
@Narrative("""
A list of test cases is shown below.
1. Test of JobParameters. JobParameterSpec 4.1.
2. Test of Transaction. TransactionSpec 1.1.1.
3. Test of DBAccess. DBAccessSpec 3.1.
4. Test of ReProcessing. ReProcessingSpec 2.2.
5.1 Test of FlowControll, error occurs. FlowControlSpec 4.
5.2 Test of FlowControll, no error occurs. FlowControlSpec 2.1-3.
6.1 Test of Parallel. ParallelAndMultipleSpec 1.3.
6.2 Test of Multiple. ParallelAndMultipleSpec 2.1-2.

A test was created as follows.
Tests are copied from each test case.
Somewhat modified the variable name etc.
Put commonly defined field in test case.
Delete the initialization process of JobRepository.
Delete results verification using JobRepository.
""")
class SyncJobUsingEmbeddedDBSpec extends Specification {

    @Shared
            jobLauncher = new UseH2JobLauncher(new JobLauncher())

    @Shared
            jobDBUnitUtil = new DBUnitUtil()

    @Shared
            mongoUtil = new MongoUtil()

    def setup() {
        log.debug("### Spec case of [{}]", this.specificationContext.currentIteration.displayName)
        jobDBUnitUtil.dropAndCreateTable()
        mongoUtil.deleteAll()
    }

    def cleanupSpec() {
        jobDBUnitUtil.close()
        mongoUtil.close()
    }

    // 1. Test of JobParameters. JobParameterSpec 4.1.
    def "embedded JobParameterSpec Set both job parameters and environment variable with different values, and refer to it in the XML file (bean definition)"() {
        when:
        int exitCode = jobLauncher.syncJob { JobLauncher.SyncJobArg arg ->
            arg.jobRequest = new JobRequest(
                    jobFilePath: 'META-INF/jobs/ch04/jobparameter/jobAndEnvValRefInXml.xml',
                    jobName: 'jobAndEnvValRefInXml',
                    jobParameter: "num=123")
            arg.env = ['str=Hello']
        }

        then:
        exitCode == 0
        def cursorFindOne = mongoUtil.findOne(
                new LogCondition(
                        logger: RefInXmlTasklet.class.name,
                        level: 'INFO'
                ))
        cursorFindOne.message == 'str = Hello, num = 123'
    }

    // 2. Test of Transaction. TransactionSpec 1.1.1.
    def "embedded TransactionSpec Transaction control test of chunk model, Confirm implementation of intermediate commit method"() {
        setup:
        jobDBUnitUtil.deleteAll(["sales_plan_detail"] as String[])
        def jobRequest = new JobRequest(
                jobFilePath: 'META-INF/jobs/common/jobSalesPlan01.xml',
                jobName: 'jobSalesPlan01',
                jobParameter: 'inputFile=./files/test/input/ch05/transaction/sales_plan_branch_0002_incorrect.csv'
        )
        def expectDataSet = DBUnitUtil.createDataSetFormCSV("./files/expect/database/ch05/transaction/")

        when:
        def exitValue = jobLauncher.syncJob(jobRequest)

        def planDetailTable = jobDBUnitUtil.getTable("sales_plan_detail")

        then:
        exitValue == 255
        DBUnitUtil.assertEquals(expectDataSet.getTable("sales_plan_branch_0002_expect_chunked"), planDetailTable)

        def log = mongoUtil.findOne(new LogCondition(
                message: 'Encountered an error executing step jobSalesPlan01.step01 in job jobSalesPlan01',
                level: 'ERROR'
        ))
        log != null
        log.throwable._class.name == DuplicateKeyException.class.name

        // Deleted results verification using JobRepository.
    }
    
    // 3. Test of DBAccess. DBAccessSpec 3.1.
    def "DB access and data-transform in ItemProcessor."() {

        setup:
        // Put commonly defined field in test case.
        def initCustomerMstDataSet = DBUnitUtil.createDataSet {
            customer_mst {
                customer_id | customer_name | customer_address | customer_tel | charge_branch_id | create_date | update_date
                "1000000000" | "CustomerName001" | "CustomerAddress001" | "11111111111" | "01" | "[now]" | "[now]"
                "2000000000" | "CustomerName002" | "CustomerAddress002" | "11111111111" | "02" | "[now]" | "[now]"
                "3000000000" | "CustomerName003" | "CustomerAddress003" | "11111111111" | "03" | "[now]" | "[now]"
            }
        }

        // Put commonly defined field in test case.
        def initSalesPerformanceDetailDataSet = DBUnitUtil.createDataSet {
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                "000001" | 2017 | 1 | "1000000000" | 1000
                "000002" | 2017 | 1 | "2000000000" | 1000
                "000003" | 2017 | 1 | "3000000000" | 1000
            }
        }

        jobDBUnitUtil.insert(initCustomerMstDataSet)
        jobDBUnitUtil.insert(initSalesPerformanceDetailDataSet)

        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/dbaccess/DBAccessByItemProcessor.xml',
                jobName: 'DBAccessByItemProcessor'))

        then:
        exitCode == 0

        def log = mongoUtil.find(new LogCondition(
                level: 'INFO',
                message: ~/UpdateItemFromDBProcessor is called./))
        log.size() > 0

        def actualTable = jobDBUnitUtil.getTable("sales_plan_detail")

        // Put commonly defined field in test case.
        def expectSalesPLanDetailDataSet = DBUnitUtil.createDataSet {
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "01" | 2017 | 1 | 1000000000 | 1000
                "02" | 2017 | 1 | 2000000000 | 1000
                "03" | 2017 | 1 | 3000000000 | 1000
            }
        }
        def expectTable = expectSalesPLanDetailDataSet.getTable("sales_plan_detail")
        DBUnitUtil.assertEquals(expectTable, actualTable)
    }

    // 4. Test of ReProcessing. ReProcessingSpec 2.2.
    def "embedded ReProcessingSpec Restart on condition basis"() {
        setup:
        def outputFileName = "./files/test/output/ch06/reprocessing/plan_data_on_condition.csv"
        def outputFile = new File(outputFileName)
        def backupFile = new File("./files/test/output/ch06/reprocessing/plan_data_on_condition.csv.bak")
        Files.deleteIfExists(outputFile.toPath())
        Files.deleteIfExists(backupFile.toPath())
        setupIncorrectDataOnCondition()

        // expect files
        def expectBefore = new File("./files/expect/output/ch06/reprocessing/plan_data_on_condition_expect_before_restart.csv")
        def expectAfter = new File("./files/expect/output/ch06/reprocessing/plan_data_on_condition_expect_after_restart.csv")

        def jobRequest = new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/reprocessing/restartOnConditionBasisJob.xml',
                jobName: 'restartOnConditionBasisJob',
                jobParameter: "outputFile=${outputFileName}"
        )

        when:
        def exitValue1 = jobLauncher.syncJob(jobRequest)
        Files.copy(outputFile.toPath(), backupFile.toPath())
        // Since the output result is reused at restart, the output file should not be deleted

        // fixed correct data
        setupCorrectDataOnCondition()

        // delete error job log.
        mongoUtil.deleteAll()

        def exitValue2 = jobLauncher.syncJob(jobRequest)

        then:
        exitValue1 == 255
        exitValue2 == 0

        backupFile.exists()
        outputFile.exists()
        backupFile.readLines() == expectBefore.readLines()
        outputFile.readLines() == expectAfter.readLines()

        // Verify first processing data at reprocessing.
        def readItemLog = mongoUtil.find(new LogCondition(message: ~/Read item: SalesPlanDetail/)).sort { l, r -> l.timestamp <=> r.timestamp }
        readItemLog.size() > 0
        readItemLog.get(0).message == "Read item: SalesPlanDetail{branchId='0001', year=2016, month=6, customerId='C0001', amount=0}"

        // Deleted results verification using JobRepository.

        cleanup:
        Files.deleteIfExists(outputFile.toPath())
        Files.deleteIfExists(backupFile.toPath())
    }

    // Setup method used in test case No.4
    def setupIncorrectDataOnCondition() {
        jobDBUnitUtil.deleteAll(["sales_plan_detail"] as String[])
        // setup incorrect data
        jobDBUnitUtil.insert(DBUnitUtil.createDataSet({
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "0001" | 2016 | 12 | "C0001" | 0
                "0001" | 2016 | 11 | "C0001" | 0
                "0001" | 2016 | 10 | "C0001" | 0
                "0001" | 2016 | 9 | "C0001" | 0
                "0001" | 2016 | 8 | "C0001" | 0
                "0001" | 2016 | 7 | "C0001" | -1000
                "0001" | 2016 | 6 | "C0001" | 0
                "0001" | 2016 | 5 | "C0001" | 0
                "0001" | 2016 | 4 | "C0001" | 0
                "0001" | 2016 | 3 | "C0001" | 0
                "0001" | 2016 | 2 | "C0001" | 0
                "0001" | 2016 | 1 | "C0001" | 0
                "0001" | 2016 | 12 | "C0002" | 0
                "0001" | 2016 | 11 | "C0002" | 0
                "0001" | 2016 | 10 | "C0002" | 0
                "0001" | 2016 | 9 | "C0002" | 0
                "0001" | 2016 | 8 | "C0002" | 0
                "0001" | 2016 | 7 | "C0002" | 0
                "0001" | 2016 | 6 | "C0002" | 0
                "0001" | 2016 | 5 | "C0002" | 0
                "0001" | 2016 | 4 | "C0002" | 0
                "0001" | 2016 | 3 | "C0002" | 0
                "0001" | 2016 | 2 | "C0002" | 0
                "0001" | 2016 | 1 | "C0002" | 0
            }
        }))
    }

    // Setup method used in test case No.4
    def setupCorrectDataOnCondition() {
        jobDBUnitUtil.deleteAll(["sales_plan_detail"] as String[])
        // setup incorrect data
        jobDBUnitUtil.insert(DBUnitUtil.createDataSet({
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "0001" | 2016 | 12 | "C0001" | 0
                "0001" | 2016 | 11 | "C0001" | 0
                "0001" | 2016 | 10 | "C0001" | 0
                "0001" | 2016 | 9 | "C0001" | 0
                "0001" | 2016 | 8 | "C0001" | 0
                "0001" | 2016 | 7 | "C0001" | 0
                "0001" | 2016 | 6 | "C0001" | 0
                "0001" | 2016 | 5 | "C0001" | 1000
                "0001" | 2016 | 4 | "C0001" | 1000
                "0001" | 2016 | 3 | "C0001" | 1000
                "0001" | 2016 | 2 | "C0001" | 1000
                "0001" | 2016 | 1 | "C0001" | 1000
                "0001" | 2016 | 12 | "C0002" | 0
                "0001" | 2016 | 11 | "C0002" | 0
                "0001" | 2016 | 10 | "C0002" | 0
                "0001" | 2016 | 9 | "C0002" | 0
                "0001" | 2016 | 8 | "C0002" | 0
                "0001" | 2016 | 7 | "C0002" | 0
                "0001" | 2016 | 6 | "C0002" | 0
                "0001" | 2016 | 5 | "C0002" | 1000
                "0001" | 2016 | 4 | "C0002" | 1000
                "0001" | 2016 | 3 | "C0002" | 1000
                "0001" | 2016 | 2 | "C0002" | 1000
                "0001" | 2016 | 1 | "C0002" | 1000
            }
        }))
    }

    // 5.1 Test of FlowControll, error occurs. FlowControlSpec 4.
    def "embedded FlowControlSpec Test of taking over context from previous step."() {
        // Deleted the initialization process of JobRepository.

        when:
        int actualExitValue = jobLauncher.syncJob(new JobRequest(
                jobName: 'jobPromotionalFlow',
                jobFilePath: "META-INF/jobs/ch08/flowcontrol/jobPromotionalFlow.xml"))

        then: "Test no.1"
        actualExitValue == 0
        mongoUtil.findOne(new LogCondition(level: 'INFO', logger: ConfirmPromotionalTasklet.class.name)).message == 'taking over [promotion=value1]'
    }

    // 5.2 Test of FlowControll, no error occurs. FlowControlSpec 2.1-3.
    @Unroll
    def "embedded FlowControlSpec Test of conditional flow using 'next' and 'on' attribute. (#flowSequence)"() {

        when:
        int actualExitValue = jobLauncher.syncJob { JobLauncher.SyncJobArg arg ->
            arg.jobRequest = new JobRequest()
            arg.jobRequest.jobName = 'jobConditionalFlow'
            arg.jobRequest.jobFilePath = "META-INF/jobs/ch08/flowcontrol/jobConditionalFlow.xml"
            arg.jobRequest.jobParameter = "${jobParameter}"
            arg.sysprop = sysProp
        }

        then:
        actualExitValue == exitValue

        // Deleted results verification using JobRepository.
        
        expectLog == mongoUtil.findOne(new LogCondition(level: 'ERROR', logger: AbstractJob.class.name, message: 'Encountered fatal error executing job'))?.throwable?.cause?.message

        where:
        flowSequence                                     | jobParameter                                 | sysProp | cleanupTable || exitValue | jobStatus | jobTableRowCount | stepTableRowCount | stepName | stepStatus | exitStatus | expectLog
        'No.1 normal flow'                               | ''                                           | null    | true         || 0 | ['COMPLETED'] | 1 | 2 | ['jobConditionalFlow.stepA', 'jobConditionalFlow.stepB'] | ['COMPLETED', 'COMPLETED'] | ['COMPLETED', 'COMPLETED'] | null
        'No.2 step fail and alternative flow'            | 'failExecutionStep=jobConditionalFlow.stepA' | null    | true         || 0 | ['COMPLETED'] | 1 | 2 | ['jobConditionalFlow.stepA', 'jobConditionalFlow.stepC'] | ['ABANDONED', 'COMPLETED'] | ['FAILED', 'COMPLETED'] | null
        'No.3 abort step because of exit code unmatched' | 'jobConditionalFlow.stepA=DEFAULT'           | null    | true         || 255 | ['FAILED'] | 1 | 1 | ['jobConditionalFlow.stepA'] | ['COMPLETED'] | ['DEFAULT'] | 'Next state not found in flow=jobConditionalFlow for state=jobConditionalFlow.stepA with exit status=DEFAULT'

        // delete test No.4, beacause it is not executable
    }

    // 6.1 Test of Parallel. ParallelAndMultipleSpec 1.3.
    def "embedded ParallelAndMultipleSpec Execute a parallel processing job from the middle"() {
        setup:
        jobDBUnitUtil.deleteAll(["sales_plan_detail", "sales_performance_detail"] as String[])
        jobDBUnitUtil.insert(DBUnitUtil.createDataSet({
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "0001" | 2016 | 12 | "C0001" | 9999
                "0001" | 2016 | 12 | "C0002" | 9999
                "0001" | 2016 | 12 | "C0003" | 9999
                "0001" | 2016 | 12 | "C0004" | 9999
                "0001" | 2016 | 12 | "C0005" | 9999
                "0002" | 2016 | 12 | "C0011" | 9999
                "0002" | 2016 | 12 | "C0012" | 9999
                "0002" | 2016 | 12 | "C0013" | 9999
                "0002" | 2016 | 12 | "C0014" | 9999
                "0002" | 2016 | 12 | "C0015" | 9999
            }
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                "0001" | 2016 | 12 | "C0001" | 9999
                "0001" | 2016 | 12 | "C0002" | 9999
                "0001" | 2016 | 12 | "C0003" | 9999
                "0001" | 2016 | 12 | "C0004" | 1000
                "0001" | 2016 | 12 | "C0005" | 9999
                "0002" | 2016 | 12 | "C0011" | 9999
                "0002" | 2016 | 12 | "C0012" | 9999
                "0002" | 2016 | 12 | "C0013" | 9999
                "0002" | 2016 | 12 | "C0014" | 9999
                "0002" | 2016 | 12 | "C0015" | 9999
            }
        }))

        def expectData = DBUnitUtil.createDataSet({
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "0001" | 2016 | 12 | "C0001" | 1000
                "0001" | 2016 | 12 | "C0002" | 2000
                "0001" | 2016 | 12 | "C0003" | 3000
                "0001" | 2016 | 12 | "C0004" | 4000
                "0001" | 2016 | 12 | "C0005" | 5000
                "0002" | 2016 | 12 | "C0011" | 1000
                "0002" | 2016 | 12 | "C0012" | 1000
                "0002" | 2016 | 12 | "C0013" | 1000
                "0002" | 2016 | 12 | "C0014" | 1000
                "0002" | 2016 | 12 | "C0015" | 1000
            }
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                "0001" | 2016 | 12 | "C0001" | 1000
                "0001" | 2016 | 12 | "C0002" | 1000
                "0001" | 2016 | 12 | "C0003" | 1000
                "0001" | 2016 | 12 | "C0004" | 1000
                "0001" | 2016 | 12 | "C0005" | 1000
                "0002" | 2016 | 12 | "C0011" | 1000
                "0002" | 2016 | 12 | "C0012" | 1100
                "0002" | 2016 | 12 | "C0013" | 1200
                "0002" | 2016 | 12 | "C0014" | 1300
                "0002" | 2016 | 12 | "C0015" | 1400
            }
        })
        def planInputFile = './files/test/input/ch08/parallelandmultiple/planData.csv'
        def performanceInputFile = './files/test/input/ch08/parallelandmultiple/performanceData.csv'

        when:
        def exitValue = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch08/parallelandmultiple/parallelRegisterJob.xml',
                jobName: 'parallelRegisterJob',
                jobParameter: "planInputFile=${planInputFile} performanceInputFile=${performanceInputFile}"
        ))

        then:
        exitValue == 0

        def actualPlanData = jobDBUnitUtil.getTable("sales_plan_detail")
        def actualPerformanceData = jobDBUnitUtil.getTable("sales_performance_detail")
        DBUnitUtil.assertEquals(expectData.getTable("sales_plan_detail"), actualPlanData)
        DBUnitUtil.assertEquals(expectData.getTable("sales_performance_detail"), actualPerformanceData)

        // Since it is processed in parallel, the start time of each step should have been executed before the end time of each step.
        def stepStartLog = mongoUtil.find(new LogCondition(message: ~/step started./)).sort { l, r -> l.message <=> r.message }
        def stepStoptLog = mongoUtil.find(new LogCondition(message: ~/step stopped./)).sort { l, r -> l.message <=> r.message }
        stepStartLog.size() == 3
        stepStoptLog.size() == 3
        def perprocessIndex = stepStartLog.findIndexOf { it.thread == "main" }
        perprocessIndex != -1
        stepStartLog.remove(perprocessIndex)
        def preSetpStopLog = stepStoptLog.remove(perprocessIndex)
        stepStartLog.get(0).timestamp < stepStoptLog.get(1).timestamp
        stepStartLog.get(1).timestamp < stepStoptLog.get(0).timestamp
        stepStartLog.every { it.thread.contains("parallelTaskExecutor") }
        stepStoptLog.every { it.thread.contains("parallelTaskExecutor") }

        preSetpStopLog.timestamp < stepStartLog.get(0).timestamp
        preSetpStopLog.timestamp < stepStartLog.get(1).timestamp

        // Deleted results verification using JobRepository.
    }

    // 6.2 Test of Multiple. ParallelAndMultipleSpec 2.1-2.
    @Unroll
    def "embedded ParallelAndMultipleSpec Execute a job that performs multiple processing with unknown number of partitions[#description]"() {
        setup:
        jobDBUnitUtil.deleteAll(["sales_performance_detail", "invoice", "branch_mst", "customer_mst"] as String[])
        jobDBUnitUtil.insert(DBUnitUtil.createDataSet({
            branch_mst {
                branch_id | branch_name | branch_address | branch_tel | create_date
                "0001" | "Tokyo" | "DUMMY" | "DUMMY" | '[now]'
                "0002" | "Osaka" | "DUMMY" | "DUMMY" | '[now]'
                "0003" | "Nagoya" | "DUMMY" | "DUMMY" | '[now]'
            }
            customer_mst {
                customer_id | customer_name | customer_address | customer_tel | charge_branch_id | create_date
                "C0001" | "CUSTOMER01" | "DUMMY" | "DUMMY" | "0001" | '[now]'
                "C0002" | "CUSTOMER02" | "DUMMY" | "DUMMY" | "0002" | '[now]'
                "C0003" | "CUSTOMER03" | "DUMMY" | "DUMMY" | "0003" | '[now]'
                "C0004" | "CUSTOMER04" | "DUMMY" | "DUMMY" | "0001" | '[now]'
                "C0005" | "CUSTOMER05" | "DUMMY" | "DUMMY" | "0002" | '[now]'
                "C0006" | "CUSTOMER06" | "DUMMY" | "DUMMY" | "0003" | '[now]'
            }
            invoice {
                invoice_no | invoice_date | invoice_amount | customer_id
                "INVOICE101" | '2016-12-01' | 1100 | "C0001"
                "INVOICE102" | '2016-11-02' | 1200 | "C0001"
                "INVOICE103" | '2016-10-03' | 1300 | "C0001"
                "INVOICE201" | '2016-12-01' | 2100 | "C0002"
                "INVOICE202" | '2016-11-02' | 2200 | "C0002"
                "INVOICE203" | '2016-10-03' | 2300 | "C0002"
                "INVOICE301" | '2016-12-01' | 3100 | "C0003"
                "INVOICE302" | '2016-11-02' | 3200 | "C0003"
                "INVOICE303" | '2016-10-03' | 3300 | "C0003"
                "INVOICE401" | '2016-12-01' | 4100 | "C0004"
                "INVOICE402" | '2016-11-02' | 4200 | "C0004"
                "INVOICE403" | '2016-10-03' | 4300 | "C0004"
                "INVOICE501" | '2016-12-01' | 5100 | "C0005"
                "INVOICE502" | '2016-11-02' | 5200 | "C0005"
                "INVOICE503" | '2016-10-03' | 5300 | "C0005"
                "INVOICE601" | '2016-12-01' | 6100 | "C0006"
                "INVOICE602" | '2016-11-02' | 6200 | "C0006"
                "INVOICE603" | '2016-10-03' | 6300 | "C0006"
            }
        }))

        def expectData = DBUnitUtil.createDataSet({
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                "0001" | 2016 | 10 | "C0001" | 1300
                "0001" | 2016 | 10 | "C0004" | 4300
                "0001" | 2016 | 11 | "C0001" | 1200
                "0001" | 2016 | 11 | "C0004" | 4200
                "0001" | 2016 | 12 | "C0001" | 1100
                "0001" | 2016 | 12 | "C0004" | 4100
                "0002" | 2016 | 10 | "C0002" | 2300
                "0002" | 2016 | 10 | "C0005" | 5300
                "0002" | 2016 | 11 | "C0002" | 2200
                "0002" | 2016 | 11 | "C0005" | 5200
                "0002" | 2016 | 12 | "C0002" | 2100
                "0002" | 2016 | 12 | "C0005" | 5100
                "0003" | 2016 | 10 | "C0003" | 3300
                "0003" | 2016 | 10 | "C0006" | 6300
                "0003" | 2016 | 11 | "C0003" | 3200
                "0003" | 2016 | 11 | "C0006" | 6200
                "0003" | 2016 | 12 | "C0003" | 3100
                "0003" | 2016 | 12 | "C0006" | 6100
            }
        })

        expect:
        def exitValue = jobLauncher.syncJob { JobLauncher.SyncJobArg arg ->
            arg.jobRequest = new JobRequest(
                    jobFilePath: 'META-INF/jobs/ch08/parallelandmultiple/multipleInvoiceSummarizeJob.xml',
                    jobName: 'multipleInvoiceSummarizeJob'
            )
            arg.env = ["grid.size=0", "thread.size=${threadSize}"] as String[]
            arg.timeout = 120 * 1000L
            arg.timeUnit = TimeUnit.MILLISECONDS
        }
        exitValue == 0

        DBUnitUtil.assertEquals(expectData.getTable("sales_performance_detail"), jobDBUnitUtil.getTable("sales_performance_detail"))

        def workerStepLog = mongoUtil.find(new LogCondition(message: ~/step started./))
        workerStepLog.size() == divisionNum
        workerStepLog.any { it.thread == 'parallelTaskExecutor-1' } == thead1exists
        workerStepLog.any { it.thread == 'parallelTaskExecutor-2' } == thead2exists
        workerStepLog.any { it.thread == 'parallelTaskExecutor-3' } == thead3exists

        // Deleted results verification using JobRepository.

        where:
        description                           | divisionNum | threadSize || thead1exists | thead2exists | thead3exists
        "divisionNum is less than threadSize" | 3           | 2          || true | true | false
        "divisionNum is equal to threadSize"  | 3           | 3          || true | true | true
        "divisionNum is more than threadSize" | 3           | 4          || true | true | true
    }

    // Specify the profile embedded and start the syncJob
    class UseH2JobLauncher {
        private delegate

        UseH2JobLauncher(delegate) {
            this.delegate = delegate
        }

        int syncJob(JobRequest jobRequest, long timeout = 120 * 1000L, TimeUnit timeUnit = TimeUnit.MILLISECONDS,
                    String[] env = [], String[] sysprop = []) {
            if (sysprop == null) {
                sysprop = ['spring.profiles.active=embedded']
            } else {
                sysprop += 'spring.profiles.active=embedded'
            }
            delegate.syncJob(jobRequest, timeout, timeUnit, env, sysprop)
        }

        int syncJob(Closure c) {
            def syncJobArg = new JobLauncher.SyncJobArg()
            c(syncJobArg)
            syncJob(syncJobArg.jobRequest, syncJobArg.timeout, syncJobArg.timeUnit, syncJobArg.env, syncJobArg.sysprop)
        }
    }
}
