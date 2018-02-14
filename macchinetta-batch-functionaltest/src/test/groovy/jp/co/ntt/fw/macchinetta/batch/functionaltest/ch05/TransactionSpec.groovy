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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05

import groovy.util.logging.Slf4j
import org.junit.Rule
import org.junit.rules.TestName
import org.springframework.dao.DuplicateKeyException
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.component.ItemWriteException
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
 * Function test of transaction control.
 *
 * @since 5.0.0
 */
@Slf4j
@Narrative("""
A list of test cases is shown below.
1. Transaction control test of a single data source
1-1. Transaction control test of chunk model
1-2. Tasklet Model Transaction Control Test
1-3. Transaction control test of non-transactional data source
2. Transaction control test for multiple data sources
2-1. Acquisition test from multiple data sources
2-2. Test output to multiple data sources
""")
class TransactionSpec extends Specification {

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

    // Testcase 1.1, test no.1
    def "Transaction control test of chunk model, Confirm implementation of intermediate commit method"() {
        setup:
        jobDB.deleteAll(["sales_plan_detail"] as String[])
        def jobRequest = new JobRequest(
                jobFilePath: 'META-INF/jobs/common/jobSalesPlan01.xml',
                jobName: 'jobSalesPlan01',
                jobParameter: 'inputFile=./files/test/input/ch05/transaction/sales_plan_branch_0002_incorrect.csv'
        )
        def expectDataSet = DBUnitUtil.createDataSetFormCSV("./files/expect/database/ch05/transaction/")

        when:
        def exitValue = launcher.syncJob(jobRequest)

        def planDetailTable = jobDB.getTable("sales_plan_detail")

        then:
        exitValue == 255
        DBUnitUtil.assertEquals(expectDataSet.getTable("sales_plan_branch_0002_expect_chunked"), planDetailTable)

        def log = mongoUtil.findOne(new LogCondition(
                message: 'Encountered an error executing step jobSalesPlan01.step01 in job jobSalesPlan01',
                level: 'ERROR'
        ))
        log != null
        log.throwable._class.name == DuplicateKeyException.class.name
        def stepExecution = adminDB.getTable("batch_step_execution")
        stepExecution.getValue(0, 'commit_count') == 5
        stepExecution.getValue(0, 'read_count') == 60
        stepExecution.getValue(0, 'write_count') == 50
        stepExecution.getValue(0, 'rollback_count') == 1
    }

    // Testcase 1.2, test no.1
    def "Transaction control test of tasklet model, Confirm implementation of intermediate commit method"() {
        setup:
        jobDB.deleteAll(["sales_plan_detail"] as String[])
        def jobRequest = new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/transaction/createSalesPlanChunkTranTask.xml',
                jobName: 'createSalesPlanChunkTranTask',
                jobParameter: 'inputFile=./files/test/input/ch05/transaction/sales_plan_branch_0002_incorrect.csv'
        )
        def expectDataSet = DBUnitUtil.createDataSetFormCSV("./files/expect/database/ch05/transaction/")

        when:
        def exitValue = launcher.syncJob(jobRequest)

        def planDetailTable = jobDB.getTable("sales_plan_detail")

        then:
        exitValue == 255
        DBUnitUtil.assertEquals(expectDataSet.getTable("sales_plan_branch_0002_expect_chunked"), planDetailTable)

