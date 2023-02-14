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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06

import groovy.util.logging.Slf4j
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.inputvalidation.module.ValidateAndAbortByTryCatchItemProcessor
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.inputvalidation.module.ValidateAndAbortItemProcessor
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.inputvalidation.module.ValidateAndBulkMessageItemProcessor
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.inputvalidation.module.ValidateAndBulkMessageTasklet
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.inputvalidation.module.ValidateAndContinueItemProcessor
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.inputvalidation.module.ValidateAndMessageItemProcessor
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.DBUnitUtil
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.JobLauncher
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.JobRequest
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.LogCondition
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.MongoUtil
import spock.lang.Narrative
import spock.lang.Shared
import spock.lang.Specification

/**
 * Function test of input validation.
 *
 * @since 2.0.1
 *
 */
@Slf4j
@Narrative("""
A list of test cases is shown below.
1. Test input validation.
1.1 Processing is aborted if an error occurs.
1.2 Processing continues even if an error occurs.
1.3 Processing continues and message output even if an error occurs.
1.4 Processing continues and message output at the end even if an error occurs.
1.5 Processing continues and message output at the end even if an error occurs.
""")
class InputValidationSpec extends Specification {

    @Shared
            jobLauncher = new JobLauncher()

    @Shared
            adminDBUnitUtil = new DBUnitUtil("admin")

    @Shared
            jobDBUnitUtil = new DBUnitUtil()

    @Shared
            mongoUtil = new MongoUtil()

    def setup() {
        log.debug("### Spec case of [{}]", this.specificationContext.currentIteration.displayName)
        adminDBUnitUtil.dropAndCreateTable()
        jobDBUnitUtil.dropAndCreateTable()
        mongoUtil.deleteAll()
    }

    def cleanupSpec() {
        adminDBUnitUtil.close()
        jobDBUnitUtil.close()
        mongoUtil.close()
    }


