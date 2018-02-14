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
import org.dbunit.dataset.ITable
import org.junit.Rule
import org.junit.rules.TestName
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.terasoluna.batch.async.db.AsyncBatchDaemon
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.DBUnitUtil
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.JobLauncher
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.JobRequest
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.LogCondition
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.MongoUtil
import spock.lang.IgnoreIf
import spock.lang.Narrative
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Function test of asynchronous execution (DB polling).
 *
 * @since 5.0.0
 */
@Slf4j
@Narrative("""
A list of test cases is shown below.
1. Testing the DB polling processing sequence
1-1. Daemon start test
1-2. Test of polling
1-3. Testing job execution
1-4. Daemon Stop Test
2. Testing the polling status transition
3. Test in case of abnormality in DB polling
4. Test recovery at job abnormal termination
5. Test the customization of the job request table
6. Test the customization of the clock
7. Testing multiple daemon startups
8. Testing Modularity
""")
class AsyncJobWithDBSpec extends Specification {

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
        def stopFile = launcher.stopFile()
        Files.deleteIfExists(stopFile.toPath())
    }

    def cleanupSpec() {
        mongoUtil.close()
        adminDB.close()
        jobDB.close()
    }

    // Testcase 1.1, test no.1
    def "Normal system test of DB polling processing sequence"() {
        setup:
        jobDB.deleteAll(["sales_performance_detail", "sales_performance_summary"] as String[])

        when:
        def env = ["async-batch-daemon.job-concurrency-num=5",
                   "async-batch-daemon.polling-interval=5000",
                   "async-batch-daemon.polling-initial-delay=10000"] as String[]
        def p = launcher.startAsyncBatchDaemon(env)
        def jobRequest = new JobRequest(
                jobName: 'jobSalesPerformance02',
                jobParameter: 'param1=non_parameter'
        )
        def jobSeqId = launcher.registerAsyncJob(adminDB, jobRequest)

        launcher.waitAsyncJob(adminDB, jobSeqId, 180 * 1000L)

        def noStopFileName = launcher.stopFile().canonicalFile.toString() + "-not"
        def noStopFile = new File(noStopFileName)
        noStopFile.createNewFile()


        sleep(5L * 1000L)
        def notFoundStopfileLog = mongoUtil.findOne(message: 'Async Batch Daemon has detected the polling stop file, and then shutdown now!')
        def daemonNotStopLog = mongoUtil.findOne(message: 'Async Batch Daemon stopped after all jobs completed.')

        sleep(5L * 1000L)

        launcher.stopAsyncBatchDaemon(p)

        mongoUtil.waitForOutputLog(new LogCondition(message: 'Async Batch Daemon stopped after all jobs completed.'))

        // daemon log
        def daemonLaunchLog = mongoUtil.findOne(message: 'Async Batch Daemon start.')
        def schedulerStartLog = mongoUtil.findOne(new LogCondition(message: "Initializing ExecutorService  'daemonTaskScheduler'", logger: ThreadPoolTaskScheduler.class.name))
        def pollingLog = mongoUtil.find(new LogCondition(message: 'Polling processing.')).sort {l,r -> l.timestamp <=> r.timestamp}
        def findStopFileLog = mongoUtil.findOne(message: 'Async Batch Daemon has detected the polling stop file, and then shutdown now!')
        def daemonStopLog = mongoUtil.findOne(message: 'Async Batch Daemon stopped after all jobs completed.')
        def pollingStopLog = mongoUtil.findOne(message: 'JobRequestPollTask is called shutdown.')

        // job log
        def jobStartLog = mongoUtil.findOne(message: 'job started. [JobName:jobSalesPerformance02]')
        def jobFinishLog = mongoUtil.findOne(message: 'job finished.[JobName:jobSalesPerformance02][ExitStatus:COMPLETED]')

        // job request table
        def jobRequestTable = adminDB.getTable('batch_job_request')
        def jobExecutionId = (Integer) jobRequestTable.getValue(0, "job_execution_id")
        jobExecutionId = jobExecutionId == null ? -1 : jobExecutionId

        // job repository
        def jobParamSql = "SELECT * FROM batch_job_execution_params WHERE job_execution_id = " + jobExecutionId + " AND key_name = 'param1'"
        def jobParamTable = adminDB.createQueryTable("jobParam", jobParamSql)
        def jobExecutionSql = "SELECT * FROM batch_job_execution WHERE job_execution_id = " + jobExecutionId
        def jobExecutionTable = adminDB.createQueryTable("jobExecution", jobExecutionSql)


        then:
        daemonLaunchLog != null
        schedulerStartLog != null
        pollingLog.size() >= 2
        notFoundStopfileLog == null
        daemonNotStopLog == null
        findStopFileLog != null
        daemonStopLog != null
        pollingStopLog != null
        noStopFile.exists()

        jobStartLog != null
        jobFinishLog != null

        // datetime compare
        def cursorFirstPollingLog = pollingLog.get(0)
        def cursorSecondPollingLog = pollingLog.get(1)

        // Confirm Delay of Approximately 10 Seconds over.(initial-delay)
        def diffInitialInterval = (cursorFirstPollingLog.timestamp.time - schedulerStartLog.timestamp.time) / 1000
        diffInitialInterval > 10.0

        // Make sure the polling interval is approximately 5 seconds.
        def diffPollingInterval = (cursorSecondPollingLog.timestamp.time - cursorFirstPollingLog.timestamp.time) / 1000
        diffPollingInterval > 5.0

        // Assertion to the data from database.
        jobExecutionId != -1
        jobParamTable.getValue(0, "string_val") == "non_parameter"
        jobExecutionTable.getValue(0, "exit_code") == "COMPLETED"
        p.exitValue() == 0

        cleanup:
        if (noStopFile != null) {
            Files.deleteIfExists(noStopFile.toPath())
        }
    }

    // Testcase 1.2, test no.1
    def "The job request is acquired with the specified number of cases"() {
        setup:
        def jobSeqIds = []
        20.times {
            def jobRequest = new JobRequest(
                    jobName: 'asyncJobLongTermTask',
                    jobParameter: 'seq=' + it
            )
            jobSeqIds << launcher.registerAsyncJob(adminDB, jobRequest)
        }

        when:
        def env = ["async-batch-daemon.job-concurrency-num=5", "async-batch-daemon.polling-interval=5000"] as String[]
        def p = launcher.startAsyncBatchDaemon(env)

        def count = 0
        while (count < 30) {
            def pollingLog = mongoUtil.find(new LogCondition(message: 'Polling processing.'))
            if (pollingLog.size() > 1) {
                break
            }
            count++
            sleep(1000L)
        }
        def jobRequestSql = "SELECT * FROM batch_job_request WHERE polling_status = 'POLLED' ORDER BY job_seq_id ASC"
        def jobRequestTable = adminDB.createQueryTable("jobRequest", jobRequestSql)

        launcher.waitAsyncJob(adminDB, jobSeqIds.get(0) as long, 180L, TimeUnit.SECONDS)
        launcher.stopAsyncBatchDaemon(p)

        then:
        count < 30
        jobRequestTable.rowCount == 5
        for (int i in 0..<jobRequestTable.rowCount) {
            assert jobRequestTable.getValue(i, "job_seq_id") == jobSeqIds.get(i)
        }
        p.exitValue() == 0
    }

    // Testcase 1.3, test no.1
    def "Job does not start with contents set for job request"() {
        setup:
        def jobRequest = new JobRequest(
                jobName: 'jobNotDefinitionName'
        )
        def jobSeqId = launcher.registerAsyncJob(adminDB, jobRequest)

        when:
        def p = launcher.startAsyncBatchDaemon()
        launcher.waitAsyncJob(adminDB, jobSeqId)
        launcher.stopAsyncBatchDaemon(p)

        mongoUtil.waitForOutputLog(new LogCondition(message: 'Async Batch Daemon stopped after all jobs completed.'))

        def jobRequestTable = adminDB.getTable("batch_job_request")
        def checkMessage = 'Job execution fail. [JobSeqId:' + jobSeqId + '][JobName:jobNotDefinitionName]'
        def errorLog = mongoUtil.findOne(new LogCondition(message: checkMessage))

        then:
        jobRequestTable.getValue(0, "polling_status") == 'EXECUTED'
        jobRequestTable.getValue(0, "job_execution_id") == null
        errorLog != null
        errorLog.throwable._class.simpleName == 'NoSuchJobException'
        p.exitValue() == 0
    }

    // Testcase 1.3, test no.2 and Testcase 2, test no. 1,3,4
    def "No more jobs will be started than concurrency"() {
        setup:
        def jobSeqIds = []
        def trasitionCheckIds = []
        20.times {
            def jobRequest = new JobRequest(
                    jobName: 'asyncJobLongTermTask'
            )
            def jobSeqId = launcher.registerAsyncJob(adminDB, jobRequest)
            jobSeqIds << jobSeqId
            if (it < 5) {
                trasitionCheckIds << jobSeqId
            }

        }
        def initJobRequestTable = adminDB.getTable("batch_job_request")

        when:
        def env = ["async-batch-daemon.job-concurrency-num=5", "async-batch-daemon.polling-interval=5000"] as String[]
        def p = launcher.startAsyncBatchDaemon(env)

        // first polled job are executing.
        def count = 0
        while (count < 30) {
            def pollingLog = mongoUtil.find(new LogCondition(message: 'Polling processing.'))
            if (pollingLog.size() > 1) {
                break
            }
            count++
            sleep(1000L)
        }
        assert count != 30
        def polledJobRequestTable = adminDB.getTable("batch_job_request")

        launcher.waitAsyncJob(adminDB, jobSeqIds.get(0) as long, 180L, TimeUnit.SECONDS)

        // second polled job are executing.
        def pollingCount = mongoUtil.find(new LogCondition(message: 'Polling processing.')).size()
        count = 0
        while (count < 30) {
            def pollingLog = mongoUtil.find(new LogCondition(message: 'Polling processing.'))
            if (pollingLog.size() > pollingCount + 1) {
                break
            }
            count++
            sleep(1000L)
        }
        assert count != 30
        def executedJobRequestTable = adminDB.getTable("batch_job_request")
        launcher.waitAsyncJob(adminDB, jobSeqIds.get(5) as long, 180L, TimeUnit.SECONDS)

        launcher.stopAsyncBatchDaemon(p)

        mongoUtil.waitForOutputLog(new LogCondition(message: 'Async Batch Daemon stopped after all jobs completed.'))

        def taskRejectionLog = mongoUtil.find(new LogCondition(message: ~/Concurrency number of executing job is over, and skip this and after requests./))

        then:
        countByPollingStatus(initJobRequestTable, 'INIT') == 20
        countByPollingStatus(polledJobRequestTable, 'POLLED') == 5
        countByPollingStatus(executedJobRequestTable, 'POLLED') == 5
        countByPollingStatus(polledJobRequestTable, 'INIT') == 15
        countByPollingStatus(executedJobRequestTable, 'INIT') == 10
        countByPollingStatus(polledJobRequestTable, 'EXECUTED') == 0
        countByPollingStatus(executedJobRequestTable, 'EXECUTED') == 5

        // Confirm transition of polling status
        findJobSeqIdByPollingStatus(polledJobRequestTable, 'POLLED') - trasitionCheckIds == []
        findJobSeqIdByPollingStatus(executedJobRequestTable, 'EXECUTED') - trasitionCheckIds == []
        taskRejectionLog.size() != 0
        p.exitValue() == 0
    }

    // Testcase 1.4, test no.1
    def "The asynchronous daemon is stopped after the running job ends"() {
        setup:
        def jobRequest = new JobRequest(
                jobName: 'asyncJobLongTermTask'
        )
        launcher.registerAsyncJob(adminDB, jobRequest)

        when:
        def p = launcher.startAsyncBatchDaemon()
        // wait polled job
        def count = 0
        while (count < 30) {
            def pollingLog = mongoUtil.find(new LogCondition(message: 'Polling processing.'))
            if (pollingLog.size() > 1) {
                break
            }
            count++
            sleep(500L)
        }
        then:
        count < 30

        when:
        launcher.stopAsyncBatchDaemon(p, 180, TimeUnit.SECONDS)

        mongoUtil.waitForOutputLog(new LogCondition(message: 'job finished.[JobName:asyncJobLongTermTask][ExitStatus:COMPLETED]'))
        mongoUtil.waitForOutputLog(new LogCondition(message: 'Async Batch Daemon stopped after all jobs completed.'))

        def daemonStopLog = mongoUtil.findOne(new LogCondition(message: 'Async Batch Daemon stopped after all jobs completed.'))
        def jobFinishLog = mongoUtil.findOne(new LogCondition(message: 'job finished.[JobName:asyncJobLongTermTask][ExitStatus:COMPLETED]'))
        def jobRequestTable = adminDB.getTable('batch_job_request')

        then:
        jobRequestTable.getValue(0, "polling_status") == 'EXECUTED'
        daemonStopLog.timestamp.after(jobFinishLog.timestamp)
        p.exitValue() == 0
    }

    // Testcase 1.4, test no.2
    def "Before the asynchronous batch daemon starts, if the stop file is created, the asynchronous batch daemon will stop immediately"() {
        setup:
        5.times {
            def jobRequest = new JobRequest(
                    jobName: 'asyncJobLongTermTask'
            )
            launcher.registerAsyncJob(adminDB, jobRequest)
        }
        def env = ["async-batch-daemon.polling-interval=5000", "async-batch-daemon.polling-initial-delay=5000"] as String[]
        def command = "java -cp target/dependency/* ${AsyncBatchDaemon.class.name}"

        def stopFile = launcher.stopFile()
        if (!stopFile.exists()) {
            stopFile.createNewFile()
        }

        when:
        def p = JobLauncher.executeProcess(command, env, null)

        def daemonStopLog = mongoUtil.waitForOutputLog(new LogCondition(message: 'Polling stop file already exists. Daemon stop. Please delete the file. [Path:' + stopFile.toPath() + ']'))
        def jobRequestTable = adminDB.getTable('batch_job_request')

        def exitValue = p.waitFor()

        then:
        for (int i in 0..<5) {
            jobRequestTable.getValue(i, "polling_status") == 'INIT'
        }
        daemonStopLog != null
        exitValue == 255

        cleanup:
        if (stopFile != null) {
            Files.deleteIfExists(stopFile.toPath())
        }
    }

    // Testcase 1.4, test no.3
    @IgnoreIf({ os.windows })
    def "The asynchronous daemon is normally stopped when send signal SIGINT"() {
        setup:
        def command = "sh scripts/ch04/asyncjobwithdb/send_sigint_async_batch_daemon.sh"

        when:
        def p = launcher.startAsyncBatchDaemon()
        // wait polled job
        def count = 0
        while (count < 30) {
            def pollingLog = mongoUtil.find(new LogCondition(message: 'Polling processing.'))
            if (pollingLog.size() > 1) {
                break
            }
            count++
            sleep(500L)
        }
        then:
        count < 30

        when:
        def signalProcess = launcher.executeProcess(command)
        signalProcess.waitForOrKill(TimeUnit.SECONDS.toMillis(10L))

        mongoUtil.waitForOutputLog(new LogCondition(message: 'Shutting down ExecutorService'))

        then:
        signalProcess.exitValue() == 0
        p.waitFor() == 130    // 128 + SIGINT
        mongoUtil.find(new LogCondition(
                message: 'JobRequestPollTask is called shutdown.',
                level: 'INFO'
        )).size() == 1
        mongoUtil.find(new LogCondition(
                message: 'Async Batch Daemon stopped after all jobs completed.',
                level: 'INFO'
        )).size() == 0
    }

    // Testcase 1.4, test no.4
    def "when stop file created as directory, the asynchronous batch daemon do not stop."() {
        setup:
        def jobRequest = new JobRequest(
                jobName: 'asyncJobLongTermTask'
        )
        launcher.registerAsyncJob(adminDB, jobRequest)

        def stopFile = launcher.stopFile()

        when:
        def p = launcher.startAsyncBatchDaemon()
        // wait polled job
        def count = 0
        while (count < 30) {
            def pollingLog = mongoUtil.find(new LogCondition(message: 'Polling processing.'))
            if (pollingLog.size() > 1) {
                break
            }
            count++
            sleep(500L)
        }
        then:
        count < 30

        when:
        def result = 0

        Files.createDirectory(stopFile.toPath())

        try {
            mongoUtil.waitForOutputLog(new LogCondition(message: "Polling stop file must be a regular file. [Path:${stopFile.toPath()}]"))
        } catch (TimeoutException e) {
            result = -1
        }
        stopFile.deleteDir()

        launcher.stopAsyncBatchDaemon(p, 180, TimeUnit.SECONDS)

        mongoUtil.waitForOutputLog(new LogCondition(message: 'Async Batch Daemon stopped after all jobs completed.'))

        then:
        result == 0

        cleanup:
        if (stopFile.exists()) {
            if (stopFile.isDirectory()) {
                stopFile.deleteDir()
            } else {
                stopFile.delete()
            }
        }
    }

    @Requires({ os.windows })
    // Testcase 3, test no.1
    def "When an error occurs in DB polling, the transaction is rolled back (Windows)"() {
        setup:
        jobDB.deleteAll(["invoice"] as String[])
        def jobRequest = new JobRequest(
                jobName: 'asyncJobSingleTranTask'
        )
        launcher.registerAsyncJob(adminDB, jobRequest)

        when:
        def p = launcher.startAsyncBatchDaemon()

        // Wait ten to process.
        mongoUtil.waitForOutputLog(100 * 1000L, new LogCondition(message: 'Invoice added. [invoice no:invoice-0010]'))

        // Kill process.
        p.waitForOrKill(100L)

        def invoiceTable = jobDB.getTable("invoice")
        def jobRequestTable = adminDB.getTable("batch_job_request")

        then:
        invoiceTable.rowCount == 0
        jobRequestTable.getValue(0, "polling_status") == 'POLLED'
        !p.alive
        p.exitValue() != 0
    }

    @Requires({ os.linux })
    // Testcase 3, test no.1
    def "When an error occurs in DB polling, the transaction is rolled back (Linux)"() {
        setup:
        jobDB.deleteAll(["invoice"] as String[])
        def jobRequest = new JobRequest(
                jobName: 'asyncJobSingleTranTask'
        )
        launcher.registerAsyncJob(adminDB, jobRequest)

        def command = "sh scripts/ch04/asyncjobwithdb/send_kill_sigint_async_batch_daemon.sh"

        when:
        def p = launcher.startAsyncBatchDaemon()

        // Wait ten to process.
        mongoUtil.waitForOutputLog(100 * 1000L, new LogCondition(message: 'Invoice added. [invoice no:invoice-0010]'))

        // Kill process.
        def signalProcess = launcher.executeProcess(command)
        signalProcess.waitForOrKill(TimeUnit.SECONDS.toMillis(10L))
        p.waitFor()

        def invoiceTable = jobDB.getTable("invoice")
        def jobRequestTable = adminDB.getTable("batch_job_request")

        then:
        signalProcess.exitValue() == 0
        !p.alive
        p.exitValue() != 0

        invoiceTable.rowCount == 0
        jobRequestTable.getValue(0, "polling_status") == 'POLLED'
    }

    // Testcase 3, test no.2
    def "If an error occurs in the DB polling, perform the next polling task without retrying"() {
        setup:
        jobDB.deleteAll(["sales_performance_detail", "sales_performance_summary"] as String[])
        def env = ["async-batch-daemon.polling-interval=10000"] as String[]

        when:
        def p = launcher.startAsyncBatchDaemon(env)

        // Wait till you confirm the first polling.
        mongoUtil.waitForOutputLog(new LogCondition(message: 'Polling processing.'))

        // Drop job request table.
        adminDB.executeSqlScript(adminDB.conf.ch04.defaultDropSqlsFilePaths as String[])

        // Wait until the third poll is confirmed in the faulty environment.
        def count = 0
        while (count < 30) {
            def pollingLog = mongoUtil.find(new LogCondition(message: 'Polling processing.'))
            if (pollingLog.size() > 3) {
                break
            }
            count++
            sleep(500L)
        }

        // Create job request table.
        adminDB.executeSqlScript(adminDB.conf.ch04.defaultCreateSqlFilePaths as String[])

        def jobRequest = new JobRequest(
                jobName: 'jobSalesPerformance02',
                jobParameter: 'param1=non_parameter'
        )
        def jobSeqId = launcher.registerAsyncJob(adminDB, jobRequest)

        launcher.waitAsyncJob(adminDB, jobSeqId)
        launcher.stopAsyncBatchDaemon(p)

        def jobStartLog = mongoUtil.findOne(message: 'job started. [JobName:jobSalesPerformance02]')
        def jobRequestTable = adminDB.getTable('batch_job_request')
        def errorLog = mongoUtil.find(new LogCondition(message: 'Unexpected error occurred in scheduled task.'))

        then:
        jobStartLog != null
        jobRequestTable.getValue(0, "polling_status") == 'EXECUTED'
        errorLog.size() > 0
        errorLog.get(0).throwable._class.name == "org.springframework.jdbc.BadSqlGrammarException"
    }

    // Testcase 4, test no.2
    def "Recovery at job abnormal termination (restart)"() {
        setup:
        // Copy input file form incorrect data file.
        def inputFile = new File("./files/test/input/ch04/asyncjobwithdb/sales_performance_case4-2.csv")
        def incorrectFile = new File("./files/test/input/ch04/asyncjobwithdb/sales_performance_incorrect.csv")
        Files.deleteIfExists(inputFile.toPath())
        inputFile << incorrectFile.readBytes()

        jobDB.deleteAll(["sales_performance_detail"] as String[])
        def jobRequest = new JobRequest(
                jobName: 'jobSalesPerformance01',
                jobParameter: 'inputFile=./files/test/input/ch04/asyncjobwithdb/sales_performance_case4-2.csv'
        )
        def jobSeqId = launcher.registerAsyncJob(adminDB, jobRequest)

        when:
        def p = launcher.startAsyncBatchDaemon()
        launcher.waitAsyncJob(adminDB, jobSeqId, 180 * 1000L)
        launcher.stopAsyncBatchDaemon(p)

        // Snapshot of state at job abnormal termination.
        def jobRequestTable = adminDB.getTable("batch_job_request")
        def jobExecutionId = jobRequestTable.getValue(0, "job_execution_id")
        def asyncJobExecutionTable = adminDB.createQueryTable("job_execution",
                "SELECT * FROM batch_job_execution WHERE job_execution_id = " + jobExecutionId)
        def jobInstanceId = asyncJobExecutionTable.getValue(0, "job_instance_id")
        def asyncSalesPerformanceTable = jobDB.getTable("sales_performance_detail")

        mongoUtil.waitForOutputLog(new LogCondition(message: 'Async Batch Daemon stopped after all jobs completed.'))

        // Copy input file form correct data file.
        def correctFile = new File("./files/test/input/ch04/asyncjobwithdb/sales_performance_correct.csv")
        Files.deleteIfExists(inputFile.toPath())
        inputFile << correctFile.readBytes()

        // Clear async job log.
        mongoUtil.deleteAll()

        // Restart job
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/common/jobSalesPerformance01.xml',
                jobName: jobExecutionId,
                jobParameter: '-restart'
        ))

        def syncSalesPerformanceTable = jobDB.getTable("sales_performance_detail")
        def syncJobExecutionTable = adminDB.createQueryTable("job_execution",
                "SELECT * FROM batch_job_execution WHERE job_execution_id != " + jobExecutionId + " AND job_instance_id = " + jobInstanceId)
        def nextJobExecutionId = syncJobExecutionTable.getValue(0, "job_execution_id")
        def syncStepExecutionTable = adminDB.createQueryTable("step_execution",
                "SELECT * FROM batch_step_execution WHERE job_execution_id = " + nextJobExecutionId)

        mongoUtil.waitForOutputLog(new LogCondition(message: 'job finished.[JobName:jobSalesPerformance01][ExitStatus:COMPLETED]'))

        def readLogs = mongoUtil.find(new LogCondition(message: ~/Read item: SalesPerformanceDetail/))

        then:
        jobRequestTable.getValue(0, "polling_status") == 'EXECUTED'
        asyncJobExecutionTable.rowCount == 1
        asyncJobExecutionTable.getValue(0, "exit_code") == 'FAILED'
        asyncSalesPerformanceTable.rowCount == 10
        exitValue == 0
        syncSalesPerformanceTable.rowCount == 30
        syncStepExecutionTable.getValue(0, "read_count") == 20
        syncStepExecutionTable.getValue(0, "write_count") == 20
        readLogs.size() == 20
        readLogs.get(0).message.indexOf("cust11") != -1

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
    }

    // Testcase 4, test no.3
    def "Recovery at job abnormal termination (stop)"() {
        setup:

        def random = new Random()

        jobDB.deleteAll(["sales_performance_detail", "sales_performance_summary"] as String[])
        jobDB.insert(DBUnitUtil.createDataSet({
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                "0001" | 2016 | 12 | "C0001" | random.nextInt(100000)
                "0006" | 2016 | 12 | "C0002" | random.nextInt(100000)
                "0001" | 2016 | 12 | "C0003" | random.nextInt(100000)
                "0002" | 2016 | 12 | "C0004" | random.nextInt(100000)
                "0003" | 2016 | 12 | "C0005" | random.nextInt(100000)
                "0004" | 2016 | 12 | "C0006" | random.nextInt(100000)
                "0001" | 2016 | 12 | "C0007" | random.nextInt(100000)
                "0007" | 2016 | 12 | "C0008" | random.nextInt(100000)
                "0002" | 2016 | 12 | "C0009" | random.nextInt(100000)
                "0002" | 2016 | 12 | "C0010" | random.nextInt(100000)
                "0001" | 2016 | 12 | "C0011" | random.nextInt(100000)
                "0001" | 2016 | 12 | "C0012" | random.nextInt(100000)
                "0003" | 2016 | 12 | "C0013" | random.nextInt(100000)
                "0003" | 2016 | 12 | "C0014" | random.nextInt(100000)
                "0001" | 2016 | 12 | "C0015" | random.nextInt(100000)
                "0001" | 2016 | 12 | "C0016" | random.nextInt(100000)
                "0004" | 2016 | 12 | "C0017" | random.nextInt(100000)
                "0004" | 2016 | 12 | "C0018" | random.nextInt(100000)
                "0001" | 2016 | 12 | "C0019" | random.nextInt(100000)
                "0005" | 2016 | 12 | "C0020" | random.nextInt(100000)
            }
        }))
        def jobRequest = new JobRequest(
                jobName: 'asyncJobEmulateLongProcessing'
        )
        def jobSeqId = launcher.registerAsyncJob(adminDB, jobRequest)

        def runningJobSql = "SELECT T1.job_execution_id AS job_execution_id " +
                "FROM batch_job_execution T1 " +
                "INNER JOIN batch_job_instance T2 ON T1.job_instance_id = T2.job_instance_id " +
                "WHERE T1.end_time IS NULL AND T1.status = 'STARTED' AND T2.job_name = 'asyncJobEmulateLongProcessing'"

        when:
        def p = launcher.startAsyncBatchDaemon()

        mongoUtil.waitForOutputLog(new LogCondition(message: 'job started. [JobName:asyncJobEmulateLongProcessing]'))

        def runningJobExecutionId = adminDB.createQueryTable("running_job", runningJobSql).getValue(0, "job_execution_id")

        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/asyncjobwithdb/asyncJobEmulateLongProcessing.xml',
                jobName: runningJobExecutionId,
                jobParameter: '-stop'
        ))

        launcher.waitAsyncJob(adminDB, jobSeqId)
        launcher.stopAsyncBatchDaemon(p)

        mongoUtil.waitForOutputLog(new LogCondition(message: 'job finished.[JobName:asyncJobEmulateLongProcessing][ExitStatus:STOPPED]'))

        def summaryTable = jobDB.getTable("sales_performance_summary")
        def jobExecutionTable = adminDB.createQueryTable("job_execution",
                "SELECT * FROM batch_job_execution WHERE job_execution_id = " + runningJobExecutionId
        )
        def jobRequestTable = adminDB.getTable("batch_job_request")
        def jobFinishLog = mongoUtil.findOne(new LogCondition(message: 'job finished.[JobName:asyncJobEmulateLongProcessing][ExitStatus:STOPPED]'))

        then:
        summaryTable.rowCount > 0 && summaryTable.rowCount < 10 // Assume to stop within 10 seconds with enough time.
        jobExecutionTable.getValue(0, "exit_code") == 'STOPPED'
        jobRequestTable.getValue(0, "job_execution_id") == runningJobExecutionId
        exitValue == 0
        jobFinishLog != null
    }

    // Testcase 5, test no.1
    def "Customized tables and SQL are available"() {
        setup:
        // Use another connection to avoid using cache of the connection
        def adminDB2 = new DBUnitUtil('admin')

        // Drop job request table.
        adminDB2.executeSqlScript(adminDB.conf.ch04.customedDropSqlsFilePaths as String[])

        // Create customized job request table.
        adminDB2.executeSqlScript(adminDB.conf.ch04.customedCreateSqlFilePaths as String[])

        adminDB2.insert(DBUnitUtil.createDataSet({
            batch_job_request {
                job_name | job_parameter | polling_status | create_date | priority
                "asyncJobCustomizedBatchJobRequest" | "registrationOrder=1,executionOrder=3" | "INIT" | "[now]" | 3
                "asyncJobCustomizedBatchJobRequest" | "registrationOrder=2,executionOrder=1" | "INIT" | "[now]" | 1
                "asyncJobCustomizedBatchJobRequest" | "registrationOrder=3,executionOrder=2" | "INIT" | "[now]" | 2
            }
        }))
        def initJobRequestTable = adminDB2.createQueryTable("init",
                "SELECT * FROM batch_job_request ORDER BY priority ASC")
        def env = ["async-batch-daemon.job-concurrency-num=1"] as String[]
        def command = "java -cp target/dependency/* ${AsyncBatchDaemon.class.name} META-INF/ch04/asyncjobwithdb/async-batch-daemon-customized-repository.xml"

        when:
        def p = JobLauncher.executeProcess(command, env, null)
        launcher.waitAsyncJob(adminDB2, (long) initJobRequestTable.getValue(2, "job_seq_id"), 180 * 1000L)
        launcher.stopAsyncBatchDaemon(p)

        mongoUtil.waitForOutputLog(new LogCondition(message: 'Async Batch Daemon stopped after all jobs completed.'))

        def jobStartLogs = mongoUtil.find(new LogCondition(message: ~/Customized job request execution info/))

        then:
        jobStartLogs.size() == 3
        def sortLogs = jobStartLogs.sort {l,r -> l.timestamp <=> r.timestamp}

        def jobParam1 = getKeyValues(sortLogs.get(0).message)
        def jobParam2 = getKeyValues(sortLogs.get(1).message)
        def jobParam3 = getKeyValues(sortLogs.get(2).message)

        jobParam1.get("executionOrder") == '1'
        jobParam1.get("registrationOrder") == '2'
        jobParam2.get("executionOrder") == '2'
        jobParam2.get("registrationOrder") == '3'
        jobParam3.get("executionOrder") == '3'
        jobParam3.get("registrationOrder") == '1'

        cleanup:
        // Drop customized job request table and create default job request table.
        adminDB2.executeSqlScript(adminDB.conf.ch04.customedDropSqlsFilePaths as String[])
        adminDB2.executeSqlScript(adminDB.conf.ch04.defaultCreateSqlFilePaths as String[])
        adminDB2.close()
    }

    // Testcase 5, test no.2
    def "Customized tables and SQL are available (using query parameters)"() {
        setup:
        // Use another connection to avoid using cache of the connection
        def adminDB2 = new DBUnitUtil('admin')

        // Drop job request table.
        adminDB2.executeSqlScript(adminDB.conf.ch04.customedDropSqlsFilePaths as String[])

        // Create customized job request table.
        adminDB2.executeSqlScript(adminDB.conf.ch04.customedCreateSqlFilePaths as String[])

        adminDB2.insert(DBUnitUtil.createDataSet({
            batch_job_request {
                job_name                                          |  polling_status | create_date | group_id
                "asyncJobCustomizedBatchJobRequestWithQueryParam" | "INIT"          | "[now]"     | "G1"
                "asyncJobCustomizedBatchJobRequestWithQueryParam" | "INIT"          | "[now]"     | "G2"
                "asyncJobCustomizedBatchJobRequestWithQueryParam" | "INIT"          | "[now]"     | "G3"
                "asyncJobCustomizedBatchJobRequestWithQueryParam" | "INIT"          | "[now]"     | "G1"
                "asyncJobCustomizedBatchJobRequestWithQueryParam" | "INIT"          | "[now]"     | "G2"
            }
        }))
        def initJobRequestTable = adminDB2.createQueryTable("init",
                "SELECT * FROM batch_job_request WHERE group_id = 'G1' ORDER BY job_seq_id ASC")
        def env = ["async-batch-daemon.job-concurrency-num=1", "GROUP_ID=G1"] as String[]
        def command = "java -cp target/dependency/* ${AsyncBatchDaemon.class.name} META-INF/ch04/asyncjobwithdb/async-batch-daemon-query-params.xml"

        when:
        def p = JobLauncher.executeProcess(command, env, null)
        launcher.waitAsyncJob(adminDB2, (long) initJobRequestTable.getValue(1, "job_seq_id"), 180 * 1000L)
        launcher.stopAsyncBatchDaemon(p)

        mongoUtil.waitForOutputLog(new LogCondition(message: 'Async Batch Daemon stopped after all jobs completed.'))

        def jobStartLogs = mongoUtil.find(new LogCondition(message: ~/Customized job request execution info \[groupId:G1]/))

        then:
        jobStartLogs.size() == 2

        def actualTable = adminDB2.getIncludedColumnsTable("batch_job_request", ["job_seq_id", "polling_status", "group_id"] as String[])
        def expectDataSet = DBUnitUtil.createDataSet({
            batch_job_request {
                job_seq_id | polling_status | group_id
                1          | "EXECUTED"     | "G1"
                2          | "INIT"         | "G2"
                3          | "INIT"         | "G3"
                4          | "EXECUTED"     | "G1"
                5          | "INIT"         | "G2"
            }
        })
        DBUnitUtil.assertEquals(expectDataSet.getTable("batch_job_request"), actualTable)

        cleanup:
        // Drop customized job request table and create default job request table.
        adminDB2.executeSqlScript(adminDB.conf.ch04.customedDropSqlsFilePaths as String[])
        adminDB2.executeSqlScript(adminDB.conf.ch04.defaultCreateSqlFilePaths as String[])
        adminDB2.close()
    }

    // Testcase 6, test no.1
    @Unroll
    def "Customized clock for timestamp (timezone : #timezone)"() {
        setup:
        def jobRequest = new JobRequest(
                jobName: 'asyncJobCustomizedClockTask'
        )
        def command = "java -Duser.timezone=${timezone} -cp target/dependency/* ${AsyncBatchDaemon.class.name} META-INF/ch04/asyncjobwithdb/async-batch-daemon-clock.xml"
        def jobSeqId = launcher.registerAsyncJob(adminDB, jobRequest)

        when:
        def p = JobLauncher.executeProcess(command)

        launcher.waitAsyncJob(adminDB, jobSeqId)
        launcher.stopAsyncBatchDaemon(p)

        mongoUtil.waitForOutputLog(new LogCondition(message: 'Async Batch Daemon stopped after all jobs completed.'))

        then:
        def polledLog = mongoUtil.findOne(
                new LogCondition(
                        message: "==> Parameters: POLLED(String), null, ${updateDate}, 1(Long), INIT(String)"
                )
        )
        def executedLog = mongoUtil.findOne(
                new LogCondition(
                        message: "==> Parameters: EXECUTED(String), 1(Long), ${updateDate}, 1(Long), POLLED(String)"
                )
        )

        polledLog != null
        executedLog != null

        def actualUpdateDateTimestamp = adminDB.getTable("batch_job_request").getValue(0, "update_date") as Timestamp
        def actualUpdateDateZonedDataTime = ZonedDateTime.of(actualUpdateDateTimestamp.toLocalDateTime(), ZoneId.of(timezone, ZoneId.SHORT_IDS))
        actualUpdateDateZonedDataTime.toInstant() == ZonedDateTime.parse('2016-12-31T16:00-08:00[America/Los_Angeles]').toInstant()

        where :
        timezone                | updateDate
        "UTC"                   | "2017-01-01 00:00:00.0(Timestamp)"
        "America/Los_Angeles"   | "2016-12-31 16:00:00.0(Timestamp)"
    }

    // Testcase 7, test no.1 and Testcase 2, test no.2
    def "Job is executed exclusively by multiple startup and optimistic lock of daemon"() {
        setup:
        jobDB.deleteAll(["sales_performance_detail", "sales_performance_summary"] as String[])
        def jobRequest = new JobRequest(
                jobName: 'asyncJobMultipleStartupConfirmJob'
        )
        def jobSeqId = launcher.registerAsyncJob(adminDB, jobRequest)
        def env_interval = "async-batch-daemon.polling-interval=60000"
        def sql = adminDB.sql
        def pessimisticLockSql = "SELECT job_seq_id FROM batch_job_request WHERE job_seq_id = ? FOR UPDATE"
        def timeoutCount = 120

        when:
        def p1 = null
        def p2 = null
        def count = 0
        sql.withTransaction {
            sql.firstRow(pessimisticLockSql, [jobSeqId])

            p1 = launcher.startAsyncBatchDaemon(["app.daemonName=daemon01", env_interval] as String[])
            p2 = launcher.startAsyncBatchDaemon(["app.daemonName=daemon02", env_interval] as String[])

            // Wait until the third poll is confirmed in the faulty environment.
            while (count < timeoutCount) {
                def pollingLog = mongoUtil.find(new LogCondition(
                        message: ~/Preparing: UPDATE batch_job_request SET polling_status/))
                if (pollingLog.size() >= 2) {
                    break
                }
                count++
                sleep(500L)
            }
            sleep(1000L)
        }

        launcher.waitAsyncJob(adminDB, jobSeqId)

        launcher.stopAsyncBatchDaemon(p1)

        mongoUtil.waitForOutputLog(new LogCondition(message: 'Async Batch Daemon stopped after all jobs completed.'))

        def executedDaemonLog = mongoUtil.find(new LogCondition(message: ~/Job started on daemon/))
        def dbUpdateLog = mongoUtil.find(new LogCondition(message: '<==    Updates: 0'))

        then:
        count < timeoutCount    // Timeout occurrence.
        executedDaemonLog.size() == 1
        // Confirm optimism lock
        dbUpdateLog.size() == 1
    }

    // Testcase 8, test no.1
    def "The bean is overwritten in order not to modularize it"() {
        setup:
        def jobRequest1 = new JobRequest(
                jobName: 'asyncJobModularizeConfirmJob01'
        )
        def jobSeqId1 = launcher.registerAsyncJob(adminDB, jobRequest1)
        def jobRequest2 = new JobRequest(
                jobName: 'asyncJobModularizeConfirmJob02'
        )
        def jobSeqId2 = launcher.registerAsyncJob(adminDB, jobRequest2)
        def command = "java -cp target/dependency/* ${AsyncBatchDaemon.class.name} META-INF/ch04/asyncjobwithdb/async-batch-daemon-nomodule.xml"

        when:
        def p = JobLauncher.executeProcess(command)
        launcher.waitAsyncJob(adminDB, jobSeqId1)
        launcher.waitAsyncJob(adminDB, jobSeqId2)
        launcher.stopAsyncBatchDaemon(p)

        mongoUtil.waitForOutputLog(new LogCondition(message: 'Async Batch Daemon stopped after all jobs completed.'))

        def itemProceesorLog1 = mongoUtil.find(new LogCondition(message: 'Processing on ModularizationConfirmationItemProcessor01.'))
        def itemProceesorLog2 = mongoUtil.find(new LogCondition(message: 'Processing on ModularizationConfirmationItemProcessor02.'))

        then:
        itemProceesorLog1.size() != itemProceesorLog2.size()
        itemProceesorLog1.size() == 0 ? itemProceesorLog2.size() != 0 : itemProceesorLog2.size() == 0
    }

    // Testcase 8, test no.2
    def "Bean is not overwritten because it is modularized"() {
        setup:
        def jobRequest1 = new JobRequest(
                jobName: 'asyncJobModularizeConfirmJob01'
        )
        def jobSeqId1 = launcher.registerAsyncJob(adminDB, jobRequest1)
        def jobRequest2 = new JobRequest(
                jobName: 'asyncJobModularizeConfirmJob02'
        )
        def jobSeqId2 = launcher.registerAsyncJob(adminDB, jobRequest2)

        when:
        def p = launcher.startAsyncBatchDaemon()
        launcher.waitAsyncJob(adminDB, jobSeqId1, 180 * 1000L)
        launcher.waitAsyncJob(adminDB, jobSeqId2, 180 * 1000L)
        launcher.stopAsyncBatchDaemon(p)

        mongoUtil.waitForOutputLog(new LogCondition(message: 'Async Batch Daemon stopped after all jobs completed.'))

        def itemProceesorLog1 = mongoUtil.findOne(new LogCondition(message: 'Processing on ModularizationConfirmationItemProcessor01.'))
        def itemProceesorLog2 = mongoUtil.findOne(new LogCondition(message: 'Processing on ModularizationConfirmationItemProcessor02.'))

        then:
        itemProceesorLog1 != null
        itemProceesorLog2 != null
    }

    // utils
    def countByPollingStatus(ITable iTable, String status) {
        def count = 0
        for (int i in 0..<iTable.rowCount) {
            if (iTable.getValue(i, "polling_status") == status) {
                count++
            }
        }
        count
    }

    def findJobSeqIdByPollingStatus(ITable iTable, String status) {
        def ids = []
        for (int i in 0..<iTable.rowCount) {
            if (iTable.getValue(i, "polling_status") == status) {
                ids << iTable.getValue(i, "job_seq_id")
            }
        }
        ids
    }

    def getKeyValues(String params) {
        def keyValues = [:]
        def index = params.indexOf("[")
        while (index != -1) {
            def nextIndex = params.indexOf(":", index)
            if (nextIndex == -1) {
                break
            }
            def key = params.substring(index + 1, nextIndex)
            def endIndex = params.indexOf("]", nextIndex)
            endIndex = endIndex == -1 ? params.length() - 1 : endIndex
            def value = params.substring(nextIndex + 1, endIndex)
            keyValues.put(key, value)
            index = params.indexOf("[", endIndex)
        }

        keyValues
    }
}
