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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06

import groovy.util.logging.Slf4j
import org.junit.Rule
import org.junit.rules.TestName
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobOperator
import org.springframework.context.ApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.DBUnitUtil
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.JobLauncher
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.JobRequest
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.LogCondition
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.MongoUtil
import spock.lang.Narrative
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files


/**
 * Function test of re-processing.
 *
 * @since 5.0.0
 */
@Slf4j
@Narrative("""
A list of test cases is shown below.
1. Rerun : Not tested
2. Restart
""")
class ReProcessingSpec extends Specification {

    @Rule
    TestName testName = new TestName()

    @Shared
            launcher = new JobLauncher()

    @Shared
            mongoUtil = new MongoUtil()

    @Shared
            adminDB = new DBUnitUtil('admin')

    @Shared
            jobDB = new DBUnitUtil('job')

    def setupSpec() {
        jobDB.dropAndCreateTable()
    }

    def setup() {
        log.debug("### Spec case of [{}]", testName.methodName)
        adminDB.dropAndCreateTable()
        mongoUtil.deleteAll()
    }

    def cleanupSpec() {
        mongoUtil.close()
        adminDB.close()
        jobDB.close()
    }

    // Testcase 2, test no.1
    def "Restart on number of items"() {
        setup:
        def outputFileName = "./files/test/output/ch06/reprocessing/plan_data_on_number.csv"
        def outputFile = new File(outputFileName)
        def backupFile = new File("./files/test/output/ch06/reprocessing/plan_data_on_number.csv.bak")
        Files.deleteIfExists(outputFile.toPath())
        Files.deleteIfExists(backupFile.toPath())
        setupIncorrectDataOnNumber()

        // expect files
        def expectBefore = new File("./files/expect/output/ch06/reprocrssing/plan_data_on_number_expect_before_restart.csv")
        def expectAfter = new File("./files/expect/output/ch06/reprocrssing/plan_data_on_number_expect_after_restart.csv")

        when:
        def exitValue1 = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/reprocessing/restartOnNumberOfItemsJob.xml',
                jobName: 'restartOnNumberOfItemsJob',
                jobParameter: "outputFile=${outputFileName}"
        ))
        Files.copy(outputFile.toPath(), backupFile.toPath())
        // Since the output result is reused at restart, the output file should not be deleted

        def failedJobExecution = adminDB.getTable("batch_job_execution")
        def failedStepExecution = adminDB.getTable("batch_step_execution")

        // fixed correct data
        setupCorrectDataOnNumber()

        // delete error job log.
        mongoUtil.deleteAll()

        def exitValue2 = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/reprocessing/restartOnNumberOfItemsJob.xml',
                jobName: 'restartOnNumberOfItemsJob',
                jobParameter: '-restart'
        ))

        def completedJobExecution = adminDB.getTable("batch_job_execution")
        def completedtepExecution = adminDB.getTable("batch_step_execution")

        then:
        exitValue1 == 255
        exitValue2 == 0

        backupFile.exists()
        outputFile.exists()
        backupFile.readLines() == expectBefore.readLines()
        outputFile.readLines() == expectAfter.readLines()

        // Verify first processing data at reprocessing.
        def readItemLog = mongoUtil.find(new LogCondition(message: ~/Read item: SalesPlanDetail/)).sort {l,r -> l.timestamp <=> r.timestamp}
        readItemLog.size() > 0
        readItemLog.get(0).message == "Read item: SalesPlanDetail{branchId='0001', year=2016, month=6, customerId='C0001', amount=1000}"

        failedJobExecution.rowCount == 1
        failedJobExecution.getValue(0, 'status') == 'FAILED'
        failedStepExecution.rowCount == 1
        failedStepExecution.getValue(0, 'status') == 'FAILED'
        failedStepExecution.getValue(0, 'step_name') == 'restartOnNumberOfItemsJob.step01'
        failedStepExecution.getValue(0, 'commit_count') == 1
        failedStepExecution.getValue(0, 'read_count') == 20
        failedStepExecution.getValue(0, 'write_count') == 10
        failedStepExecution.getValue(0, 'rollback_count') == 1

        completedJobExecution.rowCount == 2
        failedJobExecution.getValue(0, 'job_instance_id') == completedJobExecution.getValue(1, 'job_instance_id') // Confirm that the same job has restarted.
        completedJobExecution.getValue(1, 'status') == 'COMPLETED'
        completedtepExecution.rowCount == 2
        completedtepExecution.getValue(0, 'status') == 'FAILED'
        completedtepExecution.getValue(0, 'step_name') == 'restartOnNumberOfItemsJob.step01'
        completedtepExecution.getValue(0, 'commit_count') == 1
        completedtepExecution.getValue(0, 'read_count') == 20
        completedtepExecution.getValue(0, 'write_count') == 10
        completedtepExecution.getValue(0, 'rollback_count') == 1
        completedtepExecution.getValue(1, 'status') == 'COMPLETED'
        completedtepExecution.getValue(1, 'step_name') == 'restartOnNumberOfItemsJob.step01'
        completedtepExecution.getValue(1, 'commit_count') == 2
        completedtepExecution.getValue(1, 'read_count') == 14
        completedtepExecution.getValue(1, 'write_count') == 14
        completedtepExecution.getValue(1, 'rollback_count') == 0

        cleanup:
        Files.deleteIfExists(outputFile.toPath())
        Files.deleteIfExists(backupFile.toPath())
    }

    // Testcase 2, test no.2
    def "Restart on condition basis"() {
        setup:
        def outputFileName = "./files/test/output/ch06/reprocessing/plan_data_on_condition.csv"
        def outputFile = new File(outputFileName)
        def backupFile = new File("./files/test/output/ch06/reprocessing/plan_data_on_condition.csv.bak")
        Files.deleteIfExists(outputFile.toPath())
        Files.deleteIfExists(backupFile.toPath())
        setupIncorrectDataOnCondition()

        // expect files
        def expectBefore = new File("./files/expect/output/ch06/reprocrssing/plan_data_on_condition_expect_before_restart.csv")
        def expectAfter = new File("./files/expect/output/ch06/reprocrssing/plan_data_on_condition_expect_after_restart.csv")

        def jobRequest = new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/reprocessing/restartOnConditionBasisJob.xml',
                jobName: 'restartOnConditionBasisJob',
                jobParameter: "outputFile=${outputFileName}"
        )

        when:
        def exitValue1 = launcher.syncJob(jobRequest)
        Files.copy(outputFile.toPath(), backupFile.toPath())
        // Since the output result is reused at restart, the output file should not be deleted

        def failedJobExecution = adminDB.getTable("batch_job_execution")
        def failedStepExecution = adminDB.getTable("batch_step_execution")
        def failedStepExecutionContext = adminDB.getTable("batch_step_execution_context")

        // fixed correct data
        setupCorrectDataOnCondition()

        // delete error job log.
        mongoUtil.deleteAll()

        def exitValue2 = launcher.syncJob(jobRequest)

        def completedJobExecution = adminDB.getTable("batch_job_execution")
        def completedtepExecution = adminDB.getTable("batch_step_execution")
        def completedtepExecutionContext = adminDB.getTable("batch_step_execution_context")

        then:
        exitValue1 == 255
        exitValue2 == 0

        backupFile.exists()
        outputFile.exists()
        backupFile.readLines() == expectBefore.readLines()
        outputFile.readLines() == expectAfter.readLines()

        // Verify first processing data at reprocessing.
        def readItemLog = mongoUtil.find(new LogCondition(message: ~/Read item: SalesPlanDetail/)).sort {l,r -> l.timestamp <=> r.timestamp}
        readItemLog.size() > 0
        readItemLog.get(0).message == "Read item: SalesPlanDetail{branchId='0001', year=2016, month=6, customerId='C0001', amount=0}"

        failedJobExecution.rowCount == 1
        failedJobExecution.getValue(0, 'status') == 'FAILED'
        failedStepExecution.rowCount == 1
        failedStepExecution.getValue(0, 'status') == 'FAILED'
        failedStepExecution.getValue(0, 'step_name') == 'restartOnConditionBasisJob.step01'
        failedStepExecution.getValue(0, 'commit_count') == 1
        failedStepExecution.getValue(0, 'read_count') == 20
        failedStepExecution.getValue(0, 'write_count') == 10
        failedStepExecution.getValue(0, 'rollback_count') == 1
        def failedShortContext = failedStepExecutionContext.getValue(0, 'short_context')
        failedShortContext ==~ /.*read\.count","int":10.*/
        failedShortContext ==~ /.*Writer\.written","long":10.*/

        completedJobExecution.rowCount == 2
        failedJobExecution.getValue(0, 'job_instance_id') != completedJobExecution.getValue(1, 'job_instance_id') // Confirm that restarted as another job
        completedJobExecution.getValue(1, 'status') == 'COMPLETED'
        completedtepExecution.rowCount == 2
        completedtepExecution.getValue(0, 'status') == 'FAILED'
        completedtepExecution.getValue(0, 'step_name') == 'restartOnConditionBasisJob.step01'
        completedtepExecution.getValue(0, 'commit_count') == 1
        completedtepExecution.getValue(0, 'read_count') == 20
        completedtepExecution.getValue(0, 'write_count') == 10
        completedtepExecution.getValue(0, 'rollback_count') == 1
        completedtepExecution.getValue(1, 'status') == 'COMPLETED'
        completedtepExecution.getValue(1, 'step_name') == 'restartOnConditionBasisJob.step01'
        completedtepExecution.getValue(1, 'commit_count') == 2
        completedtepExecution.getValue(1, 'read_count') == 14
        completedtepExecution.getValue(1, 'write_count') == 14
        completedtepExecution.getValue(1, 'rollback_count') == 0
        def completedShortContext0 = completedtepExecutionContext.getValue(0, 'short_context')
        completedShortContext0 ==~ /.*read\.count","int":10.*/
        completedShortContext0 ==~ /.*Writer\.written","long":10.*/
        def completedShortContext1 = completedtepExecutionContext.getValue(1, 'short_context')
        completedShortContext1 ==~ /.*read\.count","int":15.*/
        completedShortContext1 ==~ /.*Writer\.written","long":14.*/

        cleanup:
        Files.deleteIfExists(outputFile.toPath())
        Files.deleteIfExists(backupFile.toPath())
    }


    // Testcase 2, test no.3
    def "Restart on number of items by job operator"() {
        setup:
        def outputFileName = "./files/test/output/ch06/reprocessing/plan_data_on_number.csv"
        def outputFile = new File(outputFileName)
        def backupFile = new File("./files/test/output/ch06/reprocessing/plan_data_on_number.csv.bak")
        Files.deleteIfExists(outputFile.toPath())
        Files.deleteIfExists(backupFile.toPath())
        setupIncorrectDataOnNumber()

        // expect files
        def expectBefore = new File("./files/expect/output/ch06/reprocrssing/plan_data_on_number_expect_before_restart.csv")
        def expectAfter = new File("./files/expect/output/ch06/reprocrssing/plan_data_on_number_expect_after_restart.csv")

        ApplicationContext context = new ClassPathXmlApplicationContext('META-INF/jobs/ch06/reprocessing/restartOnNumberOfItemsJob.xml')
        def jobOperator = (JobOperator) context.getBean("jobOperator")
        def jobExplorer = (JobExplorer) context.getBean("jobExplorer")

        when:
        def jobExecutionId = jobOperator.start('restartOnNumberOfItemsJob', "outputFile=${outputFileName}")
        def jobExecution = jobExplorer.getJobExecution(jobExecutionId)

        Files.copy(outputFile.toPath(), backupFile.toPath())
        // Since the output result is reused at restart, the output file should not be deleted

        // fixed correct data
        setupCorrectDataOnNumber()

        // delete error job log.
        mongoUtil.deleteAll()

        def restartJobExecutionId = jobOperator.restart(jobExecutionId)
        def restartjobExecution = jobExplorer.getJobExecution(restartJobExecutionId)

        then:
        jobExecution != null
        restartjobExecution != null
        jobExecution.getExitStatus() == ExitStatus.FAILED
        jobExecution.getStatus() == BatchStatus.FAILED
        jobExecution.getStepExecutions().size() == 1
        jobExecution.getStepExecutions().getAt(0).getStatus() == BatchStatus.FAILED
        restartjobExecution.getExitStatus() == ExitStatus.COMPLETED
        restartjobExecution.getStatus() == BatchStatus.COMPLETED
        restartjobExecution.getStepExecutions().size() == 1
        restartjobExecution.getStepExecutions().getAt(0).getStatus() == BatchStatus.COMPLETED

        backupFile.exists()
        outputFile.exists()
        backupFile.readLines() == expectBefore.readLines()
        outputFile.readLines() == expectAfter.readLines()

        // Verify first processing data at reprocessing.
        def readItemLog = mongoUtil.find(new LogCondition(message: ~/Read item: SalesPlanDetail/)).sort {l,r -> l.timestamp <=> r.timestamp}
        readItemLog.size() > 0
        readItemLog.get(0).message == "Read item: SalesPlanDetail{branchId='0001', year=2016, month=6, customerId='C0001', amount=1000}"

        cleanup:
        Files.deleteIfExists(outputFile.toPath())
        Files.deleteIfExists(backupFile.toPath())
        context.close()
    }

    // utils
    def setupIncorrectDataOnNumber() {
        jobDB.deleteAll(["sales_plan_detail"] as String[])
        // setup incorrect data
        jobDB.insert(DBUnitUtil.createDataSet({
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "0001"    | 2016 | 12    | "C0001"     | 1000
                "0001"    | 2016 | 11    | "C0001"     | 1000
                "0001"    | 2016 | 10    | "C0001"     | 1000
                "0001"    | 2016 | 9     | "C0001"     | 1000
                "0001"    | 2016 | 8     | "C0001"     | 1000
                "0001"    | 2016 | 7     | "C0001"     | -1000
                "0001"    | 2016 | 6     | "C0001"     | 1000
                "0001"    | 2016 | 5     | "C0001"     | 1000
                "0001"    | 2016 | 4     | "C0001"     | 1000
                "0001"    | 2016 | 3     | "C0001"     | 1000
                "0001"    | 2016 | 2     | "C0001"     | 1000
                "0001"    | 2016 | 1     | "C0001"     | 1000
                "0001"    | 2016 | 12    | "C0002"     | 1000
                "0001"    | 2016 | 11    | "C0002"     | 1000
                "0001"    | 2016 | 10    | "C0002"     | 1000
                "0001"    | 2016 | 9     | "C0002"     | 1000
                "0001"    | 2016 | 8     | "C0002"     | 1000
                "0001"    | 2016 | 7     | "C0002"     | 1000
                "0001"    | 2016 | 6     | "C0002"     | 1000
                "0001"    | 2016 | 5     | "C0002"     | 1000
                "0001"    | 2016 | 4     | "C0002"     | 1000
                "0001"    | 2016 | 3     | "C0002"     | 1000
                "0001"    | 2016 | 2     | "C0002"     | 1000
                "0001"    | 2016 | 1     | "C0002"     | 1000
            }
        }))
    }

    def setupCorrectDataOnNumber() {
        jobDB.deleteAll(["sales_plan_detail"] as String[])
        // setup incorrect data
        jobDB.insert(DBUnitUtil.createDataSet({
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "0001"    | 2016 | 12    | "C0001"     | 1000
                "0001"    | 2016 | 11    | "C0001"     | 1000
                "0001"    | 2016 | 10    | "C0001"     | 1000
                "0001"    | 2016 | 9     | "C0001"     | 1000
                "0001"    | 2016 | 8     | "C0001"     | 1000
                "0001"    | 2016 | 7     | "C0001"     | 1000
                "0001"    | 2016 | 6     | "C0001"     | 1000
                "0001"    | 2016 | 5     | "C0001"     | 1000
                "0001"    | 2016 | 4     | "C0001"     | 1000
                "0001"    | 2016 | 3     | "C0001"     | 1000
                "0001"    | 2016 | 2     | "C0001"     | 1000
                "0001"    | 2016 | 1     | "C0001"     | 1000
                "0001"    | 2016 | 12    | "C0002"     | 1000
                "0001"    | 2016 | 11    | "C0002"     | 1000
                "0001"    | 2016 | 10    | "C0002"     | 1000
                "0001"    | 2016 | 9     | "C0002"     | 1000
                "0001"    | 2016 | 8     | "C0002"     | 1000
                "0001"    | 2016 | 7     | "C0002"     | 1000
                "0001"    | 2016 | 6     | "C0002"     | 1000
                "0001"    | 2016 | 5     | "C0002"     | 1000
                "0001"    | 2016 | 4     | "C0002"     | 1000
                "0001"    | 2016 | 3     | "C0002"     | 1000
                "0001"    | 2016 | 2     | "C0002"     | 1000
                "0001"    | 2016 | 1     | "C0002"     | 1000
            }
        }))
    }

    def setupIncorrectDataOnCondition() {
        jobDB.deleteAll(["sales_plan_detail"] as String[])
        // setup incorrect data
        jobDB.insert(DBUnitUtil.createDataSet({
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "0001"    | 2016 | 12    | "C0001"     | 0
                "0001"    | 2016 | 11    | "C0001"     | 0
                "0001"    | 2016 | 10    | "C0001"     | 0
                "0001"    | 2016 | 9     | "C0001"     | 0
                "0001"    | 2016 | 8     | "C0001"     | 0
                "0001"    | 2016 | 7     | "C0001"     | -1000
                "0001"    | 2016 | 6     | "C0001"     | 0
                "0001"    | 2016 | 5     | "C0001"     | 0
                "0001"    | 2016 | 4     | "C0001"     | 0
                "0001"    | 2016 | 3     | "C0001"     | 0
                "0001"    | 2016 | 2     | "C0001"     | 0
                "0001"    | 2016 | 1     | "C0001"     | 0
                "0001"    | 2016 | 12    | "C0002"     | 0
                "0001"    | 2016 | 11    | "C0002"     | 0
                "0001"    | 2016 | 10    | "C0002"     | 0
                "0001"    | 2016 | 9     | "C0002"     | 0
                "0001"    | 2016 | 8     | "C0002"     | 0
                "0001"    | 2016 | 7     | "C0002"     | 0
                "0001"    | 2016 | 6     | "C0002"     | 0
                "0001"    | 2016 | 5     | "C0002"     | 0
                "0001"    | 2016 | 4     | "C0002"     | 0
                "0001"    | 2016 | 3     | "C0002"     | 0
                "0001"    | 2016 | 2     | "C0002"     | 0
                "0001"    | 2016 | 1     | "C0002"     | 0
            }
        }))
    }

    def setupCorrectDataOnCondition() {
        jobDB.deleteAll(["sales_plan_detail"] as String[])
        // setup incorrect data
        jobDB.insert(DBUnitUtil.createDataSet({
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "0001"    | 2016 | 12    | "C0001"     | 0
                "0001"    | 2016 | 11    | "C0001"     | 0
                "0001"    | 2016 | 10    | "C0001"     | 0
                "0001"    | 2016 | 9     | "C0001"     | 0
                "0001"    | 2016 | 8     | "C0001"     | 0
                "0001"    | 2016 | 7     | "C0001"     | 0
                "0001"    | 2016 | 6     | "C0001"     | 0
                "0001"    | 2016 | 5     | "C0001"     | 1000
                "0001"    | 2016 | 4     | "C0001"     | 1000
                "0001"    | 2016 | 3     | "C0001"     | 1000
                "0001"    | 2016 | 2     | "C0001"     | 1000
                "0001"    | 2016 | 1     | "C0001"     | 1000
                "0001"    | 2016 | 12    | "C0002"     | 0
                "0001"    | 2016 | 11    | "C0002"     | 0
                "0001"    | 2016 | 10    | "C0002"     | 0
                "0001"    | 2016 | 9     | "C0002"     | 0
                "0001"    | 2016 | 8     | "C0002"     | 0
                "0001"    | 2016 | 7     | "C0002"     | 0
                "0001"    | 2016 | 6     | "C0002"     | 0
                "0001"    | 2016 | 5     | "C0002"     | 1000
                "0001"    | 2016 | 4     | "C0002"     | 1000
                "0001"    | 2016 | 3     | "C0002"     | 1000
                "0001"    | 2016 | 2     | "C0002"     | 1000
                "0001"    | 2016 | 1     | "C0002"     | 1000
            }
        }))
    }

}
