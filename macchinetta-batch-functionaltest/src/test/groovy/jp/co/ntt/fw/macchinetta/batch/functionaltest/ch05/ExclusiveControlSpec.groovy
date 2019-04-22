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
import org.springframework.batch.core.launch.support.CommandLineJobRunner
import org.springframework.batch.core.step.AbstractStep
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.EmptyResultDataAccessException
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.FileExclusiveTasklet
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

/**
 * Function test of exclusive control.
 *
 * @since 5.0.0
 */
@Slf4j
@Narrative("""
A list of test cases is shown below.
1. Test file exclusive control
2. Test database exclusive control
""")
class ExclusiveControlSpec extends Specification {

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

    def setup() {
        log.debug("### Spec case of [{}]", testName.methodName)
        adminDB.dropAndCreateTable()
        jobDB.dropAndCreateTable()
        mongoUtil.deleteAll()
    }

    def cleanupSpec() {
        mongoUtil.close()
        adminDB.close()
        jobDB.close()
    }

    // testcase 1, test no.1
    @Unroll
    def "Confirming how to realize exclusive control of files between processes (#testcase)"() {
        setup:
        jobDB.insert(DBUnitUtil.createDataSet({
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "0001" | 2016 | 12 | "C0001" | 1001
                "0001" | 2016 | 12 | "C0002" | 1002
                "0001" | 2016 | 12 | "C0003" | 1003
                "0001" | 2016 | 12 | "C0004" | 1004
                "0001" | 2016 | 12 | "C0005" | 1005
                "0001" | 2016 | 12 | "C0006" | 1006
                "0001" | 2016 | 12 | "C0007" | 1007
                "0001" | 2016 | 12 | "C0008" | 1008
                "0001" | 2016 | 12 | "C0009" | 1009
                "0001" | 2016 | 12 | "C0010" | 1010
                "0002" | 2016 | 12 | "C0101" | 2001
                "0002" | 2016 | 12 | "C0102" | 2002
                "0002" | 2016 | 12 | "C0103" | 2003
                "0002" | 2016 | 12 | "C0104" | 2004
                "0002" | 2016 | 12 | "C0105" | 2005
                "0002" | 2016 | 12 | "C0106" | 2006
                "0002" | 2016 | 12 | "C0107" | 2007
                "0002" | 2016 | 12 | "C0108" | 2008
                "0002" | 2016 | 12 | "C0109" | 2009
                "0002" | 2016 | 12 | "C0110" | 2010
            }
        }))
        def outputFileName = "./files/test/output/ch05/exclusivecontorol/exclusiveFile.csv"
        def outputFile = new File(outputFileName)
        Files.deleteIfExists(outputFile.toPath())
        Files.createDirectories(outputFile.parentFile.toPath())

        def lockFileName = "./files/test/output/ch05/exclusivecontorol/exclusiveFile.lock"
        def lockFile = new File(lockFileName)
        lockFile.createNewFile()

        def command = "java -cp target/dependency/* ${CommandLineJobRunner.class.name} " +
                "${jobFilePath} ${jobName} outputFile=${outputFileName} lockFile=${lockFileName}"

        when:

        Process p1 = launcher.executeProcess(command + " branchId=0001", ["processName=p1"] as String[])
        Process p2 = launcher.executeProcess(command + " branchId=0002", ["processName=p2"] as String[])

        p1.waitFor()
        p2.waitFor()

        then:
        p1.exitValue() == 0 ? p2.exitValue() == 255 : p2.exitValue() == 0
        p2.exitValue() == 0 ? p1.exitValue() == 255 : p1.exitValue() == 0

        def lockedProcess = p1.exitValue() == 0 ? "p1" : "p2"
        def exclusiveProcess = p1.exitValue() == 0 ? "p2" : "p1"

        def expectFile = new File("./files/expect/output/ch05/exclusivecontrol/exclusiveFile_expect_${lockedProcess}.csv")
        outputFile.readLines() == expectFile.readLines()