        def log = mongoUtil.findOne(new LogCondition(
                message: 'Encountered an error executing step createSalesPlanChunkTranTask.step01 in job createSalesPlanChunkTranTask',
                level: 'ERROR'
        ))
        log != null
        log.throwable._class.name == DuplicateKeyException.class.name
        def stepExecution = adminDB.getTable("batch_step_execution")
        stepExecution.getValue(0, 'commit_count') == 0
        stepExecution.getValue(0, 'read_count') == 0
        stepExecution.getValue(0, 'write_count') == 0
        stepExecution.getValue(0, 'rollback_count') == 1
    }

    // Testcase 1.2, test no.2
    def "Transaction control test of tasklet model, Confirmation of collective commit method realization"() {
        setup:
        jobDB.deleteAll(["sales_plan_detail"] as String[])
        def jobRequest = new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/transaction/createSalesPlanSingleTranTask.xml',
                jobName: 'createSalesPlanSingleTranTask',
                jobParameter: 'inputFile=./files/test/input/ch05/transaction/sales_plan_branch_0002_incorrect.csv'
        )

        when:
        def exitValue = launcher.syncJob(jobRequest)

        def planDetailTable = jobDB.getTable("sales_plan_detail")

        then:
        exitValue == 255
        planDetailTable.rowCount == 0

        def log = mongoUtil.findOne(new LogCondition(
                message: 'Encountered an error executing step createSalesPlanSingleTranTask.step01 in job createSalesPlanSingleTranTask',
                level: 'ERROR'
        ))
        log != null
        log.throwable._class.name == DuplicateKeyException.class.name
        def stepExecution = adminDB.getTable("batch_step_execution")
        stepExecution.getValue(0, 'commit_count') == 0
        stepExecution.getValue(0, 'read_count') == 0
        stepExecution.getValue(0, 'write_count') == 0
        stepExecution.getValue(0, 'rollback_count') == 1
    }

    // Testcase 1.3, test no.1
    def "Transaction control at file output, transaction enabled"() {
        setup:
        jobDB.deleteAll(["customer_mst", "invoice"] as String[])
        jobDB.insert(DBUnitUtil.createDataSetFormCSV("./files/test/input/ch05/transaction/case_1_3"))

        def outputFileName = "./files/test/output/ch05/transaction/sales_plan_branch_0003_output_tran_enabled.csv"
        def outputFile = new File(outputFileName)
        def expectFile = new File("./files/expect/output/ch05/transaction/sales_plan_detail_branch_0003_expect_tran_enabled.csv")

        def jobRequest = new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/transaction/outputSalesPlanDetailWithTran.xml',
                jobName: 'outputSalesPlanDetailWithTran',
                jobParameter: "branchId=0003 outputFile=${outputFileName}"
        )

        when:
        def exitValue = launcher.syncJob(jobRequest)

        then:
        exitValue == 255
        outputFile.exists()
        outputFile.readLines() == expectFile.readLines()

        def log = mongoUtil.findOne(new LogCondition(
                message: 'Encountered an error executing step outputSalesPlanDetailWithTran.step01 in job outputSalesPlanDetailWithTran',
                level: 'ERROR'
        ))
        log != null
        log.throwable._class.name == ItemWriteException.class.name
        def stepExecution = adminDB.getTable("batch_step_execution")
        stepExecution.getValue(0, 'commit_count') == 2
        stepExecution.getValue(0, 'read_count') == 30
        stepExecution.getValue(0, 'write_count') == 20
        stepExecution.getValue(0, 'rollback_count') == 1

        cleanup:
        Files.deleteIfExists(outputFile.toPath())
    }

    // Testcase 1.3, test no.2
    def "Transaction control at file output, transaction disabled"() {
        setup:
        jobDB.deleteAll(["customer_mst", "invoice"] as String[])
        jobDB.insert(DBUnitUtil.createDataSetFormCSV("./files/test/input/ch05/transaction/case_1_3"))

        def outputFileName = "./files/test/output/ch05/transaction/sales_plan_branch_0003_output_tran_disabled.csv"
        def outputFile = new File(outputFileName)
        def expectFile = new File("./files/expect/output/ch05/transaction/sales_plan_detail_branch_0003_expect_tran_disabled.csv")

        def jobRequest = new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/transaction/outputSalesPlanDetailWithNoTran.xml',
                jobName: 'outputSalesPlanDetailWithNoTran',
                jobParameter: "branchId=0003 outputFile=${outputFileName}"
        )

        when:
        def exitValue = launcher.syncJob(jobRequest)

        then:
        exitValue == 255
        outputFile.exists()
        outputFile.readLines() == expectFile.readLines()

        def log = mongoUtil.findOne(new LogCondition(
                message: 'Encountered an error executing step outputSalesPlanDetailWithNoTran.step01 in job outputSalesPlanDetailWithNoTran',
                level: 'ERROR'
        ))
        log != null
        log.throwable._class.name == ItemWriteException.class.name
        def stepExecution = adminDB.getTable("batch_step_execution")
        stepExecution.getValue(0, 'commit_count') == 2
        stepExecution.getValue(0, 'read_count') == 30
        stepExecution.getValue(0, 'write_count') == 20
        stepExecution.getValue(0, 'rollback_count') == 1

        cleanup:
        Files.deleteIfExists(outputFile.toPath())
    }

    // Testcase 2.1, test no.1
    def "Multiple data source: acquisition of accompanying data before step execution"() {
        setup:
        def adminDB2 = new DBUnitUtil('admin')
        createIntialDataForTestcase2_1(adminDB2)

        def outputFileName = "./files/test/output/ch05/transaction/customer_with_branch_before_step.csv"
        def outputFile = new File(outputFileName)
        def expectFile = new File("./files/expect/output/ch05/transaction/customer_with_branch_expect.csv")

        def jobRequest = new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/transaction/outputAllCustomerList01.xml',
                jobName: 'outputAllCustomerList01',
                jobParameter: "outputFile=${outputFileName}"
        )
        def sql = '==>  Preparing: SELECT branch_id AS branchId, branch_name AS branchName, ' +
                'branch_address AS branchAddress, branch_tel AS branchTel, create_date AS createDate, ' +
                'update_date AS updateDate FROM branch_mst ORDER BY branch_id '

        when:
        def exitValue = launcher.syncJob(jobRequest)

        def sqlLogs = mongoUtil.find(new LogCondition(message: sql))

        then:
        exitValue == 0
        outputFile.exists()
        outputFile.readLines() == expectFile.readLines()
        sqlLogs.size() == 1

        def stepExecution = adminDB.getTable("batch_step_execution")
        stepExecution.getValue(0, 'commit_count') == 1
        stepExecution.getValue(0, 'read_count') == 5
        stepExecution.getValue(0, 'write_count') == 5
        stepExecution.getValue(0, 'rollback_count') == 0

        cleanup:
        // Drop branch_mst in adminDB.
        adminDB2.executeSqlScript(adminDB.conf.ch05.customedDropSqlsFilePaths as String[])
        adminDB2.close()
        Files.deleteIfExists(outputFile.toPath())
    }
    // Testcase 2.1, test no.2
    def "Multiple data source: attendant data is acquired every time by ItemProcessor"() {
        setup:
        def adminDB2 = new DBUnitUtil('admin')
        createIntialDataForTestcase2_1(adminDB2)

        def outputFileName = "./files/test/output/ch05/transaction/customer_with_branch_before_step.csv"
        def outputFile = new File(outputFileName)
        def expectFile = new File("./files/expect/output/ch05/transaction/customer_with_branch_expect.csv")

        def jobRequest = new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/transaction/outputAllCustomerList02.xml',
                jobName: 'outputAllCustomerList02',
                jobParameter: "outputFile=${outputFileName}"
        )
        def sql = '==>  Preparing: SELECT branch_id AS branchId, branch_name AS branchName, ' +
                'branch_address AS branchAddress, branch_tel AS branchTel, create_date AS createDate, ' +
                'update_date AS updateDate FROM branch_mst WHERE branch_id = ? '

        when:
        def exitValue = launcher.syncJob(jobRequest)

        def sqlLogs = mongoUtil.find(new LogCondition(message: sql))

        then:
        exitValue == 0
        outputFile.exists()
        outputFile.readLines() == expectFile.readLines()
        sqlLogs.size() == 1

        def stepExecution = adminDB.getTable("batch_step_execution")
        stepExecution.getValue(0, 'commit_count') == 1
        stepExecution.getValue(0, 'read_count') == 5
        stepExecution.getValue(0, 'write_count') == 5
        stepExecution.getValue(0, 'rollback_count') == 0

        cleanup:
        // Drop branch_mst in adminDB.
        adminDB2.executeSqlScript(adminDB.conf.ch05.customedDropSqlsFilePaths as String[])
        adminDB2.close()
        Files.deleteIfExists(outputFile.toPath())

    }

    // Testcase 2.2, test no.1
    def "Output to multiple data sources, output in multiple steps"() {
        setup:
        // Create branch_mst in adminDB.
        def adminDB2 = new DBUnitUtil('admin')
        adminDB2.executeSqlScript(adminDB.conf.ch05.customedCreateSqlFilePaths as String[])
        jobDB.deleteAll(["customer_mst"] as String[])
        adminDB2.deleteAll(["branch_mst"] as String[])

        def branchInputFileName = "./files/test/input/ch05/transaction/branch_mst.csv"
        def customerInputFileName = "./files/test/input/ch05/transaction/customer_mst.csv"
        def customerInputFile = new File(customerInputFileName)
        def customerIncorrectInputFile = new File("./files/test/input/ch05/transaction/customer_mst_incorrect.csv")
        Files.deleteIfExists(customerInputFile.toPath())
        customerInputFile << customerIncorrectInputFile.readBytes()

        def jobRequest = new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/transaction/masterUpdatePerStepJob.xml',
                jobName: 'masterUpdatePerStepJob',
                jobParameter: "branchInputFile=${branchInputFileName} customerInputFile=${customerInputFileName}"
        )

        def expectDataSet = DBUnitUtil.createDataSetFormCSV("./files/expect/database/ch05/transaction/")

        when:
        def exitValue = launcher.syncJob(jobRequest)

        def actualBranch = adminDB2.getExcludedColumnsTable("branch_mst", ["create_date", "update_date"] as String[])
        def actualCustomer = jobDB.getExcludedColumnsTable("customer_mst", ["create_date", "update_date"] as String[])


        def stepExecution_admin = adminDB2.createQueryTable("stepExecution",
                "SELECT status FROM batch_step_execution WHERE step_name = 'masterUpdatePerStepJob.admin'")
        def stepExecution_job = adminDB.createQueryTable("stepExecution",
                "SELECT status FROM batch_step_execution WHERE step_name = 'masterUpdatePerStepJob.job'")

        then:
        exitValue == 255
        DBUnitUtil.assertEquals(expectDataSet.getTable("branch_mst"), actualBranch)
        DBUnitUtil.assertEquals(expectDataSet.getTable("customer_mst_commit"), actualCustomer)
        stepExecution_admin.getValue(0, "status") == 'COMPLETED'
        stepExecution_job.getValue(0, "status") == 'FAILED'

        cleanup:
        // Drop branch_mst in adminDB.
        adminDB2.executeSqlScript(adminDB.conf.ch05.customedDropSqlsFilePaths as String[])
        adminDB2.close()
        Files.deleteIfExists(customerInputFile.toPath())
    }

    // Testcase 2.2, test no.2
    def "Output to multiple data sources, output in one step with chained transaction"() {
        setup:
        // Create branch_mst in adminDB.
        def adminDB2 = new DBUnitUtil('admin')
        adminDB2.executeSqlScript(adminDB.conf.ch05.customedCreateSqlFilePaths as String[])
        jobDB.deleteAll(["customer_mst"] as String[])
        adminDB2.deleteAll(["branch_mst"] as String[])

        def inputFileName = "./files/test/input/ch05/transaction/composite_mst.csv"
        def inputFile = new File(inputFileName)
        def incorrectInputFile = new File("./files/test/input/ch05/transaction/composite_mst_incorrect.csv")
        Files.deleteIfExists(inputFile.toPath())
        inputFile << incorrectInputFile.readBytes()

        def jobRequest = new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/transaction/masterUpdateChainedTransactionJob.xml',
                jobName: 'masterUpdateChainedTransactionJob',
                jobParameter: "inputFile=${inputFileName}"
        )

        def expectDataSet = DBUnitUtil.createDataSetFormCSV("./files/expect/database/ch05/transaction/")

        when:
        def exitValue = launcher.syncJob(jobRequest)

        def actualBranch = adminDB2.getExcludedColumnsTable("branch_mst", ["create_date", "update_date"] as String[])
        def actualCustomer = jobDB.getExcludedColumnsTable("customer_mst", ["create_date", "update_date"] as String[])

        then:
        exitValue == 255
        DBUnitUtil.assertEquals(expectDataSet.getTable("branch_mst_commit"), actualBranch)
        DBUnitUtil.assertEquals(expectDataSet.getTable("customer_mst_commit"), actualCustomer)

        def stepExecution = adminDB2.getTable("batch_step_execution")
        stepExecution.getValue(0, 'commit_count') == 2
        stepExecution.getValue(0, 'read_count') == 30
        stepExecution.getValue(0, 'write_count') == 20
        stepExecution.getValue(0, 'rollback_count') == 1

        cleanup:
        // Drop branch_mst in adminDB.
        adminDB2.executeSqlScript(adminDB.conf.ch05.customedDropSqlsFilePaths as String[])
        adminDB2.close()
        Files.deleteIfExists(inputFile.toPath())
    }

    // Testcase 2.2, test no.3
    def "Output to multiple data sources, simultaneous processing of transactional and non-transactional resources, transaction enabled"() {
        setup:
        jobDB.deleteAll(["customer_mst"] as String[])

        def inputFileName = "./files/test/input/ch05/transaction/customer_mst.csv"
        def outputFileName = "./files/test/output/ch05/transaction/customer_mst_tran_enabled.csv"
        def inputFile = new File(inputFileName)
        def outputFile = new File(outputFileName)
        def expectFile = new File('./files/expect/output/ch05/transaction/customer_mst_expect_tran_enabled.csv')
        def incorrectInputFile = new File("./files/test/input/ch05/transaction/customer_mst_incorrect.csv")
        Files.deleteIfExists(inputFile.toPath())
        Files.deleteIfExists(outputFile.toPath())
        inputFile << incorrectInputFile.readBytes()

        def jobRequest = new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/transaction/nonTransactionalResourceEnableTransactionJob.xml',
                jobName: 'nonTransactionalResourceEnableTransactionJob',
                jobParameter: "inputFile=${inputFileName} outputFile=${outputFileName}"
        )

        def expectDataSet = DBUnitUtil.createDataSetFormCSV("./files/expect/database/ch05/transaction/")

        when:
        def exitValue = launcher.syncJob(jobRequest)
        def actualCustomer = jobDB.getExcludedColumnsTable("customer_mst", ["create_date", "update_date"] as String[])

        then:
        exitValue == 255
        DBUnitUtil.assertEquals(expectDataSet.getTable("customer_mst_commit"), actualCustomer)
        outputFile.exists()
        outputFile.readLines() == expectFile.readLines()

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
        Files.deleteIfExists(outputFile.toPath())
    }

    // Testcase 2.2, test no.4
    def "Output to multiple data sources, simultaneous processing of transactional and non-transactional resources, transaction disabled"() {
        setup:
        jobDB.deleteAll(["customer_mst"] as String[])

        def inputFileName = "./files/test/input/ch05/transaction/customer_mst.csv"
        def outputFileName = "./files/test/output/ch05/transaction/customer_mst_tran_enabled.csv"
        def inputFile = new File(inputFileName)
        def outputFile = new File(outputFileName)
        def expectFile = new File('./files/expect/output/ch05/transaction/customer_mst_expect_tran_disabled.csv')
        def incorrectInputFile = new File("./files/test/input/ch05/transaction/customer_mst_incorrect.csv")
        Files.deleteIfExists(inputFile.toPath())
        Files.deleteIfExists(outputFile.toPath())
        inputFile << incorrectInputFile.readBytes()

        def jobRequest = new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/transaction/nonTransactionalResourceDisableTransactionJob.xml',
                jobName: 'nonTransactionalResourceDisableTransactionJob',
                jobParameter: "inputFile=${inputFileName} outputFile=${outputFileName}"
        )

        def expectDataSet = DBUnitUtil.createDataSetFormCSV("./files/expect/database/ch05/transaction/")

        when:
        def exitValue = launcher.syncJob(jobRequest)
        def actualCustomer = jobDB.getExcludedColumnsTable("customer_mst", ["create_date", "update_date"] as String[])

        then:
        exitValue == 255
        DBUnitUtil.assertEquals(expectDataSet.getTable("customer_mst_commit"), actualCustomer)
        outputFile.exists()
        outputFile.readLines() == expectFile.readLines()

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
        Files.deleteIfExists(outputFile.toPath())
    }

    // utils
    def createIntialDataForTestcase2_1(DBUnitUtil adminDB2) {
        // Create branch_mst in adminDB.
        adminDB2.executeSqlScript(adminDB.conf.ch05.customedCreateSqlFilePaths as String[])
        // Set initial Data
        adminDB2.deleteAll(["branch_mst"] as String[])
        adminDB2.insert(DBUnitUtil.createDataSet({
            branch_mst {
                branch_id | branch_name    | branch_address | branch_tel | create_date
                "0000"    | "headquarters" | "Tokyo"        | "03"       | "[now]"
                "0001"    | "Osaka"        | "Osaka"        | "06"       | "[now]"
                "0002"    | "Nagoya"       | "Aichi"        | "052"      | "[now]"
            }
        }))
        jobDB.deleteAll(["customer_mst", "branch_mst"] as String[])
        jobDB.insert(DBUnitUtil.createDataSet({
            customer_mst {
                customer_id | customer_name | customer_address     | customer_tel | charge_branch_id | create_date
                "C0001"     | "CUSTOMER-01" | "CUSTOMER-ADDRESS01" | "000001"     | "0000"           | "[now]"
                "C0002"     | "CUSTOMER-02" | "CUSTOMER-ADDRESS02" | "000002"     | "0002"           | "[now]"
                "C0003"     | "CUSTOMER-03" | "CUSTOMER-ADDRESS03" | "000003"     | "0000"           | "[now]"
                "C0004"     | "CUSTOMER-04" | "CUSTOMER-ADDRESS04" | "000004"     | "0001"           | "[now]"
                "C0005"     | "CUSTOMER-05" | "CUSTOMER-ADDRESS05" | "000005"     | "0002"           | "[now]"
            }
        }))

    }
}
