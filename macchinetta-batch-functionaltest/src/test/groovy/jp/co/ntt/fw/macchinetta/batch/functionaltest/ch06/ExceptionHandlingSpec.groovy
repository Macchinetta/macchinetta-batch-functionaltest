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
import org.junit.Rule
import org.junit.rules.TestName
import org.springframework.batch.core.step.AbstractStep
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.exceptionhandling.AmountCheckProcessor
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.exceptionhandling.AmountCheckWithSkipProcessor
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.exceptionhandling.ChunkAroundListener
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.exceptionhandling.ChunkComponentListener
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.exceptionhandling.JobErrorLoggingListener
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.exceptionhandling.RetryableAmountCheckProcessor
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.exceptionhandling.SalesPerformanceTasklet
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.exceptionhandling.SalesPerformanceWithSkipTasklet
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.exceptionhandling.SkipLoggingListener
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.exceptionhandling.StepErrorLoggingListener
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
 * Function test of exception handling.
 *
 * @since 2.0.1
 */
@Slf4j
@Narrative("""
A list of test cases is shown below.
1. Testing the exception-handling per a step
2. Testing the exception-handling per a job
3. Testing the exception-handling to continue a job
""")
class ExceptionHandlingSpec extends Specification {

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
        adminDB.dropAndCreateTable()
        jobDB.dropAndCreateTable()
    }

    def setup() {
        log.debug("### Spec case of [{}]", testName.methodName)
        mongoUtil.deleteAll()
    }

    def cleanupSpec() {
        mongoUtil.close()
        adminDB.close()
        jobDB.close()
    }

    // testcase 1.1
    def "handle the exception that occurred in ItemReader of Chunk"() {
        setup:
        // Copy input file form incorrect data file.
        def inputFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance.csv')
        def incorrectFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance_incorrect-01.csv')
        Files.deleteIfExists(inputFile.toPath())
        inputFile << incorrectFile.readBytes()

        jobDB.deleteAll(['sales_performance_detail'] as String[])

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/exceptionhandling/jobSalesPerfAtComponentsError.xml',
                jobName: 'jobSalesPerfAtComponentsError',
                jobParameter: 'inputFile=./files/test/input/ch06/exceptionhandling/sales_performance.csv'))
        then:
        exitCode == 255
        def log = mongoUtil.find(new LogCondition(
                level: 'ERROR',
                logger: ChunkComponentListener.class.name,
                message: 'Exception occurred while reading.'))
        log.size() > 0
        log.get(0).throwable._class.name == 'org.springframework.batch.item.file.FlatFileParseException'

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
    }

    // testcase 1.2
    def "handle the exception that occurred in ItemProcessor of Chunk"() {
        setup:
        // Copy input file form incorrect data file.
        def inputFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance.csv')
        def incorrectFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance_incorrect-02.csv')
        Files.deleteIfExists(inputFile.toPath())
        inputFile << incorrectFile.readBytes()

        jobDB.deleteAll(['sales_performance_detail'] as String[])

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/exceptionhandling/jobSalesPerfAtComponentsError.xml',
                jobName: 'jobSalesPerfAtComponentsError',
                jobParameter: 'inputFile=./files/test/input/ch06/exceptionhandling/sales_performance.csv'))
        then:
        exitCode == 255
        def log = mongoUtil.find(new LogCondition(
                level: 'ERROR',
                logger: ChunkComponentListener.class.name,
                message: ~/Exception occurred while processing/))
        log.size() > 0
        def log0 = log.get(0)
        log0.throwable._class.name == 'java.lang.IllegalStateException'
        log0.throwable.message == 'check error at processor.'
        log0.throwable.cause._class.name == 'java.lang.ArithmeticException'
        log0.throwable.cause.message == 'must be less than 10000'

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
    }

    // testcase 1.3
    def "handle the exception that occurred in ItemWriter of Chunk"() {
        setup:
        // Copy input file form incorrect data file.
        def inputFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance.csv')
        def incorrectFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance_incorrect-03.csv')
        Files.deleteIfExists(inputFile.toPath())
        inputFile << incorrectFile.readBytes()

        jobDB.deleteAll(['sales_performance_detail'] as String[])

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/exceptionhandling/jobSalesPerfAtComponentsError.xml',
                jobName: 'jobSalesPerfAtComponentsError',
                jobParameter: 'inputFile=./files/test/input/ch06/exceptionhandling/sales_performance.csv'))
        then:
        exitCode == 255
        def log = mongoUtil.find(new LogCondition(
                level: 'ERROR',
                logger: ChunkComponentListener.class.name,
                message: ~/Exception occurred while processing/))
        log.size() > 0
        def log0 = log.get(0)
        log0.throwable._class.name == 'org.springframework.dao.DuplicateKeyException'

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
    }

    // testcase 1.4
    def "handle the exception that occurred in Tasklet"() {
        setup:
        // Copy input file form incorrect data file.
        def inputFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance.csv')
        def incorrectFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance_incorrect-02.csv')
        Files.deleteIfExists(inputFile.toPath())
        inputFile << incorrectFile.readBytes()

        jobDB.deleteAll(['sales_performance_detail'] as String[])

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/exceptionhandling/jobSalesPerfTasklet.xml',
                jobName: 'jobSalesPerfTask',
                jobParameter: 'inputFile=./files/test/input/ch06/exceptionhandling/sales_performance.csv'))
        then:
        exitCode == 255
        def log = mongoUtil.find(new LogCondition(
                level: 'ERROR',
                logger: SalesPerformanceTasklet.class.name,
                message: 'exception in tasklet.'))
        log.size() > 0
        def log0 = log.get(0)
        log0.throwable._class.name == 'java.lang.ArithmeticException'
        log0.throwable.message == 'must be less than 10000'

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
    }

    // testcase 1.5
    def "handle the exception that occurred in whole Chunk"() {
        setup:
        // Copy input file form incorrect data file.
        def inputFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance.csv')
        def incorrectFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance_incorrect-01.csv')
        Files.deleteIfExists(inputFile.toPath())
        inputFile << incorrectFile.readBytes()

        jobDB.deleteAll(['sales_performance_detail'] as String[])

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/exceptionhandling/jobSalesPerfAtChunkError.xml',
                jobName: 'jobSalesPerfAtChunkError',
                jobParameter: 'inputFile=./files/test/input/ch06/exceptionhandling/sales_performance.csv'))
        then:
        exitCode == 255
        def log = mongoUtil.find(new LogCondition(
                level: 'ERROR',
                logger: ChunkAroundListener.class.name,
                message: ~/Exception occurred while chunk/))
        log.size() > 0
        log.get(0).throwable._class.name == 'org.springframework.batch.item.file.FlatFileParseException'

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
    }

    // testcase 1.6
    def "cannot handle the exception that occurred outer Chunk"() {
        setup:
        jobDB.deleteAll(['sales_performance_detail'] as String[])

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/exceptionhandling/jobSalesPerfAtChunkError.xml',
                jobName: 'jobSalesPerfAtChunkError',
                jobParameter: 'inputFile=./files/test/input/ch06/exceptionhandling/not-found.csv'))
        then:
        exitCode == 255
        def log = mongoUtil.find(new LogCondition(
                logger: ChunkAroundListener.class.name))
        log.size() == 0
    }

    // testcase 1.7
    def "handle the exception that occurred in whole Chunk (implements Tasklet)"() {
        setup:
        // Copy input file form incorrect data file.
        def inputFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance.csv')
        def incorrectFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance_incorrect-01.csv')
        Files.deleteIfExists(inputFile.toPath())
        inputFile << incorrectFile.readBytes()

        jobDB.deleteAll(['sales_performance_detail'] as String[])

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/exceptionhandling/jobSalesPerfTaskletAtChunkError.xml',
                jobName: 'jobSalesPerfTaskletAtChunkError',
                jobParameter: 'inputFile=./files/test/input/ch06/exceptionhandling/sales_performance.csv'))
        then:
        exitCode == 255
        def log = mongoUtil.find(new LogCondition(
                level: 'ERROR',
                logger: ChunkAroundListener.class.name,
                message: ~/Exception occurred while chunk/))
        log.size() > 0
        log.get(0).throwable._class.name == 'org.springframework.batch.item.file.FlatFileParseException'

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
    }

    // testcase 1.8
    def "handle the exception that occurred outer Chunk (implements Tasklet)"() {
        setup:
        jobDB.deleteAll(['sales_performance_detail'] as String[])

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/exceptionhandling/jobSalesPerfTaskletAtChunkError.xml',
                jobName: 'jobSalesPerfTaskletAtChunkError',
                jobParameter: 'inputFile=./files/test/input/ch06/exceptionhandling/not-found.csv'))
        then:
        exitCode == 255
        def log = mongoUtil.find(new LogCondition(
                level: 'ERROR',
                logger: ChunkAroundListener.class.name,
                message: ~/Exception occurred while chunk/))
        log.size() > 0
        log.get(0).throwable._class.name == 'org.springframework.batch.item.ItemStreamException'
        log.get(0).throwable.cause._class.name == 'java.lang.IllegalStateException'
        log.get(0).throwable.cause.message ==~ /Input resource must exist.*/
    }

    // testcase 1.9
    def "handle the exception that occurred in step"() {
        setup:
        // Copy input file form incorrect data file.
        def inputFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance.csv')
        def incorrectFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance_incorrect-01.csv')
        Files.deleteIfExists(inputFile.toPath())
        inputFile << incorrectFile.readBytes()

        jobDB.deleteAll(['sales_performance_detail'] as String[])

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/exceptionhandling/jobSalesPerfAtStepError.xml',
                jobName: 'jobSalesPerfAtStepError',
                jobParameter: 'inputFile=./files/test/input/ch06/exceptionhandling/sales_performance.csv'))
        then:
        exitCode == 255
        def log = mongoUtil.find(new LogCondition(
                level: 'ERROR',
                logger: StepErrorLoggingListener.class.name,
                message: 'exception has occurred in job.'))
        log.size() > 0
        log.get(0).throwable._class.name == 'org.springframework.batch.item.file.FlatFileParseException'

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
    }

    // testcase 1.10
    def "handle the exception that occurred in step (implements Tasklet)"() {
        setup:
        // Copy input file form incorrect data file.
        def inputFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance.csv')
        def incorrectFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance_incorrect-01.csv')
        Files.deleteIfExists(inputFile.toPath())
        inputFile << incorrectFile.readBytes()

        jobDB.deleteAll(['sales_performance_detail'] as String[])

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/exceptionhandling/jobSalesPerfTaskletAtStepError.xml',
                jobName: 'jobSalesPerfTaskletAtStepError',
                jobParameter: 'inputFile=./files/test/input/ch06/exceptionhandling/sales_performance.csv'))
        then:
        exitCode == 255
        def log = mongoUtil.find(new LogCondition(
                level: 'ERROR',
                logger: StepErrorLoggingListener.class.name,
                message: 'exception has occurred in job.'))
        log.size() > 0
        log.get(0).throwable._class.name == 'org.springframework.batch.item.file.FlatFileParseException'

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
    }

    // testcase 2.1
    def "handle the exception that occurred in job"() {
        setup:
        // Copy input file form incorrect data file.
        def inputFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance.csv')
        def incorrectFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance_incorrect-02.csv')
        Files.deleteIfExists(inputFile.toPath())
        inputFile << incorrectFile.readBytes()

        jobDB.deleteAll(['sales_performance_detail'] as String[])

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/exceptionhandling/jobSalesPerfAtJobError.xml',
                jobName: 'jobSalesPerfAtJobError',
                jobParameter: 'inputFile=./files/test/input/ch06/exceptionhandling/sales_performance.csv'))
        then:
        exitCode == 255
        def log = mongoUtil.find(new LogCondition(
                level: 'ERROR',
                logger: JobErrorLoggingListener.class.name,
                message: 'exception has occurred in job.'))
        log.size() > 0
        log.get(0).throwable._class.name == 'java.lang.IllegalStateException'
        log.get(0).throwable.message == 'check error at processor.'

        def log2 = mongoUtil.find(new LogCondition(
                level: 'ERROR',
                logger: JobErrorLoggingListener.class.name,
                message: ~/detected error on this item processing/))
        log2.size() > 0

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
    }

    // testcase 3.1
    def "skip all incorrect data in ItemReader"() {
        setup:
        // Copy input file form incorrect data file.
        def inputFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance.csv')
        def incorrectFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance_incorrect-04.csv')
        Files.deleteIfExists(inputFile.toPath())
        inputFile << incorrectFile.readBytes()

        jobDB.deleteAll(['sales_performance_detail'] as String[])

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/exceptionhandling/jobSalesPerfAtSkipAllReadError.xml',
                jobName: 'jobSalesPerfAtSkipAllReadError',
                jobParameter: 'inputFile=./files/test/input/ch06/exceptionhandling/sales_performance.csv'))
        then:
        exitCode == 0

        def expect = DBUnitUtil.createDataSet({
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                '0001' | 2016 | 11 | 'cust01' | 1000
                '0001' | 2016 | 11 | 'cust02' | 2000
                '0001' | 2016 | 11 | 'cust03' | 3000
                '0001' | 2016 | 11 | 'cust04' | 4000
                '0001' | 2016 | 11 | 'cust05' | 5000
                '0001' | 2016 | 11 | 'cust06' | 6000
                '0001' | 2016 | 11 | 'cust07' | 7000
                '0001' | 2016 | 11 | 'cust08' | 8000
                '0001' | 2016 | 11 | 'cust09' | 9000
                '0001' | 2016 | 11 | 'cust10' | 10000
                '0001' | 2016 | 11 | 'cust11' | 1000
                '0001' | 2016 | 11 | 'cust12' | 2000
                '0001' | 2016 | 11 | 'cust13' | 3000
                '0001' | 2016 | 11 | 'cust14' | 4000
                '0001' | 2016 | 11 | 'cust18' | 8000
                '0001' | 2016 | 11 | 'cust19' | 9000
                '0001' | 2016 | 11 | 'cust20' | 10000
                '0001' | 2016 | 11 | 'cust21' | 1000
                '0001' | 2016 | 11 | 'cust22' | 2000
                '0001' | 2016 | 11 | 'cust23' | 3000
                '0001' | 2016 | 11 | 'cust24' | 4000
                '0001' | 2016 | 11 | 'cust25' | 5000
                '0001' | 2016 | 11 | 'cust26' | 6000
                '0001' | 2016 | 11 | 'cust27' | 7000
                '0001' | 2016 | 11 | 'cust28' | 8000
                '0001' | 2016 | 11 | 'cust29' | 9000
                '0001' | 2016 | 11 | 'cust30' | 10000
            }
        }).getTable('sales_performance_detail')
        def acutal = jobDB.getTable('sales_performance_detail')
        DBUnitUtil.assertEquals(expect, acutal)

        def log = mongoUtil.find(new LogCondition(
                level: 'WARN',
                logger: SkipLoggingListener.class.name,
                message: 'skipped a record in read.'))
        log.size() == 3
        log.get(0).throwable._class.name == 'org.springframework.batch.item.file.FlatFileParseException'

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
    }

    // testcase 3.2
    def "skip never incorrect data in ItemReader"() {
        setup:
        // Copy input file form incorrect data file.
        def inputFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance.csv')
        def incorrectFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance_incorrect-04.csv')
        Files.deleteIfExists(inputFile.toPath())
        inputFile << incorrectFile.readBytes()

        jobDB.deleteAll(['sales_performance_detail'] as String[])

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/exceptionhandling/jobSalesPerfAtSkipNeverReadError.xml',
                jobName: 'jobSalesPerfAtSkipNeverReadError',
                jobParameter: 'inputFile=./files/test/input/ch06/exceptionhandling/sales_performance.csv'))
        then:
        exitCode == 255

        def expect = DBUnitUtil.createDataSet({
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                '0001' | 2016 | 11 | 'cust01' | 1000
                '0001' | 2016 | 11 | 'cust02' | 2000
                '0001' | 2016 | 11 | 'cust03' | 3000
                '0001' | 2016 | 11 | 'cust04' | 4000
                '0001' | 2016 | 11 | 'cust05' | 5000
                '0001' | 2016 | 11 | 'cust06' | 6000
                '0001' | 2016 | 11 | 'cust07' | 7000
                '0001' | 2016 | 11 | 'cust08' | 8000
                '0001' | 2016 | 11 | 'cust09' | 9000
                '0001' | 2016 | 11 | 'cust10' | 10000
            }
        }).getTable('sales_performance_detail')
        def acutal = jobDB.getTable('sales_performance_detail')
        DBUnitUtil.assertEquals(expect, acutal)

        def log = mongoUtil.find(new LogCondition(
                level: 'WARN',
                logger: SkipLoggingListener.class.name))
        log.size() == 0

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
    }

    // testcase 3.3
    def "skip specified incorrect data in ItemReader by skippable-exception-classes"() {
        setup:
        // Copy input file form incorrect data file.
        def inputFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance.csv')
        def incorrectFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance_incorrect-04.csv')
        Files.deleteIfExists(inputFile.toPath())
        inputFile << incorrectFile.readBytes()

        jobDB.deleteAll(['sales_performance_detail'] as String[])

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/exceptionhandling/jobSalesPerfAtValidSkipReadError.xml',
                jobName: 'jobSalesPerfAtValidSkipReadError',
                jobParameter: 'inputFile=./files/test/input/ch06/exceptionhandling/sales_performance.csv'))
        then:
        exitCode == 255

        def expect = DBUnitUtil.createDataSet({
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                '0001' | 2016 | 11 | 'cust01' | 1000
                '0001' | 2016 | 11 | 'cust02' | 2000
                '0001' | 2016 | 11 | 'cust03' | 3000
                '0001' | 2016 | 11 | 'cust04' | 4000
                '0001' | 2016 | 11 | 'cust05' | 5000
                '0001' | 2016 | 11 | 'cust06' | 6000
                '0001' | 2016 | 11 | 'cust07' | 7000
                '0001' | 2016 | 11 | 'cust08' | 8000
                '0001' | 2016 | 11 | 'cust09' | 9000
                '0001' | 2016 | 11 | 'cust10' | 10000
            }
        }).getTable('sales_performance_detail')
        def acutal = jobDB.getTable('sales_performance_detail')
        DBUnitUtil.assertEquals(expect, acutal)

        def log = mongoUtil.find(new LogCondition(
                logger: SkipLoggingListener.class.name))
        log.size() == 0

        def errLog = mongoUtil.find(new LogCondition(
                level: 'ERROR',
                logger: AbstractStep.class.name,
                message: ~/Encountered an error executing step/))
        errLog.size() > 0
        def errLog0 = errLog.get(0)
        errLog0.throwable._class.name == 'org.springframework.batch.core.step.skip.SkipLimitExceededException'
        errLog0.throwable.message == "Skip limit of '2' exceeded"
        errLog0.throwable.cause._class.name == 'org.springframework.batch.item.file.FlatFileParseException'

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
    }

    // testcase 3.4
    def "skip no-limit incorrect data in ItemReader by policy"() {
        setup:
        // Copy input file form incorrect data file.
        def inputFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance.csv')
        def incorrectFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance_incorrect-04.csv')
        Files.deleteIfExists(inputFile.toPath())
        inputFile << incorrectFile.readBytes()

        jobDB.deleteAll(['sales_performance_detail'] as String[])

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/exceptionhandling/jobSalesPerfAtValidNolimitSkipReadError.xml',
                jobName: 'jobSalesPerfAtValidNolimitSkipReadError',
                jobParameter: 'inputFile=./files/test/input/ch06/exceptionhandling/sales_performance.csv'))
        then:
        exitCode == 0

        def expect = DBUnitUtil.createDataSet({
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                '0001' | 2016 | 11 | 'cust01' | 1000
                '0001' | 2016 | 11 | 'cust02' | 2000
                '0001' | 2016 | 11 | 'cust03' | 3000
                '0001' | 2016 | 11 | 'cust04' | 4000
                '0001' | 2016 | 11 | 'cust05' | 5000
                '0001' | 2016 | 11 | 'cust06' | 6000
                '0001' | 2016 | 11 | 'cust07' | 7000
                '0001' | 2016 | 11 | 'cust08' | 8000
                '0001' | 2016 | 11 | 'cust09' | 9000
                '0001' | 2016 | 11 | 'cust10' | 10000
                '0001' | 2016 | 11 | 'cust11' | 1000
                '0001' | 2016 | 11 | 'cust12' | 2000
                '0001' | 2016 | 11 | 'cust13' | 3000
                '0001' | 2016 | 11 | 'cust14' | 4000
                '0001' | 2016 | 11 | 'cust18' | 8000
                '0001' | 2016 | 11 | 'cust19' | 9000
                '0001' | 2016 | 11 | 'cust20' | 10000
                '0001' | 2016 | 11 | 'cust21' | 1000
                '0001' | 2016 | 11 | 'cust22' | 2000
                '0001' | 2016 | 11 | 'cust23' | 3000
                '0001' | 2016 | 11 | 'cust24' | 4000
                '0001' | 2016 | 11 | 'cust25' | 5000
                '0001' | 2016 | 11 | 'cust26' | 6000
                '0001' | 2016 | 11 | 'cust27' | 7000
                '0001' | 2016 | 11 | 'cust28' | 8000
                '0001' | 2016 | 11 | 'cust29' | 9000
                '0001' | 2016 | 11 | 'cust30' | 10000
            }
        }).getTable('sales_performance_detail')
        def acutal = jobDB.getTable('sales_performance_detail')
        DBUnitUtil.assertEquals(expect, acutal)

        def log = mongoUtil.find(new LogCondition(
                level: 'WARN',
                logger: SkipLoggingListener.class.name,
                message: 'skipped a record in read.'))
        log.size() == 3
        log.get(0).throwable._class.name == 'org.springframework.batch.item.file.FlatFileParseException'

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
    }

    // testcase 3.5
    def "skip all incorrect data in ItemProcessor by skippable-exception-classes"() {
        setup:
        // Copy input file form incorrect data file.
        def inputFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance.csv')
        def incorrectFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance_incorrect-05.csv')
        Files.deleteIfExists(inputFile.toPath())
        inputFile << incorrectFile.readBytes()

        jobDB.deleteAll(['sales_performance_detail'] as String[])

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/exceptionhandling/jobSalesPerfAtSkipAllProcError.xml',
                jobName: 'jobSalesPerfAtSkipAllProcError',
                jobParameter: 'inputFile=./files/test/input/ch06/exceptionhandling/sales_performance.csv'))
        then:
        exitCode == 0

        def expect = DBUnitUtil.createDataSet({
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                '0001' | 2016 | 11 | 'cust01' | 1000
                '0001' | 2016 | 11 | 'cust02' | 2000
                '0001' | 2016 | 11 | 'cust03' | 3000
                '0001' | 2016 | 11 | 'cust04' | 4000
                '0001' | 2016 | 11 | 'cust05' | 5000
                '0001' | 2016 | 11 | 'cust06' | 6000
                '0001' | 2016 | 11 | 'cust07' | 7000
                '0001' | 2016 | 11 | 'cust08' | 8000
                '0001' | 2016 | 11 | 'cust09' | 9000
                '0001' | 2016 | 11 | 'cust10' | 10000
                '0001' | 2016 | 11 | 'cust11' | 1000
                '0001' | 2016 | 11 | 'cust12' | 2000
                '0001' | 2016 | 11 | 'cust13' | 3000
                '0001' | 2016 | 11 | 'cust14' | 4000
                '0001' | 2016 | 11 | 'cust18' | 8000
                '0001' | 2016 | 11 | 'cust19' | 9000
                '0001' | 2016 | 11 | 'cust20' | 10000
                '0001' | 2016 | 11 | 'cust21' | 1000
                '0001' | 2016 | 11 | 'cust22' | 2000
                '0001' | 2016 | 11 | 'cust23' | 3000
                '0001' | 2016 | 11 | 'cust24' | 4000
                '0001' | 2016 | 11 | 'cust25' | 5000
                '0001' | 2016 | 11 | 'cust26' | 6000
                '0001' | 2016 | 11 | 'cust27' | 7000
                '0001' | 2016 | 11 | 'cust28' | 8000
                '0001' | 2016 | 11 | 'cust29' | 9000
                '0001' | 2016 | 11 | 'cust30' | 10000
            }
        }).getTable('sales_performance_detail')
        def acutal = jobDB.getTable('sales_performance_detail')
        DBUnitUtil.assertEquals(expect, acutal)

        def log = mongoUtil.find(new LogCondition(
                level: 'WARN',
                logger: SkipLoggingListener.class.name,
                message: ~/skipped a record in processor/))
        log.size() == 3
        def log0 = log.get(0)
        log0.throwable._class.name == 'java.lang.IllegalStateException'
        log0.throwable.message == 'check error at processor.'
        log0.throwable.cause._class.name == 'java.lang.ArithmeticException'
        log0.throwable.cause.message == 'must be less than 10000'

        mongoUtil.find(new LogCondition(
                level: 'DEBUG',
                logger: AmountCheckProcessor.class.name)).size() > 30

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
    }

    // testcase 3.6
    def "skip all incorrect data in ItemProcessor by return null"() {
        setup:
        // Copy input file form incorrect data file.
        def inputFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance.csv')
        def incorrectFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance_incorrect-05.csv')
        Files.deleteIfExists(inputFile.toPath())
        inputFile << incorrectFile.readBytes()

        jobDB.deleteAll(['sales_performance_detail'] as String[])

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/exceptionhandling/jobSalesPerfAtSkipSpecificProcError.xml',
                jobName: 'jobSalesPerfAtSkipSpecificProcError',
                jobParameter: 'inputFile=./files/test/input/ch06/exceptionhandling/sales_performance.csv'))
        then:
        exitCode == 0

        def expect = DBUnitUtil.createDataSet({
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                '0001' | 2016 | 11 | 'cust01' | 1000
                '0001' | 2016 | 11 | 'cust02' | 2000
                '0001' | 2016 | 11 | 'cust03' | 3000
                '0001' | 2016 | 11 | 'cust04' | 4000
                '0001' | 2016 | 11 | 'cust05' | 5000
                '0001' | 2016 | 11 | 'cust06' | 6000
                '0001' | 2016 | 11 | 'cust07' | 7000
                '0001' | 2016 | 11 | 'cust08' | 8000
                '0001' | 2016 | 11 | 'cust09' | 9000
                '0001' | 2016 | 11 | 'cust10' | 10000
                '0001' | 2016 | 11 | 'cust11' | 1000
                '0001' | 2016 | 11 | 'cust12' | 2000
                '0001' | 2016 | 11 | 'cust13' | 3000
                '0001' | 2016 | 11 | 'cust14' | 4000
                '0001' | 2016 | 11 | 'cust18' | 8000
                '0001' | 2016 | 11 | 'cust19' | 9000
                '0001' | 2016 | 11 | 'cust20' | 10000
                '0001' | 2016 | 11 | 'cust21' | 1000
                '0001' | 2016 | 11 | 'cust22' | 2000
                '0001' | 2016 | 11 | 'cust23' | 3000
                '0001' | 2016 | 11 | 'cust24' | 4000
                '0001' | 2016 | 11 | 'cust25' | 5000
                '0001' | 2016 | 11 | 'cust26' | 6000
                '0001' | 2016 | 11 | 'cust27' | 7000
                '0001' | 2016 | 11 | 'cust28' | 8000
                '0001' | 2016 | 11 | 'cust29' | 9000
                '0001' | 2016 | 11 | 'cust30' | 10000
            }
        }).getTable('sales_performance_detail')
        def acutal = jobDB.getTable('sales_performance_detail')
        DBUnitUtil.assertEquals(expect, acutal)

        def log = mongoUtil.find(new LogCondition(
                level: 'WARN',
                logger: AmountCheckWithSkipProcessor.class.name,
                message: ~/skipped a record in processor/))
        log.size() == 3
        def log0 = log.get(0)
        log0.throwable._class.name == 'java.lang.ArithmeticException'
        log0.throwable.message == 'must be less than 10000'

        mongoUtil.find(new LogCondition(
                level: 'DEBUG',
                logger: AmountCheckWithSkipProcessor.class.name)).size() == 30

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
    }

    // testcase 3.7
    def "skip all incorrect data in Tasklet by programatic-operation"() {
        setup:
        // Copy input file form incorrect data file.
        def inputFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance.csv')
        def incorrectFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance_incorrect-05.csv')
        Files.deleteIfExists(inputFile.toPath())
        inputFile << incorrectFile.readBytes()

        jobDB.deleteAll(['sales_performance_detail'] as String[])

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/exceptionhandling/jobSalesPerfTaskletWithSkip.xml',
                jobName: 'jobSalesPerfTaskWithSkip',
                jobParameter: 'inputFile=./files/test/input/ch06/exceptionhandling/sales_performance.csv'))
        then:
        exitCode == 0

        def expect = DBUnitUtil.createDataSet({
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                '0001' | 2016 | 11 | 'cust01' | 1000
                '0001' | 2016 | 11 | 'cust02' | 2000
                '0001' | 2016 | 11 | 'cust03' | 3000
                '0001' | 2016 | 11 | 'cust04' | 4000
                '0001' | 2016 | 11 | 'cust05' | 5000
                '0001' | 2016 | 11 | 'cust06' | 6000
                '0001' | 2016 | 11 | 'cust07' | 7000
                '0001' | 2016 | 11 | 'cust08' | 8000
                '0001' | 2016 | 11 | 'cust09' | 9000
                '0001' | 2016 | 11 | 'cust10' | 10000
                '0001' | 2016 | 11 | 'cust11' | 1000
                '0001' | 2016 | 11 | 'cust12' | 2000
                '0001' | 2016 | 11 | 'cust13' | 3000
                '0001' | 2016 | 11 | 'cust14' | 4000
                '0001' | 2016 | 11 | 'cust18' | 8000
                '0001' | 2016 | 11 | 'cust19' | 9000
                '0001' | 2016 | 11 | 'cust20' | 10000
                '0001' | 2016 | 11 | 'cust21' | 1000
                '0001' | 2016 | 11 | 'cust22' | 2000
                '0001' | 2016 | 11 | 'cust23' | 3000
                '0001' | 2016 | 11 | 'cust24' | 4000
                '0001' | 2016 | 11 | 'cust25' | 5000
                '0001' | 2016 | 11 | 'cust26' | 6000
                '0001' | 2016 | 11 | 'cust27' | 7000
                '0001' | 2016 | 11 | 'cust28' | 8000
                '0001' | 2016 | 11 | 'cust29' | 9000
                '0001' | 2016 | 11 | 'cust30' | 10000
            }
        }).getTable('sales_performance_detail')
        def acutal = jobDB.getTable('sales_performance_detail')
        DBUnitUtil.assertEquals(expect, acutal)

        def log = mongoUtil.find(new LogCondition(
                level: 'WARN',
                logger: SalesPerformanceWithSkipTasklet.class.name,
                message: 'exception in tasklet, but process-continue.'))
        log.size() == 3
        def log0 = log.get(0)
        log0.throwable._class.name == 'java.lang.ArithmeticException'
        log0.throwable.message == 'must be less than 10000'

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
    }

    // testcase 3.8
    def "retry-ok incorrect data in ItemProcessor"() {
        setup:
        // Copy input file form incorrect data file.
        def inputFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance.csv')
        def incorrectFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance_incorrect-05.csv')
        Files.deleteIfExists(inputFile.toPath())
        inputFile << incorrectFile.readBytes()

        jobDB.deleteAll(['sales_performance_detail'] as String[])

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/exceptionhandling/jobSalesPerfWithRetry.xml',
                jobName: 'jobSalesPerfWithRetry',
                jobParameter: 'inputFile=./files/test/input/ch06/exceptionhandling/sales_performance.csv adjust-times=2'))
        then:
        exitCode == 0

        def expect = DBUnitUtil.createDataSet({
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                '0001' | 2016 | 11 | 'cust01' | 1000
                '0001' | 2016 | 11 | 'cust02' | 2000
                '0001' | 2016 | 11 | 'cust03' | 3000
                '0001' | 2016 | 11 | 'cust04' | 4000
                '0001' | 2016 | 11 | 'cust05' | 5000
                '0001' | 2016 | 11 | 'cust06' | 6000
                '0001' | 2016 | 11 | 'cust07' | 7000
                '0001' | 2016 | 11 | 'cust08' | 8000
                '0001' | 2016 | 11 | 'cust09' | 9000
                '0001' | 2016 | 11 | 'cust10' | 10000
                '0001' | 2016 | 11 | 'cust11' | 1000
                '0001' | 2016 | 11 | 'cust12' | 2000
                '0001' | 2016 | 11 | 'cust13' | 3000
                '0001' | 2016 | 11 | 'cust14' | 4000
                '0001' | 2016 | 11 | 'cust15' | 5500
                '0001' | 2016 | 11 | 'cust16' | 6600
                '0001' | 2016 | 11 | 'cust17' | 7700
                '0001' | 2016 | 11 | 'cust18' | 8000
                '0001' | 2016 | 11 | 'cust19' | 9000
                '0001' | 2016 | 11 | 'cust20' | 10000
                '0001' | 2016 | 11 | 'cust21' | 1000
                '0001' | 2016 | 11 | 'cust22' | 2000
                '0001' | 2016 | 11 | 'cust23' | 3000
                '0001' | 2016 | 11 | 'cust24' | 4000
                '0001' | 2016 | 11 | 'cust25' | 5000
                '0001' | 2016 | 11 | 'cust26' | 6000
                '0001' | 2016 | 11 | 'cust27' | 7000
                '0001' | 2016 | 11 | 'cust28' | 8000
                '0001' | 2016 | 11 | 'cust29' | 9000
                '0001' | 2016 | 11 | 'cust30' | 10000
            }
        }).getTable('sales_performance_detail')
        def acutal = jobDB.getTable('sales_performance_detail')
        DBUnitUtil.assertEquals(expect, acutal)

        def log = mongoUtil.find(new LogCondition(
                level: 'INFO',
                logger: RetryableAmountCheckProcessor.class.name,
                message: 'execute with retry. [retry-count:2]'))
        log.size() == 3

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
    }

    // testcase 3.9
    def "retry-over incorrect data in ItemProcessor"() {
        setup:
        // Copy input file form incorrect data file.
        def inputFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance.csv')
        def incorrectFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance_incorrect-05.csv')
        Files.deleteIfExists(inputFile.toPath())
        inputFile << incorrectFile.readBytes()

        jobDB.deleteAll(['sales_performance_detail'] as String[])

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/exceptionhandling/jobSalesPerfWithRetry.xml',
                jobName: 'jobSalesPerfWithRetry',
                jobParameter: 'inputFile=./files/test/input/ch06/exceptionhandling/sales_performance.csv adjust-times=10'))
        then:
        exitCode == 255

        def expect = DBUnitUtil.createDataSet({
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                '0001' | 2016 | 11 | 'cust01' | 1000
                '0001' | 2016 | 11 | 'cust02' | 2000
                '0001' | 2016 | 11 | 'cust03' | 3000
                '0001' | 2016 | 11 | 'cust04' | 4000
                '0001' | 2016 | 11 | 'cust05' | 5000
                '0001' | 2016 | 11 | 'cust06' | 6000
                '0001' | 2016 | 11 | 'cust07' | 7000
                '0001' | 2016 | 11 | 'cust08' | 8000
                '0001' | 2016 | 11 | 'cust09' | 9000
                '0001' | 2016 | 11 | 'cust10' | 10000
            }
        }).getTable('sales_performance_detail')
        def acutal = jobDB.getTable('sales_performance_detail')
        DBUnitUtil.assertEquals(expect, acutal)

        def log = mongoUtil.find(new LogCondition(
                level: 'INFO',
                logger: RetryableAmountCheckProcessor.class.name,
                message: 'execute with retry. [retry-count:2]'))
        log.size() == 1

        def errlog = mongoUtil.find(new LogCondition(
                level: 'ERROR',
                logger: AbstractStep.class.name,
                message: ~/Encountered an error executing step/))
        errlog.size() > 0
        def errlog0 = errlog.get(0)

        errlog0.throwable._class.name == 'java.lang.IllegalStateException'
        errlog0.throwable.message == 'check error at processor.'
        errlog0.throwable.cause._class.name == 'java.lang.ArithmeticException'
        errlog0.throwable.cause.message == 'must be less than 10000'

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
    }

    // testcase 3.10
    def "specified skip all incorrect data in ItemReader by skippable-exception-classes, but it is invalid"() {
        setup:
        // Copy input file form incorrect data file.
        def inputFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance.csv')
        def incorrectFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance_incorrect-04.csv')
        Files.deleteIfExists(inputFile.toPath())
        inputFile << incorrectFile.readBytes()

        jobDB.deleteAll(['sales_performance_detail'] as String[])

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/exceptionhandling/jobSalesPerfAtInvalidSkipReadError.xml',
                jobName: 'jobSalesPerfAtInvalidSkipReadError',
                jobParameter: 'inputFile=./files/test/input/ch06/exceptionhandling/sales_performance.csv'))
        then:
        exitCode == 255

        def expect = DBUnitUtil.createDataSet({
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                '0001' | 2016 | 11 | 'cust01' | 1000
                '0001' | 2016 | 11 | 'cust02' | 2000
                '0001' | 2016 | 11 | 'cust03' | 3000
                '0001' | 2016 | 11 | 'cust04' | 4000
                '0001' | 2016 | 11 | 'cust05' | 5000
                '0001' | 2016 | 11 | 'cust06' | 6000
                '0001' | 2016 | 11 | 'cust07' | 7000
                '0001' | 2016 | 11 | 'cust08' | 8000
                '0001' | 2016 | 11 | 'cust09' | 9000
                '0001' | 2016 | 11 | 'cust10' | 10000
            }
        }).getTable('sales_performance_detail')
        def acutal = jobDB.getTable('sales_performance_detail')
        DBUnitUtil.assertEquals(expect, acutal)

        def log = mongoUtil.find(new LogCondition(
                level: 'ERROR',
                logger: AbstractStep.class.name,
                message: ~/Encountered an error executing step/))
        log.size() > 0
        log.get(0).throwable._class.name == 'org.springframework.batch.core.step.skip.NonSkippableReadException'
        log.get(0).throwable.cause._class.name == 'org.springframework.batch.item.file.FlatFileParseException'

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
    }

    // testcase 3.11
    def "retry-policy mismatch, incorrect data in ItemProcessor"() {
        setup:
        // Copy input file form incorrect data file.
        def inputFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance.csv')
        def incorrectFile = new File('./files/test/input/ch06/exceptionhandling/sales_performance_incorrect-05.csv')
        Files.deleteIfExists(inputFile.toPath())
        inputFile << incorrectFile.readBytes()

        jobDB.deleteAll(['sales_performance_detail'] as String[])

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch06/exceptionhandling/jobSalesPerfWithRetryPolicy.xml',
                jobName: 'jobSalesPerfWithRetryPolicy',
                jobParameter: 'inputFile=./files/test/input/ch06/exceptionhandling/sales_performance.csv adjust-times=3'))
        then:
        exitCode == 255

        def expect = DBUnitUtil.createDataSet({
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                '0001' | 2016 | 11 | 'cust01' | 1000
                '0001' | 2016 | 11 | 'cust02' | 2000
                '0001' | 2016 | 11 | 'cust03' | 3000
                '0001' | 2016 | 11 | 'cust04' | 4000
                '0001' | 2016 | 11 | 'cust05' | 5000
                '0001' | 2016 | 11 | 'cust06' | 6000
                '0001' | 2016 | 11 | 'cust07' | 7000
                '0001' | 2016 | 11 | 'cust08' | 8000
                '0001' | 2016 | 11 | 'cust09' | 9000
                '0001' | 2016 | 11 | 'cust10' | 10000
            }
        }).getTable('sales_performance_detail')
        def acutal = jobDB.getTable('sales_performance_detail')
        DBUnitUtil.assertEquals(expect, acutal)

        def log = mongoUtil.find(new LogCondition(
                level: 'INFO',
                logger: RetryableAmountCheckProcessor.class.name,
                message: 'execute with retry. [retry-count:2]'))
        log.size() == 0

        def errlog = mongoUtil.find(new LogCondition(
                level: 'ERROR',
                logger: AbstractStep.class.name,
                message: ~/Encountered an error executing step/))
        errlog.size() > 0
        def errlog0 = errlog.get(0)

        errlog0.throwable._class.name == 'java.lang.IllegalStateException'
        errlog0.throwable.message == 'check error at processor.'
        errlog0.throwable.cause._class.name == 'java.lang.ArithmeticException'
        errlog0.throwable.cause.message == 'must be less than 10000'

        cleanup:
        Files.deleteIfExists(inputFile.toPath())
    }
}