        mongoUtil.find(new LogCondition(
                message: "Acquire lock. [processName=${lockedProcess}]",
                logger: loggerName,
                level: 'INFO'
        )).size() == 1
        mongoUtil.find(new LogCondition(
                message: "Acquire lock. [processName=${exclusiveProcess}]",
                logger: loggerName,
                level: 'INFO'
        )).size() == 0

        mongoUtil.find(new LogCondition(
                message: "Failed to acquire lock. [lockFile=${lockFileName}] [processName=${lockedProcess}]",
                logger: loggerName,
                level: 'ERROR'
        )).size() == 0
        mongoUtil.find(new LogCondition(
                message: "Failed to acquire lock. [lockFile=${lockFileName}] [processName=${exclusiveProcess}]",
                logger: loggerName,
                level: 'ERROR'
        )).size() == 1


        cleanup:
        Files.deleteIfExists(outputFile.toPath())
        Files.deleteIfExists(lockFile.toPath())

        where:
        testcase  | jobFilePath                                                  | jobName              || loggerName
        "Tasklet" | "META-INF/jobs/ch05/exclusivecontrol/fileLockTaskletJob.xml" | "fileLockTaskletJob" || FileExclusiveTasklet.class.name
    }

    // Testcase 2, test no 1,2,3
    @Unroll
    def "Confirmation of how to realize optimistic lock. assertUpdates=#assertUpdates (#testcase)"() {
        setup:
        jobDB.insert(DBUnitUtil.createDataSet({
            branch_mst {
                branch_id | branch_name | branch_address | branch_tel | create_date
                "0001" | "Tokyo" | "Tokyo Address" | "03" | "[now]"
            }
        }))
        def command1 = "java -cp target/dependency/* ${CommandLineJobRunner.class.name} " +
                "${jobPath} ${jobName} branchId=0001 identifier=p01 assertUpdates=${assertUpdates}"
        def command2 = "java -cp target/dependency/* ${CommandLineJobRunner.class.name} " +
                "${jobPath} ${jobName} branchId=0001 identifier=p02 assertUpdates=${assertUpdates}"
        def pessimisticLockSql = "SELECT branch_id FROM branch_mst WHERE branch_id = ? FOR UPDATE"
        def timeoutCount = 120

        def sql = jobDB.sql

        when:
        Process p1 = null
        Process p2 = null
        def count = 0
        sql.withTransaction {
            sql.firstRow(pessimisticLockSql, ["0001"])

            p1 = launcher.executeProcess(command1)
            p2 = launcher.executeProcess(command2)

            while (count < timeoutCount) {
                def pollingLog = mongoUtil.find(new LogCondition(
                        message: ~/Preparing: UPDATE branch_mst SET branch_name/))
                if (pollingLog.size() >= 2) {
                    break
                }
                count++
                sleep(500L)
            }
            if (count == timeoutCount) {
                throw new IllegalStateException("test time out.")
            }
        }
        p1.waitFor()
        p2.waitFor()

        then:
        p1.exitValue() == 0 ? p2.exitValue() == optimisticJobExitValue : p2.exitValue() == 0
        p2.exitValue() == 0 ? p1.exitValue() == optimisticJobExitValue : p1.exitValue() == 0

        def exitValueMap = ["p01": p1.exitValue(), "p02": p2.exitValue()]

        def updateLog = mongoUtil.find(new LogCondition(
                message: ~/Parameters: /,
                logger: 'jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.repository.ExclusiveControlRepository.branchExclusiveUpdate',
                level: 'DEBUG'
        ))

        updateLog.size() == 2

        def exceptionLog = mongoUtil.findOne(new LogCondition(
                message: ~/Encountered an error executing step chunkOptimisticLockCheckJob.step01/,
                logger: AbstractStep.name,
                level: 'ERROR'
        ))
        exceptionLog?.throwable?._class == lockFailedException

        // check succeeded process for updated.
        exitValueMap[jobDB.getTable("branch_mst").getValue(0, "branch_name").toString().substring('Tokyo - '.length())] == 0

        where:
        testcase  | jobPath                                                                 | jobName                         | assertUpdates || optimisticJobExitValue | lockFailedException
        "Chunk"   | "META-INF/jobs/ch05/exclusivecontrol/chunkOptimisticLockCheckJob.xml"   | "chunkOptimisticLockCheckJob"   | "false"       || 0                      | null
        "Chunk"   | "META-INF/jobs/ch05/exclusivecontrol/chunkOptimisticLockCheckJob.xml"   | "chunkOptimisticLockCheckJob"   | "true"        || 255                    | EmptyResultDataAccessException.class
        "Tasklet" | "META-INF/jobs/ch05/exclusivecontrol/taskletOptimisticLockCheckJob.xml" | "taskletOptimisticLockCheckJob" | "none"        || 0                      | null
    }

    // Testcase 2, test no 4
    def "Confirming how to realize pessimistic locking by chunk [Detect lock]"() {
        setup:
        def branchMst = DBUnitUtil.createDataSet({
            branch_mst {
                branch_id | branch_name | branch_address | branch_tel | create_date
                "0001" | "Tokyo" | "Tokyo Address" | "03" | "[now]"
                "0002" | "Osaka" | "Osaka Address" | "06" | "[now]"
                "0003" | "Tokyo" | "Tokyo3 Address" | "03" | "[now]"
                "0004" | "Tokyo" | "Tokyo4 Address" | "03" | "[now]"
                "0005" | "Tokyo" | "Tokyo5 Address" | "03" | "[now]"
            }
        })
        jobDB.insert(branchMst)
        def pessimisticLockSql = "SELECT branch_id FROM branch_mst WHERE branch_id = ? FOR UPDATE"

        def sql = jobDB.sql

        when:
        def exitValue = -1
        sql.withTransaction {
            sql.firstRow(pessimisticLockSql, ["0001"])

            exitValue = launcher.syncJob(new JobRequest(
                    jobFilePath: 'META-INF/jobs/ch05/exclusivecontrol/chunkPessimisticLockCheckJob.xml',
                    jobName: 'chunkPessimisticLockCheckJob',
                    jobParameter: 'identifier=p01 branchName=Tokyo'
            ))

        }

        then:
        exitValue == 255

        mongoUtil.waitForOutputLog(new LogCondition(message: 'job finished.[JobName:chunkPessimisticLockCheckJob][ExitStatus:FAILED]'))
        mongoUtil.find(new LogCondition(
                message: '==>  Preparing: SELECT branch_id AS branchId, branch_name AS branchName, branch_address AS ' +
                        'branchAddress, branch_tel AS branchTel, create_date AS createDate, update_date AS updateDate ' +
                        'FROM branch_mst WHERE branch_id = ? AND branch_name = ? FOR UPDATE NOWAIT ')).size() == 1
        def errorLog = mongoUtil.find(new LogCondition(
                message: 'Encountered an error executing step chunkPessimisticLockCheckJob.step01 in job chunkPessimisticLockCheckJob'))
        errorLog.size() == 1
        def lockLog = errorLog.get(0)
        lockLog.throwable._class == CannotAcquireLockException.class

        DBUnitUtil.assertEquals(branchMst.getTable("branch_mst"),
                jobDB.getExcludedColumnsTable("branch_mst", ["update_date"] as String[]))
        jobDB.getTable("branch_mst").getValue(0, "update_date") == null
    }

    // Testcase 2, test no 5
    def "Confirming how to realize pessimistic locking by chunk [Do not detect lock]"() {
        setup:
        def branchMst = DBUnitUtil.createDataSet({
            branch_mst {
                branch_id | branch_name | branch_address | branch_tel | create_date
                "0001" | "Tokyo" | "Tokyo Address" | "03" | "[now]"
                "0002" | "Osaka" | "Osaka Address" | "06" | "[now]"
                "0003" | "Tokyo" | "Tokyo3 Address" | "03" | "[now]"
                "0004" | "Tokyo" | "Tokyo4 Address" | "03" | "[now]"
                "0005" | "Tokyo" | "Tokyo5 Address" | "03" | "[now]"
            }
        })
        jobDB.insert(branchMst)

        def expectData = DBUnitUtil.createDataSet({
            branch_mst {
                branch_id | branch_name | branch_address | branch_tel
                "0001" | "Tokyo - p01" | "Tokyo Address - p01" | "03"
                "0002" | "Osaka" | "Osaka Address" | "06"
                "0003" | "Tokyo - p01" | "Tokyo3 Address - p01" | "03"
                "0004" | "Tokyo - p01" | "Tokyo4 Address - p01" | "03"
                "0005" | "Tokyo - p01" | "Tokyo5 Address - p01" | "03"
            }
        })

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/exclusivecontrol/chunkPessimisticLockCheckJob.xml',
                jobName: 'chunkPessimisticLockCheckJob',
                jobParameter: 'identifier=p01 branchName=Tokyo'
        ))

        then:
        exitValue == 0

        DBUnitUtil.assertEquals(expectData.getTable("branch_mst"),
                jobDB.getExcludedColumnsTable("branch_mst", ["create_date", "update_date"] as String[]))
    }

    // Testcase 2, test no 6
    def "Confirming how to realize pessimistic locking by tasklet [Detect lock]"() {
        setup:
        def branchMst = DBUnitUtil.createDataSet({
            branch_mst {
                branch_id | branch_name | branch_address | branch_tel | create_date
                "0001" | "Tokyo" | "Tokyo Address" | "03" | "[now]"
            }
        })
        jobDB.insert(branchMst)
        def pessimisticLockSql = "SELECT branch_id FROM branch_mst WHERE branch_id = ? FOR UPDATE"

        def sql = jobDB.sql

        when:
        def exitValue = -1
        sql.withTransaction {
            sql.firstRow(pessimisticLockSql, ["0001"])

            exitValue = launcher.syncJob(new JobRequest(
                    jobFilePath: 'META-INF/jobs/ch05/exclusivecontrol/taskletPessimisticLockCheckJob.xml',
                    jobName: 'taskletPessimisticLockCheckJob',
                    jobParameter: 'branchId=0001 identifier=p01'
            ))

        }

        then:
        exitValue == 255
        mongoUtil.waitForOutputLog(new LogCondition(message: 'job finished.[JobName:taskletPessimisticLockCheckJob][ExitStatus:FAILED]'))
        mongoUtil.find(new LogCondition(
                message: '==>  Preparing: SELECT branch_id AS branchId, branch_name AS branchName, branch_address AS ' +
                        'branchAddress, branch_tel AS branchTel, create_date AS createDate, update_date AS updateDate ' +
                        'FROM branch_mst WHERE branch_id = ? FOR UPDATE NOWAIT ')).size() == 1
        def errorLog = mongoUtil.find(new LogCondition(
                message: 'Encountered an error executing step taskletPessimisticLockCheckJob.step01 in job taskletPessimisticLockCheckJob'))
        errorLog.size() == 1
        def lockLog = errorLog.get(0)
        lockLog.throwable._class == CannotAcquireLockException.class

        DBUnitUtil.assertEquals(branchMst.getTable("branch_mst"),
                jobDB.getExcludedColumnsTable("branch_mst", ["update_date"] as String[]))
        jobDB.getTable("branch_mst").getValue(0, "update_date") == null
    }

    // Testcase 2, test no 7
    def "Confirming how to realize pessimistic locking by tasklet [Do not detect lock]"() {
        setup:
        def branchMst = DBUnitUtil.createDataSet({
            branch_mst {
                branch_id | branch_name | branch_address | branch_tel | create_date
                "0001" | "Tokyo" | "Tokyo Address" | "03" | "[now]"
            }
        })
        jobDB.insert(branchMst)

        def expectData = DBUnitUtil.createDataSet({
            branch_mst {
                branch_id | branch_name | branch_address | branch_tel
                "0001" | "Tokyo" | "Tokyo Address - updated." | "03"
            }
        })

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/exclusivecontrol/taskletPessimisticLockCheckJob.xml',
                jobName: 'taskletPessimisticLockCheckJob',
                jobParameter: 'branchId=0001 identifier=p01'
        ))

        then:
        exitValue == 0

        DBUnitUtil.assertEquals(expectData.getTable("branch_mst"),
                jobDB.getExcludedColumnsTable("branch_mst", ["create_date", "update_date"] as String[]))
    }

}
