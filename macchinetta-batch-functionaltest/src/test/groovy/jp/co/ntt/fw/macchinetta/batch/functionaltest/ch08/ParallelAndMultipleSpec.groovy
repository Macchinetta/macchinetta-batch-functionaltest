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
import org.junit.Rule
import org.junit.rules.TestName
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
 * Function test of parallel and multiple.
 *
 * @since 2.0.1
 */
@Slf4j
@Narrative("""
1. Testing parallelism
2. Testing multiple processing
3. Test control break
""")
class ParallelAndMultipleSpec extends Specification {

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
        adminDB.dropAndCreateTable() // To make verification easier to rebuild every time.
        mongoUtil.deleteAll()
    }

    def cleanupSpec() {
        mongoUtil.close()
        adminDB.close()
        jobDB.close()
    }

    // Testcase 1, test no.1
    def "Execute the parallel processing from the beginning Job is executed"() {
        setup:
        jobDB.deleteAll(["sales_plan_summary", "sales_plan_detail", "sales_performance_summary", "sales_performance_detail"] as String[])
        jobDB.insert(DBUnitUtil.createDataSet({
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "0001"    | 2016 | 12    | "C0001"     | 1000
                "0001"    | 2016 | 12    | "C0002"     | 2000
                "0001"    | 2016 | 12    | "C0003"     | 3000
                "0001"    | 2016 | 12    | "C0004"     | 4000
                "0001"    | 2016 | 12    | "C0005"     | 5000
                "0002"    | 2016 | 12    | "C0011"     | 1000
                "0002"    | 2016 | 12    | "C0012"     | 1000
                "0002"    | 2016 | 12    | "C0013"     | 1000
                "0002"    | 2016 | 12    | "C0014"     | 1000
                "0002"    | 2016 | 12    | "C0015"     | 1000
            }
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                "0001"    | 2016 | 12    | "C0001"     | 1000
                "0001"    | 2016 | 12    | "C0002"     | 1000
                "0001"    | 2016 | 12    | "C0003"     | 1000
                "0001"    | 2016 | 12    | "C0004"     | 1000
                "0001"    | 2016 | 12    | "C0005"     | 1000
                "0002"    | 2016 | 12    | "C0011"     | 1000
                "0002"    | 2016 | 12    | "C0012"     | 1100
                "0002"    | 2016 | 12    | "C0013"     | 1200
                "0002"    | 2016 | 12    | "C0014"     | 1300
                "0002"    | 2016 | 12    | "C0015"     | 1400
            }
        }))

        def expectData = DBUnitUtil.createDataSet({
            sales_plan_summary {
                branch_id | year | month | amount
                "0001"    | 2016 | 12    | 15000
                "0002"    | 2016 | 12    | 5000
            }
            sales_performance_summary {
                branch_id | year | month | amount
                "0001"    | 2016 | 12    | 5000
                "0002"    | 2016 | 12    | 6000
            }
        })

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch08/parallelandmultiple/parallelSummarizeJob.xml',
                jobName: 'parallelSummarizeJob'
        ))

        then:
        exitValue == 0
        
        def actualPlanData = jobDB.getTable("sales_plan_summary")
        def actualPerformanceData = jobDB.getTable("sales_performance_summary")
        DBUnitUtil.assertEquals(expectData.getTable("sales_plan_summary"), actualPlanData)
        DBUnitUtil.assertEquals(expectData.getTable("sales_performance_summary"), actualPerformanceData)

        // Since it is processed in parallel, the start time of each step should have been executed before the end time of each step.
        def stepStartLog = mongoUtil.find(new LogCondition(message: ~/step started./)).sort{l,r -> l.message <=> r.message}
        def stepStoptLog = mongoUtil.find(new LogCondition(message: ~/step stopped./)).sort{l,r -> l.message <=> r.message}
        stepStartLog.size() == 2
        stepStoptLog.size() == 2
        stepStartLog.get(0).timestamp < stepStoptLog.get(1).timestamp
        stepStartLog.get(1).timestamp < stepStoptLog.get(0).timestamp
        stepStartLog.every {it.thread.contains("parallelTaskExecutor")}
        stepStoptLog.every {it.thread.contains("parallelTaskExecutor")}

        def stepExecution = adminDB.getTable("batch_step_execution")
        stepExecution.rowCount == 2
        stepExecution.getValue(0, "start_time") < stepExecution.getValue(1, "end_time")
        stepExecution.getValue(1, "start_time") < stepExecution.getValue(0, "end_time")
        stepExecution.getValue(0, "exit_code") == 'COMPLETED'
        stepExecution.getValue(1, "exit_code") == 'COMPLETED'
        def jobExecution = adminDB.getTable("batch_job_execution")
        jobExecution.getValue(0, "exit_code") == 'COMPLETED'
    }

    // Testcase 1, test no.2
    def "Execute the parallel processing from the beginning Job is executed (One step generates an error)"() {
        setup:
        jobDB.deleteAll(["sales_plan_summary", "sales_plan_detail", "sales_performance_summary", "sales_performance_detail"] as String[])
        jobDB.insert(DBUnitUtil.createDataSet({
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "0001"    | 2016 | 12    | "C0001"     | 1000
                "0001"    | 2016 | 12    | "C0002"     | 2000
                "0001"    | 2016 | 12    | "C0003"     | 3000
                "0001"    | 2016 | 12    | "C0004"     | 4000
                "0001"    | 2016 | 12    | "C0005"     | 5000
                "0002"    | 2016 | 12    | "C0011"     | 1000
                "0002"    | 2016 | 12    | "C0012"     | 1000
                "0002"    | 2016 | 12    | "C0013"     | 1000
                "0002"    | 2016 | 12    | "C0014"     | 1000
                "0002"    | 2016 | 12    | "C0015"     | 1000
            }
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                "0001"    | 2016 | 12    | "C0001"     | 1000
                "0001"    | 2016 | 12    | "C0002"     | 1000
                "0001"    | 2016 | 12    | "C0003"     | 1000
                "0001"    | 2016 | 12    | "C0004"     | 1000
                "0001"    | 2016 | 12    | "C0005"     | 1000
                "0002"    | 2016 | 12    | "C0011"     | 1000
                "0002"    | 2016 | 12    | "C0012"     | 1100
                "0002"    | 2016 | 12    | "C0013"     | 1200
                "0002"    | 2016 | 12    | "C0014"     | 1300
                "0002"    | 2016 | 12    | "C0015"     | 1400
            }
            sales_performance_summary {
                branch_id | year | month | amount
                "0002"    | 2016 | 12    | 5000
            }
        }))

        def expectData = DBUnitUtil.createDataSet({
            sales_plan_summary {
                branch_id | year | month | amount
                "0001"    | 2016 | 12    | 15000
                "0002"    | 2016 | 12    | 5000
            }
            sales_performance_summary {
                branch_id | year | month | amount
                "0001"    | 2016 | 12    | 5000
                "0002"    | 2016 | 12    | 5000
            }
        })

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch08/parallelandmultiple/parallelSummarizeJob.xml',
                jobName: 'parallelSummarizeJob'
        ))

        then:
        exitValue == 255

        def actualPlanData = jobDB.getTable("sales_plan_summary")
        def actualPerformanceData = jobDB.getTable("sales_performance_summary")
        DBUnitUtil.assertEquals(expectData.getTable("sales_plan_summary"), actualPlanData)
        DBUnitUtil.assertEquals(expectData.getTable("sales_performance_summary"), actualPerformanceData)

        // Since it is processed in parallel, the start time of each step should have been executed before the end time of each step.
        def stepStartLog = mongoUtil.find(new LogCondition(message: ~/step started./)).sort{l,r -> l.message <=> r.message}
        def stepStoptLog = mongoUtil.find(new LogCondition(message: ~/step stopped./)).sort{l,r -> l.message <=> r.message}
        stepStartLog.size() == 2
        stepStoptLog.size() == 2
        stepStartLog.get(0).timestamp < stepStoptLog.get(1).timestamp
        stepStartLog.get(1).timestamp < stepStoptLog.get(0).timestamp
        stepStartLog.every {it.thread.contains("parallelTaskExecutor")}
        stepStoptLog.every {it.thread.contains("parallelTaskExecutor")}

        def stepExecution = adminDB.getTable("batch_step_execution")
        stepExecution.rowCount == 2
        stepExecution.getValue(0, "start_time") < stepExecution.getValue(1, "end_time")
        stepExecution.getValue(1, "start_time") < stepExecution.getValue(0, "end_time")


        def planIndex = (stepExecution.getValue(0, "step_name") == 'parallelSummarizeJob.step.plan') ? 0 :1
        def performanceIndex = planIndex == 0 ? 1 : 0
        stepExecution.getValue(planIndex, "exit_code") == 'COMPLETED'
        stepExecution.getValue(performanceIndex, "exit_code") == 'FAILED'
        def jobExecution = adminDB.getTable("batch_job_execution")
        jobExecution.getValue(0, "exit_code") == 'FAILED'
    }


    // Testcase 1, test no.3
    def "Execute a parallel processing job from the middle"() {
        setup:
        jobDB.deleteAll(["sales_plan_detail", "sales_performance_detail"] as String[])
        jobDB.insert(DBUnitUtil.createDataSet({
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "0001"    | 2016 | 12    | "C0001"     | 9999
                "0001"    | 2016 | 12    | "C0002"     | 9999
                "0001"    | 2016 | 12    | "C0003"     | 9999
                "0001"    | 2016 | 12    | "C0004"     | 9999
                "0001"    | 2016 | 12    | "C0005"     | 9999
                "0002"    | 2016 | 12    | "C0011"     | 9999
                "0002"    | 2016 | 12    | "C0012"     | 9999
                "0002"    | 2016 | 12    | "C0013"     | 9999
                "0002"    | 2016 | 12    | "C0014"     | 9999
                "0002"    | 2016 | 12    | "C0015"     | 9999
            }
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                "0001"    | 2016 | 12    | "C0001"     | 9999
                "0001"    | 2016 | 12    | "C0002"     | 9999
                "0001"    | 2016 | 12    | "C0003"     | 9999
                "0001"    | 2016 | 12    | "C0004"     | 1000
                "0001"    | 2016 | 12    | "C0005"     | 9999
                "0002"    | 2016 | 12    | "C0011"     | 9999
                "0002"    | 2016 | 12    | "C0012"     | 9999
                "0002"    | 2016 | 12    | "C0013"     | 9999
                "0002"    | 2016 | 12    | "C0014"     | 9999
                "0002"    | 2016 | 12    | "C0015"     | 9999
            }
        }))

        def expectData = DBUnitUtil.createDataSet({
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "0001"    | 2016 | 12    | "C0001"     | 1000
                "0001"    | 2016 | 12    | "C0002"     | 2000
                "0001"    | 2016 | 12    | "C0003"     | 3000
                "0001"    | 2016 | 12    | "C0004"     | 4000
                "0001"    | 2016 | 12    | "C0005"     | 5000
                "0002"    | 2016 | 12    | "C0011"     | 1000
                "0002"    | 2016 | 12    | "C0012"     | 1000
                "0002"    | 2016 | 12    | "C0013"     | 1000
                "0002"    | 2016 | 12    | "C0014"     | 1000
                "0002"    | 2016 | 12    | "C0015"     | 1000
            }
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                "0001"    | 2016 | 12    | "C0001"     | 1000
                "0001"    | 2016 | 12    | "C0002"     | 1000
                "0001"    | 2016 | 12    | "C0003"     | 1000
                "0001"    | 2016 | 12    | "C0004"     | 1000
                "0001"    | 2016 | 12    | "C0005"     | 1000
                "0002"    | 2016 | 12    | "C0011"     | 1000
                "0002"    | 2016 | 12    | "C0012"     | 1100
                "0002"    | 2016 | 12    | "C0013"     | 1200
                "0002"    | 2016 | 12    | "C0014"     | 1300
                "0002"    | 2016 | 12    | "C0015"     | 1400
            }
        })
        def planInputFile = './files/test/input/ch08/paralleandmultiple/planData.csv'
        def performanceInputFile = './files/test/input/ch08/paralleandmultiple/performanceData.csv'

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch08/parallelandmultiple/parallelRegisterJob.xml',
                jobName: 'parallelRegisterJob',
                jobParameter: "planInputFile=${planInputFile} performanceInputFile=${performanceInputFile}"
        ))

        then:
        exitValue == 0

        def actualPlanData = jobDB.getTable("sales_plan_detail")
        def actualPerformanceData = jobDB.getTable("sales_performance_detail")
        DBUnitUtil.assertEquals(expectData.getTable("sales_plan_detail"), actualPlanData)
        DBUnitUtil.assertEquals(expectData.getTable("sales_performance_detail"), actualPerformanceData)

        // Since it is processed in parallel, the start time of each step should have been executed before the end time of each step.
        def stepStartLog = mongoUtil.find(new LogCondition(message: ~/step started./)).sort{l,r -> l.message <=> r.message}
        def stepStoptLog = mongoUtil.find(new LogCondition(message: ~/step stopped./)).sort{l,r -> l.message <=> r.message}
        stepStartLog.size() == 3
        stepStoptLog.size() == 3
        def perprocessIndex = stepStartLog.findIndexOf {it.thread == "main"}
        perprocessIndex != -1
        stepStartLog.remove(perprocessIndex)
        def preSetpStopLog = stepStoptLog.remove(perprocessIndex)
        stepStartLog.get(0).timestamp < stepStoptLog.get(1).timestamp
        stepStartLog.get(1).timestamp < stepStoptLog.get(0).timestamp
        stepStartLog.every {it.thread.contains("parallelTaskExecutor")}
        stepStoptLog.every {it.thread.contains("parallelTaskExecutor")}

        preSetpStopLog.timestamp < stepStartLog.get(0).timestamp
        preSetpStopLog.timestamp < stepStartLog.get(1).timestamp

        def stepExecution = adminDB.getTable("batch_step_execution")
        stepExecution.rowCount == 3
        int preprocessRow = -1
        for (int i in 0..<stepExecution.rowCount) {
            if (stepExecution.getValue(i, "step_execution_id") == 1) {
                preprocessRow = i
                break
            }
        }
        preprocessRow != -1
        stepExecution.getValue(preprocessRow, "step_name") == 'parallelRegisterJob.step.preprocess'
        stepExecution.getValue(0, "exit_code") == 'COMPLETED'
        stepExecution.getValue(1, "exit_code") == 'COMPLETED'
        stepExecution.getValue(2, "exit_code") == 'COMPLETED'
        def parallelRows = [0,1,2] - [preprocessRow]
        stepExecution.getValue(parallelRows.get(0), "start_time") < stepExecution.getValue(parallelRows.get(1), "end_time")
        stepExecution.getValue(parallelRows.get(1), "start_time") < stepExecution.getValue(parallelRows.get(0), "end_time")
        def jobExecution = adminDB.getTable("batch_job_execution")
        jobExecution.getValue(0, "exit_code") == 'COMPLETED'
    }

    // Testcase 2, test no.1-2
    @Unroll
    def "Execute a job that performs multiple processing with unknown number of partitions[#description]"() {
        setup:
        jobDB.deleteAll(["sales_performance_detail", "invoice", "branch_mst", "customer_mst"] as String[])
        jobDB.insert(DBUnitUtil.createDataSet({
            branch_mst {
                branch_id | branch_name | branch_address | branch_tel | create_date
                "0001"    | "Tokyo"     | "DUMMY"        | "DUMMY"    | '[now]'
                "0002"    | "Osaka"     | "DUMMY"        | "DUMMY"    | '[now]'
                "0003"    | "Nagoya"    | "DUMMY"        | "DUMMY"    | '[now]'
            }
            customer_mst {
                customer_id | customer_name | customer_address | customer_tel | charge_branch_id | create_date
                "C0001"     | "CUSTOMER01"  | "DUMMY"          | "DUMMY"      | "0001"           | '[now]'
                "C0002"     | "CUSTOMER02"  | "DUMMY"          | "DUMMY"      | "0002"           | '[now]'
                "C0003"     | "CUSTOMER03"  | "DUMMY"          | "DUMMY"      | "0003"           | '[now]'
                "C0004"     | "CUSTOMER04"  | "DUMMY"          | "DUMMY"      | "0001"           | '[now]'
                "C0005"     | "CUSTOMER05"  | "DUMMY"          | "DUMMY"      | "0002"           | '[now]'
                "C0006"     | "CUSTOMER06"  | "DUMMY"          | "DUMMY"      | "0003"           | '[now]'
            }
            invoice {
                invoice_no   | invoice_date | invoice_amount | customer_id
                "INVOICE101" | '2016-12-01' | 1100           | "C0001"
                "INVOICE102" | '2016-11-02' | 1200           | "C0001"
                "INVOICE103" | '2016-10-03' | 1300           | "C0001"
                "INVOICE201" | '2016-12-01' | 2100           | "C0002"
                "INVOICE202" | '2016-11-02' | 2200           | "C0002"
                "INVOICE203" | '2016-10-03' | 2300           | "C0002"
                "INVOICE301" | '2016-12-01' | 3100           | "C0003"
                "INVOICE302" | '2016-11-02' | 3200           | "C0003"
                "INVOICE303" | '2016-10-03' | 3300           | "C0003"
                "INVOICE401" | '2016-12-01' | 4100           | "C0004"
                "INVOICE402" | '2016-11-02' | 4200           | "C0004"
                "INVOICE403" | '2016-10-03' | 4300           | "C0004"
                "INVOICE501" | '2016-12-01' | 5100           | "C0005"
                "INVOICE502" | '2016-11-02' | 5200           | "C0005"
                "INVOICE503" | '2016-10-03' | 5300           | "C0005"
                "INVOICE601" | '2016-12-01' | 6100           | "C0006"
                "INVOICE602" | '2016-11-02' | 6200           | "C0006"
                "INVOICE603" | '2016-10-03' | 6300           | "C0006"
            }
        }))

        def expectData = DBUnitUtil.createDataSet({
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                "0001"    | 2016 | 10    | "C0001"     | 1300
                "0001"    | 2016 | 10    | "C0004"     | 4300
                "0001"    | 2016 | 11    | "C0001"     | 1200
                "0001"    | 2016 | 11    | "C0004"     | 4200
                "0001"    | 2016 | 12    | "C0001"     | 1100
                "0001"    | 2016 | 12    | "C0004"     | 4100
                "0002"    | 2016 | 10    | "C0002"     | 2300
                "0002"    | 2016 | 10    | "C0005"     | 5300
                "0002"    | 2016 | 11    | "C0002"     | 2200
                "0002"    | 2016 | 11    | "C0005"     | 5200
                "0002"    | 2016 | 12    | "C0002"     | 2100
                "0002"    | 2016 | 12    | "C0005"     | 5100
                "0003"    | 2016 | 10    | "C0003"     | 3300
                "0003"    | 2016 | 10    | "C0006"     | 6300
                "0003"    | 2016 | 11    | "C0003"     | 3200
                "0003"    | 2016 | 11    | "C0006"     | 6200
                "0003"    | 2016 | 12    | "C0003"     | 3100
                "0003"    | 2016 | 12    | "C0006"     | 6100
            }
        })

        expect:
        def exitValue = launcher.syncJob { JobLauncher.SyncJobArg arg ->
            arg.jobRequest = new JobRequest(
                    jobFilePath: 'META-INF/jobs/ch08/parallelandmultiple/multipleInvoiceSummarizeJob.xml',
                    jobName: 'multipleInvoiceSummarizeJob'
            )
            arg.env = ["grid.size=0", "thread.size=${threadSize}"] as String[]
            arg.timeout = 120 * 1000L
            arg.timeUnit = TimeUnit.MILLISECONDS
        }
        exitValue == 0

        DBUnitUtil.assertEquals(expectData.getTable("sales_performance_detail"), jobDB.getTable("sales_performance_detail"))

        def slaveStepLog = mongoUtil.find(new LogCondition(message: ~/step started./))
        slaveStepLog.size() == divisionNum
        slaveStepLog.any { it.thread == 'parallelTaskExecutor-1'} == thead1exists
        slaveStepLog.any { it.thread == 'parallelTaskExecutor-2'} == thead2exists
        slaveStepLog.any { it.thread == 'parallelTaskExecutor-3'} == thead3exists

        def stepExecution = adminDB.getTable("batch_step_execution")
        stepExecution.rowCount == divisionNum + 1
        stepExecution.getValue(0, "exit_code") == 'COMPLETED'
        stepExecution.getValue(1, "exit_code") == 'COMPLETED'
        stepExecution.getValue(2, "exit_code") == 'COMPLETED'
        stepExecution.getValue(3, "exit_code") == 'COMPLETED'
        def jobExecution = adminDB.getTable("batch_job_execution")
        jobExecution.getValue(0, "exit_code") == 'COMPLETED'

        where:
        description                           |divisionNum | threadSize || thead1exists | thead2exists | thead3exists
        "divisionNum is less than threadSize" | 3           | 2          || true         | true         | false
        "divisionNum is equal to threadSize"  | 3           | 3          || true         | true         | true
        "divisionNum is more than threadSize" | 3           | 4          || true         | true         | true
    }

    // Testcase 2, test no.3-4
    @Unroll
    def "Execute a job that performs multiple processing with a fixed number of partitions[#description]"() {
        setup:
        jobDB.deleteAll(["sales_plan_summary", "sales_performance_summary"] as String[])
        jobDB.insert(DBUnitUtil.createDataSet({
            sales_performance_summary {
                branch_id | year | month | amount
                "0001"    | 2016 | 12    | 1000
                "0002"    | 2016 | 12    | 1000
                "0003"    | 2016 | 12    | 1000
                "0004"    | 2016 | 12    | 1000
                "0005"    | 2016 | 12    | 1000
                "0006"    | 2016 | 12    | 1000
                "0007"    | 2016 | 12    | 1000
                "0008"    | 2016 | 12    | 1000
                "0009"    | 2016 | 12    | 1000
                "0010"    | 2016 | 12    | 1000
            }
        }))
        def expectData = DBUnitUtil.createDataSet({
            sales_plan_summary {
                branch_id | year | month | amount
                "0001"    | 2017 | 12    | 1500
                "0002"    | 2017 | 12    | 1500
                "0003"    | 2017 | 12    | 1500
                "0004"    | 2017 | 12    | 1500
                "0005"    | 2017 | 12    | 1500
                "0006"    | 2017 | 12    | 1500
                "0007"    | 2017 | 12    | 1500
                "0008"    | 2017 | 12    | 1500
                "0009"    | 2017 | 12    | 1500
                "0010"    | 2017 | 12    | 1500
            }
        })

        expect:
        def exitValue = launcher.syncJob { JobLauncher.SyncJobArg arg ->
            arg.jobRequest = new JobRequest(
                    jobFilePath: 'META-INF/jobs/ch08/parallelandmultiple/multipleCreateSalesPlanSummaryJob.xml',
                    jobName: 'multipleCreateSalesPlanSummaryJob',
                    jobParameter: 'year=2016 month=12'
            )
            arg.env = ["grid.size=${gridSize}", "thread.size=${threadSize}"] as String[]
            arg.timeout = 120 * 1000L
            arg.timeUnit = TimeUnit.MILLISECONDS
        }
        exitValue == 0

        DBUnitUtil.assertEquals(expectData.getTable("sales_plan_summary"), jobDB.getTable("sales_plan_summary"))

        def slaveStepLog = mongoUtil.find(new LogCondition(message: ~/step started./))
        slaveStepLog.size() == gridSize
        slaveStepLog.any { it.thread == 'parallelTaskExecutor-1'} == thead1exists
        slaveStepLog.any { it.thread == 'parallelTaskExecutor-2'} == thead2exists
        slaveStepLog.any { it.thread == 'parallelTaskExecutor-3'} == thead3exists

        def stepExecution = adminDB.getTable("batch_step_execution")
        stepExecution.rowCount == gridSize + 1
        stepExecution.getValue(0, "exit_code") == 'COMPLETED'
        stepExecution.getValue(1, "exit_code") == 'COMPLETED'
        stepExecution.getValue(2, "exit_code") == 'COMPLETED'
        stepExecution.getValue(3, "exit_code") == 'COMPLETED'
        def jobExecution = adminDB.getTable("batch_job_execution")
        jobExecution.getValue(0, "exit_code") == 'COMPLETED'

        where:
        description                        | gridSize | threadSize || thead1exists | thead2exists | thead3exists
        "gridSize is less than threadSize" | 3        | 2          || true         | true         | false
        "gridSize is equal to threadSize"  | 3        | 3          || true         | true         | true
        "gridSize is more than threadSize" | 3        | 4          || true         | true         | true

    }

    // Testcase 2, test no.5
    def "Execute a job that performs multiple processing (from DB to multiple files)"() {
        jobDB.deleteAll(["branch_mst", "sales_plan_summary", "sales_performance_summary"] as String[])
        jobDB.insert(DBUnitUtil.createDataSet({
            branch_mst {
                branch_id | branch_name | branch_address | branch_tel | create_date
                "0001"    | "Tokyo"     | "DUMMY"        | "DUMMY"    | '[now]'
                "0002"    | "Osaka"     | "DUMMY"        | "DUMMY"    | '[now]'
                "0003"    | "Nagoya"    | "DUMMY"        | "DUMMY"    | '[now]'
            }
            sales_plan_summary {
                branch_id | year | month | amount
                "0001"    | 2016 | 12    | 1000
                "0002"    | 2016 | 12    | 1000
                "0003"    | 2016 | 12    | 1000
            }
            sales_performance_summary {
                branch_id | year | month | amount
                "0001"    | 2016 | 12    | 1100
                "0002"    | 2016 | 12    | 750
                "0003"    | 2016 | 12    | 400
            }
        }))
        def outputDir = "./files/test/output/ch08/parallelandmultiple"
        def expectDir = "./files/expect/output/ch08/parallelandmultiple"
        def expectFileNames = ["0001", "0002", "0003"]
        expectFileNames.each() {it ->
            def file = new File(outputDir, "Evaluation_${it}.csv")
            Files.deleteIfExists(file.toPath())
        }

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch08/parallelandmultiple/jobEvaluationReport.xml',
                jobName: 'jobEvaluationReport',
                jobParameter: "outputPath=${outputDir} year=2016 month=12"
        ))

        then:
        exitValue == 0

        def slaveStepLog = mongoUtil.find(new LogCondition(message: ~/step started./))
        slaveStepLog.size() == 3
        slaveStepLog.any { it.thread == 'parallelTaskExecutor-1'}
        slaveStepLog.any { it.thread == 'parallelTaskExecutor-2'}
        slaveStepLog.any { it.thread == 'parallelTaskExecutor-3'}

        expectFileNames.every {it ->
            def file = new File(outputDir, "Evaluation_${it}.csv")
            file.exists()
        }
        expectFileNames.every { it ->
            def actualFile = new File(outputDir, "Evaluation_${it}.csv")
            def expectFile = new File(expectDir, "Evaluation_${it}.csv")
            actualFile.readLines() == expectFile.readLines()
        }

        cleanup:
        expectFileNames.each() {it ->
            def file = new File(outputDir, "Evaluation_${it}.csv")
            Files.deleteIfExists(file.toPath())
        }

    }
}