    // 1.1.1
    def "Perform input check. If an error occurs, throw an input check exception and abort the process."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/inputvalidation/jobInputValidationAbort.xml',
                jobName: 'jobInputValidationAbort',
                jobParameter: "inputFile=files/test/input/ch06/inputvalidation/sales_plan_detail_01.csv"))

        then:
        exitCode == 255

        def expectJobExecutionDataSet = DBUnitUtil.createDataSet {
            batch_job_execution {
                job_execution_id | exit_code
                1 | "FAILED"
            }
        }
        def actualTable1 = adminDBUnitUtil.getIncludedColumnsTable("batch_job_execution",
                expectJobExecutionDataSet.getTableMetaData("batch_job_execution").getColumns())
        def expectITable1 = expectJobExecutionDataSet.getTable("batch_job_execution")
        DBUnitUtil.assertEquals(expectITable1, actualTable1)

        mongoUtil.find(
                new LogCondition(
                        logger: ValidateAndAbortItemProcessor.class.name,
                        level: 'ERROR',
                        message: ~/Exception occurred in input validation at the 5 th item/
                )).size() == 1

        def expectSalesPlanDetailDataSet = DBUnitUtil.createDataSet {
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "000001" | 3016 | 1 | "0000000001" | 100000000
                "000002" | 3017 | 2 | "0000000002" | 200000000
                "000003" | 3018 | 3 | "0000000003" | 300000000
            }
        }
        def actualTable2 = jobDBUnitUtil.getIncludedColumnsTable("sales_plan_detail",
                expectSalesPlanDetailDataSet.getTableMetaData("sales_plan_detail").getColumns())
        def expectITable2 = expectSalesPlanDetailDataSet.getTable("sales_plan_detail")
        DBUnitUtil.assertEquals(expectITable2, actualTable2)
    }
    // 1.1.2
    def "Perform input check using try catch. If an error occurs, throw an input check exception and abort the process."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/inputvalidation/jobInputValidationAbortByTryCatch.xml',
                jobName: 'jobInputValidationAbortByTryCatch',
                jobParameter: "inputFile=files/test/input/ch06/inputvalidation/sales_plan_detail_01.csv"))

        then:
        exitCode == 255

        def expectJobExecutionDataSet = DBUnitUtil.createDataSet {
            batch_job_execution {
                job_execution_id | exit_code
                1 | "FAILED"
            }
        }
        def actualTable1 = adminDBUnitUtil.getIncludedColumnsTable("batch_job_execution",
                expectJobExecutionDataSet.getTableMetaData("batch_job_execution").getColumns())
        def expectITable1 = expectJobExecutionDataSet.getTable("batch_job_execution")
        DBUnitUtil.assertEquals(expectITable1, actualTable1)

        mongoUtil.find(
                new LogCondition(
                        logger: ValidateAndAbortByTryCatchItemProcessor.class.name,
                        level: 'ERROR',
                        message: ~/Exception occurred in input validation at the 5 th item/
                )).size() == 1

        def expectSalesPlanDetailDataSet = DBUnitUtil.createDataSet {
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "000001" | 3016 | 1 | "0000000001" | 100000000
                "000002" | 3017 | 2 | "0000000002" | 200000000
                "000003" | 3018 | 3 | "0000000003" | 300000000
            }
        }
        def actualTable2 = jobDBUnitUtil.getIncludedColumnsTable("sales_plan_detail",
                expectSalesPlanDetailDataSet.getTableMetaData("sales_plan_detail").getColumns())
        def expectITable2 = expectSalesPlanDetailDataSet.getTable("sales_plan_detail")
        DBUnitUtil.assertEquals(expectITable2, actualTable2)
    }

    //1.2
    def "Perform input check. If an error occurs, log output and continue processing."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/inputvalidation/jobInputValidationContinue.xml',
                jobName: 'jobInputValidationContinue',
                jobParameter: "inputFile=files/test/input/ch06/inputvalidation/sales_plan_detail_01.csv"))

        then:
        exitCode == 0

        def expectJobExecutionDataSet = DBUnitUtil.createDataSet {
            batch_job_execution {
                job_execution_id | exit_code
                1 | "COMPLETED"
            }
        }
        def actualTable1 = adminDBUnitUtil.getIncludedColumnsTable("batch_job_execution",
                expectJobExecutionDataSet.getTableMetaData("batch_job_execution").getColumns())
        def expectITable1 = expectJobExecutionDataSet.getTable("batch_job_execution")
        DBUnitUtil.assertEquals(expectITable1, actualTable1)

        def expectStepExecutionDataSet = DBUnitUtil.createDataSet {
            batch_step_execution {
                step_execution_id | job_execution_id | commit_count | read_count | filter_count | write_count | exit_code
                "1" | 1 | 4 | 9 | 3 | 6 | "COMPLETED"
            }
        }
        def actualTable2 = adminDBUnitUtil.getIncludedColumnsTable("batch_step_execution",
                expectStepExecutionDataSet.getTableMetaData("batch_step_execution").getColumns())
        def expectITable2 = expectStepExecutionDataSet.getTable("batch_step_execution")
        DBUnitUtil.assertEquals(expectITable2, actualTable2)

        def cursorFind = mongoUtil.find(
                new LogCondition(
                        logger: ValidateAndContinueItemProcessor.class.name,
                        level: 'WARN'
                ))
        cursorFind.size() == 3

        cursorFind.any{ it.message.contains("Skipping item because exception occurred in input validation at the 5 th item.") }
        cursorFind.any{ it.message.contains("Skipping item because exception occurred in input validation at the 6 th item.") }
        cursorFind.any{ it.message.contains("Skipping item because exception occurred in input validation at the 7 th item.") }

        def expectSalesPlanDetailDataSet = DBUnitUtil.createDataSet {
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "000001" | 3016 | 1 | "0000000001" | 100000000
                "000002" | 3017 | 2 | "0000000002" | 200000000
                "000003" | 3018 | 3 | "0000000003" | 300000000
                "000004" | 3019 | 4 | "0000000004" | 400000000
                "000008" | 3023 | 8 | "0000000008" | 800000000
                "000009" | 3024 | 9 | "0000000009" | 900000000
            }
        }
        def actualTable3 = jobDBUnitUtil.getIncludedColumnsTable("sales_plan_detail",
                expectSalesPlanDetailDataSet.getTableMetaData("sales_plan_detail").getColumns())
        def expectITable3 = expectSalesPlanDetailDataSet.getTable("sales_plan_detail")
        DBUnitUtil.assertEquals(expectITable3, actualTable3)
    }

    // 1.3
    def "Perfrom input check. If an error occurs, message and log output and continue processing."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/inputvalidation/jobInputValidationMessage.xml',
                jobName: 'jobInputValidationMessage',
                jobParameter: "inputFile=files/test/input/ch06/inputvalidation/sales_plan_detail_01.csv"))

        then:
        exitCode == 0

        def expectJobExecutionDataSet = DBUnitUtil.createDataSet {
            batch_job_execution {
                job_execution_id | exit_code
                1 | "COMPLETED"
            }
        }
        def actualTable1 = adminDBUnitUtil.getIncludedColumnsTable("batch_job_execution",
                expectJobExecutionDataSet.getTableMetaData("batch_job_execution").getColumns())
        def expectITable1 = expectJobExecutionDataSet.getTable("batch_job_execution")
        DBUnitUtil.assertEquals(expectITable1, actualTable1)

        def expectStepExecutionDataSet = DBUnitUtil.createDataSet {
            batch_step_execution {
                step_execution_id | job_execution_id | commit_count | read_count | filter_count | write_count | exit_code
                "1" | 1 | 4 | 9 | 3 | 6 | "COMPLETED"
            }
        }
        def actualTable2 = adminDBUnitUtil.getIncludedColumnsTable("batch_step_execution",
                expectStepExecutionDataSet.getTableMetaData("batch_step_execution").getColumns())
        def expectITable2 = expectStepExecutionDataSet.getTable("batch_step_execution")
        DBUnitUtil.assertEquals(expectITable2, actualTable2)

        def cursorFind = mongoUtil.find(
                new LogCondition(
                        logger: ValidateAndMessageItemProcessor.class.name,
                        level: 'WARN'
                ))
        cursorFind.size() == 3

        cursorFind.any{ it.message.contains("branchId must be between 1 and 6 characters.Skipping item because exception occurred in input validation at the 5 th item.") }
        cursorFind.any{ it.message.contains("For year, enter a value less than or equal to 9,999.Skipping item because exception occurred in input validation at the 6 th item.") }
        cursorFind.any{ it.message.contains("For month, enter a value less than or equal to 12.Skipping item because exception occurred in input validation at the 7 th item.") }

        def expectSalesPlanDetailDataSet = DBUnitUtil.createDataSet {
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "000001" | 3016 | 1 | "0000000001" | 100000000
                "000002" | 3017 | 2 | "0000000002" | 200000000
                "000003" | 3018 | 3 | "0000000003" | 300000000
                "000004" | 3019 | 4 | "0000000004" | 400000000
                "000008" | 3023 | 8 | "0000000008" | 800000000
                "000009" | 3024 | 9 | "0000000009" | 900000000
            }
        }
        def actualTable3 = jobDBUnitUtil.getIncludedColumnsTable("sales_plan_detail",
                expectSalesPlanDetailDataSet.getTableMetaData("sales_plan_detail").getColumns())
        def expectITable3 = expectSalesPlanDetailDataSet.getTable("sales_plan_detail")
        DBUnitUtil.assertEquals(expectITable3, actualTable3)
    }

    // 1.4
    def "Perfrom input check. If an error occurs, continue processing and message and log are output at the end"() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/inputvalidation/jobInputValidationBulkMessage.xml',
                jobName: 'jobInputValidationBulkMessage',
                jobParameter: "inputFile=files/test/input/ch06/inputvalidation/sales_plan_detail_02.csv"))

        then:
        exitCode == 0

        def expectJobExecutionDataSet = DBUnitUtil.createDataSet {
            batch_job_execution {
                job_execution_id | exit_code
                1 | "COMPLETED"
            }
        }
        def actualTable1 = adminDBUnitUtil.getIncludedColumnsTable("batch_job_execution",
                expectJobExecutionDataSet.getTableMetaData("batch_job_execution").getColumns())
        def expectITable1 = expectJobExecutionDataSet.getTable("batch_job_execution")
        DBUnitUtil.assertEquals(expectITable1, actualTable1)

        def expectStepExecutionDataSet = DBUnitUtil.createDataSet {
            batch_step_execution {
                step_execution_id | job_execution_id | commit_count | read_count | filter_count | write_count | exit_code
                "1" | 1 | 4 | 9 | 3 | 6 | "COMPLETED"
            }
        }
        def actualTable2 = adminDBUnitUtil.getIncludedColumnsTable("batch_step_execution",
                expectStepExecutionDataSet.getTableMetaData("batch_step_execution").getColumns())
        def expectITable2 = expectStepExecutionDataSet.getTable("batch_step_execution")
        DBUnitUtil.assertEquals(expectITable2, actualTable2)

        def cursorFind = mongoUtil.find(
                new LogCondition(
                        logger: ValidateAndBulkMessageItemProcessor.class.name,
                        level: 'WARN'
                ))

        cursorFind.size() == 4

        def list = cursorFind.message.asList()

        def index = -1
        for (str in list) {
            index++
            if (str.contains("Skipping item because exception occurred in input validation")) break
        }

        def errorMessageList = [list[index], list[index + 1], list[index + 2], list[index + 3]]
        errorMessageList.any { it.contains("branchId must be between 1 and 6 characters. Skipping item because exception occurred in input validation at the 5 th item.") }
        errorMessageList.any { it.contains("For month, enter a value less than or equal to 12. Skipping item because exception occurred in input validation at the 5 th item.") }
        errorMessageList.any { it.contains("For year, enter a value less than or equal to 9,999. Skipping item because exception occurred in input validation at the 6 th item.") }
        errorMessageList.any { it.contains("For month, enter a value less than or equal to 12. Skipping item because exception occurred in input validation at the 7 th item.") }

        def expectSalesPlanDetailDataSet = DBUnitUtil.createDataSet {
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "000001" | 3016 | 1 | "0000000001" | 100000000
                "000002" | 3017 | 2 | "0000000002" | 200000000
                "000003" | 3018 | 3 | "0000000003" | 300000000
                "000004" | 3019 | 4 | "0000000004" | 400000000
                "000008" | 3023 | 8 | "0000000008" | 800000000
                "000009" | 3024 | 9 | "0000000009" | 900000000
            }
        }
        def actualTable3 = jobDBUnitUtil.getIncludedColumnsTable("sales_plan_detail",
                expectSalesPlanDetailDataSet.getTableMetaData("sales_plan_detail").getColumns())
        def expectITable3 = expectSalesPlanDetailDataSet.getTable("sales_plan_detail")
        DBUnitUtil.assertEquals(expectITable3, actualTable3)
    }

    // 1.5
    def "Perfrom input check Tasklet. If an error occurs, continue processing and message and log are output at the end"() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/inputvalidation/jobInputValidationBulkMessageTask.xml',
                jobName: 'jobInputValidationBulkMessageTask',
                jobParameter: "inputFile=files/test/input/ch06/inputvalidation/sales_plan_detail_02.csv"))

        then:
        exitCode == 0

        def expectJobExecutionDataSet = DBUnitUtil.createDataSet {
            batch_job_execution {
                job_execution_id | exit_code
                1 | "COMPLETED"
            }
        }
        def actualTable1 = adminDBUnitUtil.getIncludedColumnsTable("batch_job_execution",
                expectJobExecutionDataSet.getTableMetaData("batch_job_execution").getColumns())
        def expectITable1 = expectJobExecutionDataSet.getTable("batch_job_execution")
        DBUnitUtil.assertEquals(expectITable1, actualTable1)

        def expectStepExecutionDataSet = DBUnitUtil.createDataSet {
            batch_step_execution {
                step_execution_id | job_execution_id | commit_count | read_count | write_count | exit_code
                "1" | 1 | 1 | 0 | 0 | "COMPLETED"
            }
        }
        def actualTable2 = adminDBUnitUtil.getIncludedColumnsTable("batch_step_execution",
                expectStepExecutionDataSet.getTableMetaData("batch_step_execution").getColumns())
        def expectITable2 = expectStepExecutionDataSet.getTable("batch_step_execution")
        DBUnitUtil.assertEquals(expectITable2, actualTable2)

        def cursorFind = mongoUtil.find(
                new LogCondition(
                        logger: ValidateAndBulkMessageTasklet.class.name,
                        level: 'WARN'
                ))

        cursorFind.size() == 4

        def list = cursorFind.message.asList()

        def index = -1
        for (str in list) {
            index++
            if (str.contains("Skipping item because exception occurred in input validation")) break
        }

        def errorMessageList = [list[index], list[index + 1], list[index + 2], list[index + 3]]
        errorMessageList.any { it.contains("branchId must be between 1 and 6 characters. Skipping item because exception occurred in input validation at the 5 th item.") }
        errorMessageList.any { it.contains("For month, enter a value less than or equal to 12. Skipping item because exception occurred in input validation at the 5 th item.") }
        errorMessageList.any { it.contains("For year, enter a value less than or equal to 9,999. Skipping item because exception occurred in input validation at the 6 th item.") }
        errorMessageList.any { it.contains("For month, enter a value less than or equal to 12. Skipping item because exception occurred in input validation at the 7 th item.") }

        def expectSalesPlanDetailDataSet = DBUnitUtil.createDataSet {
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "000001" | 3016 | 1 | "0000000001" | 100000000
                "000002" | 3017 | 2 | "0000000002" | 200000000
                "000003" | 3018 | 3 | "0000000003" | 300000000
                "000004" | 3019 | 4 | "0000000004" | 400000000
                "000008" | 3023 | 8 | "0000000008" | 800000000
                "000009" | 3024 | 9 | "0000000009" | 900000000
            }
        }
        def actualTable3 = jobDBUnitUtil.getIncludedColumnsTable("sales_plan_detail",
                expectSalesPlanDetailDataSet.getTableMetaData("sales_plan_detail").getColumns())
        def expectITable3 = expectSalesPlanDetailDataSet.getTable("sales_plan_detail")
        DBUnitUtil.assertEquals(expectITable3, actualTable3)
    }

}
