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
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.listener.AllProcessListener
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.listener.BeforeStepException
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.listener.LoggingReader
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.listener.LoggingWriter
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.listener.annotation.AnnotationAmountCheckProcessor
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.listener.annotation.AnnotationSalesPlanDetailRegisterTasklet
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
 * Function test of listener.
 *
 * @since 5.0.0
 */
@Slf4j
@Narrative("""
A list of test cases is shown below.
1. Testing listeners in chunk-oriented jobs
2. Testing the listener in a tasklet-oriented job
3. Testing listeners in chunk-oriented jobs with Annotation
4. Testing the listener in a tasklet-oriented job with Annotation
5. Testing listeners mixed with annotation in chunk-oriented jobs
6. Testing the listener mixed with annotation in a tasklet-oriented job
7. Testing exception occurrs on listener in chunk-oriented job
8. Testing Job abort on listener in chunk-oriented job and tasklet-oriented job

Notice that the number of reads of the normal ItemReader contains the number of termination marks (null).
""")

class ListenerSpec extends Specification {

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

    // Testcase 1, test no.1
    @Unroll
    def "All listeners are processed (normal chunk oriented job) within #scope scope"() {
        setup:
        jobDB.deleteAll(["sales_plan_detail"] as String[])
        def inputFileName = "./files/test/input/ch04/listener/sales_plan_detail_correct.csv"

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/listener/chunkJobWithListener.xml',
                jobName: "chunkJobWithListenerWithin${scope}Scope",
                jobParameter: "inputFile=${inputFileName}"
        ))
        mongoUtil.waitForOutputLog(new LogCondition(
                message: ~/Closing org.springframework.context.support.ClassPathXmlApplicationContext/
        ))

        then:
        exitValue == 0
        mongoUtil.find(new LogCondition(
                message: "Common Logging: job started. [chunkJobWithListenerWithin${scope}Scope]",
                logger: AllProcessListener.class.name)).size() == jobListener
        mongoUtil.find(new LogCondition(
                message: "Common Logging: job finished. [chunkJobWithListenerWithin${scope}Scope]",
                logger: AllProcessListener.class.name)).size() == jobListener

        mongoUtil.find(new LogCondition(
                message: "Common Logging: step started. [chunkJobWithListenerWithin${scope}Scope.step01]",
                logger: AllProcessListener.class.name)).size() == stepListener
        mongoUtil.find(new LogCondition(
                message: "Common Logging: step finished. [chunkJobWithListenerWithin${scope}Scope.step01]",
                logger: AllProcessListener.class.name)).size() == stepListener

        mongoUtil.find(new LogCondition(message: 'Common Logging: before chunk.',
                logger: AllProcessListener.class.name)).size() == chunkListener
        mongoUtil.find(new LogCondition(message: 'Common Logging: after chunk.',
                logger: AllProcessListener.class.name)).size() == chunkListener
        mongoUtil.find(new LogCondition(message: 'Common Logging: after chunk error.',
                logger: AllProcessListener.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: 'Common Logging: before read.',
                logger: AllProcessListener.class.name)).size() == (itemReadListener == 0 ? 0 : itemReadListener + 1)  // Addition by end mark
        mongoUtil.find(new LogCondition(message: ~/Common Logging: after read./,
                logger: AllProcessListener.class.name)).size() == itemReadListener
        mongoUtil.find(new LogCondition(message: 'Common Logging: on read error.',
                logger: AllProcessListener.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: ~/Common Logging: before process./,
                logger: AllProcessListener.class.name)).size() == itemProcessListener
        mongoUtil.find(new LogCondition(message: ~/Common Logging: after process./,
                logger: AllProcessListener.class.name)).size() == itemProcessListener
        mongoUtil.find(new LogCondition(message: 'Common Logging: on process error.',
                logger: AllProcessListener.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: 'Common Logging: before write. [Item size :5]',
                logger: AllProcessListener.class.name)).size() == ItemWriterListener
        mongoUtil.find(new LogCondition(message: 'Common Logging: after write. [Item size :5]',
                logger: AllProcessListener.class.name)).size() == ItemWriterListener
        mongoUtil.find(new LogCondition(message: 'Common Logging: on write error.',
                logger: AllProcessListener.class.name)).size() == 0

        where:
        scope     || jobListener | stepListener | chunkListener | itemReadListener | itemProcessListener | ItemWriterListener
        "Job"     || 1           | 0            | 0             | 0                | 0                   | 0
        "Step"    || 0           | 1            | 1             | 5                | 5                   | 1
        "Tasklet" || 0           | 1            | 1             | 5                | 5                   | 1
        "Chunk"   || 0           | 1            | 1             | 5                | 5                   | 1
        "All"     || 1           | 1            | 1             | 5                | 5                   | 1
    }

    // Testcase 1, test no.2
    @Unroll
    def "All listeners are processed (abnormal chunk oriented job: error on #process) within #scope scope"() {
        setup:
        jobDB.deleteAll(["sales_plan_detail"] as String[])
        def inputFileName = "./files/test/input/ch04/listener/sales_plan_detail_incorrect_${process}.csv"

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/listener/chunkJobWithListener.xml',
                jobName: "chunkJobWithListenerWithin${scope}Scope",
                jobParameter: "inputFile=${inputFileName}"
        ))
        mongoUtil.waitForOutputLog(new LogCondition(
                message: ~/Closing org.springframework.context.support.ClassPathXmlApplicationContext/
        ))

        then:
        exitValue == 255
        mongoUtil.find(new LogCondition(
                message: "Common Logging: job started. [chunkJobWithListenerWithin${scope}Scope]",
                logger: AllProcessListener.class.name)).size() == jobListener
        mongoUtil.find(new LogCondition(
                message: "Common Logging: job finished. [chunkJobWithListenerWithin${scope}Scope]",
                logger: AllProcessListener.class.name)).size() == jobListener

        mongoUtil.find(new LogCondition(
                message: "Common Logging: step started. [chunkJobWithListenerWithin${scope}Scope.step01]",
                logger: AllProcessListener.class.name)).size() == stepListener
        mongoUtil.find(new LogCondition(
                message: "Common Logging: step finished. [chunkJobWithListenerWithin${scope}Scope.step01]",
                logger: AllProcessListener.class.name)).size() == stepListener

        mongoUtil.find(new LogCondition(message: 'Common Logging: before chunk.',
                logger: AllProcessListener.class.name)).size() == beforeChunk
        mongoUtil.find(new LogCondition(message: 'Common Logging: after chunk.',
                logger: AllProcessListener.class.name)).size() == afterChunk
        mongoUtil.find(new LogCondition(message: 'Common Logging: after chunk error.',
                logger: AllProcessListener.class.name)).size() == afterChunkError

        mongoUtil.find(new LogCondition(message: 'Common Logging: before read.',
                logger: AllProcessListener.class.name)).size() == beforeRead
        mongoUtil.find(new LogCondition(message: ~/Common Logging: after read./,
                logger: AllProcessListener.class.name)).size() == afterRead
        mongoUtil.find(new LogCondition(message: 'Common Logging: on read error.',
                logger: AllProcessListener.class.name)).size() == onReadError

        mongoUtil.find(new LogCondition(message: ~/Common Logging: before process./,
                logger: AllProcessListener.class.name)).size() == beforeProcess
        mongoUtil.find(new LogCondition(message: ~/Common Logging: after process./,
                logger: AllProcessListener.class.name)).size() == afterProcess
        mongoUtil.find(new LogCondition(message: 'Common Logging: on process error.',
                logger: AllProcessListener.class.name)).size() == onProcessError

        mongoUtil.find(new LogCondition(message: 'Common Logging: before write. [Item size :5]',
                logger: AllProcessListener.class.name)).size() == beforeWrite
        mongoUtil.find(new LogCondition(message: 'Common Logging: after write. [Item size :5]',
                logger: AllProcessListener.class.name)).size() == afterWrite
        mongoUtil.find(new LogCondition(message: 'Common Logging: on write error.',
                logger: AllProcessListener.class.name)).size() == onWriteError

        where:
        scope     | process     || jobListener | stepListener | beforeChunk | afterChunk | afterChunkError | beforeRead | afterRead | onReadError | beforeProcess | afterProcess | onProcessError | beforeWrite | afterWrite | onWriteError
        "Job"     | "reader"    || 1           | 0            | 0           | 0          | 0               | 0          | 0         | 0           | 0             | 0            | 0              | 0           | 0          | 0
        "Job"     | "processor" || 1           | 0            | 0           | 0          | 0               | 0          | 0         | 0           | 0             | 0            | 0              | 0           | 0          | 0
        "Job"     | "writer"    || 1           | 0            | 0           | 0          | 0               | 0          | 0         | 0           | 0             | 0            | 0              | 0           | 0          | 0
        "Step"    | "reader"    || 0           | 1            | 1           | 0          | 1               | 4          | 3         | 1           | 0             | 0            | 0              | 0           | 0          | 0
        "Step"    | "processor" || 0           | 1            | 1           | 0          | 1               | 6          | 5         | 0           | 4             | 3            | 1              | 0           | 0          | 0
        "Step"    | "writer"    || 0           | 1            | 1           | 0          | 1               | 6          | 5         | 0           | 5             | 5            | 0              | 1           | 0          | 1
        "Tasklet" | "reader"    || 0           | 1            | 1           | 0          | 1               | 4          | 3         | 1           | 0             | 0            | 0              | 0           | 0          | 0
        "Tasklet" | "processor" || 0           | 1            | 1           | 0          | 1               | 6          | 5         | 0           | 4             | 3            | 1              | 0           | 0          | 0
        "Tasklet" | "writer"    || 0           | 1            | 1           | 0          | 1               | 6          | 5         | 0           | 5             | 5            | 0              | 1           | 0          | 1
        "Chunk"   | "reader"    || 0           | 1            | 1           | 0          | 1               | 4          | 3         | 1           | 0             | 0            | 0              | 0           | 0          | 0
        "Chunk"   | "processor" || 0           | 1            | 1           | 0          | 1               | 6          | 5         | 0           | 4             | 3            | 1              | 0           | 0          | 0
        "Chunk"   | "writer"    || 0           | 1            | 1           | 0          | 1               | 6          | 5         | 0           | 5             | 5            | 0              | 1           | 0          | 1
        "All"     | "reader"    || 1           | 1            | 1           | 0          | 1               | 4          | 3         | 1           | 0             | 0            | 0              | 0           | 0          | 0
        "All"     | "processor" || 1           | 1            | 1           | 0          | 1               | 6          | 5         | 0           | 4             | 3            | 1              | 0           | 0          | 0
        "All"     | "writer"    || 1           | 1            | 1           | 0          | 1               | 6          | 5         | 0           | 5             | 5            | 0              | 1           | 0          | 1
    }

    // Testcase 2, test no.1
    @Unroll
    def "Only specific listeners are processed (normal tasklet oriented  job) within #scope scope"() {
        setup:
        jobDB.deleteAll(["sales_plan_detail"] as String[])
        def inputFileName = "./files/test/input/ch04/listener/sales_plan_detail_correct.csv"

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/listener/taskletJobWithListener.xml',
                jobName: "taskletJobWithListenerWithin${scope}Scope",
                jobParameter: "inputFile=${inputFileName}"
        ))
        mongoUtil.waitForOutputLog(new LogCondition(
                message: ~/Closing org.springframework.context.support.ClassPathXmlApplicationContext/
        ))

        then:
        exitValue == 0
        mongoUtil.find(new LogCondition(
                message: "Common Logging: job started. [taskletJobWithListenerWithin${scope}Scope]",
                logger: AllProcessListener.class.name)).size() == jobListener
        mongoUtil.find(new LogCondition(
                message: "Common Logging: job finished. [taskletJobWithListenerWithin${scope}Scope]",
                logger: AllProcessListener.class.name)).size() == jobListener

        mongoUtil.find(new LogCondition(
                message: "Common Logging: step started. [taskletJobWithListenerWithin${scope}Scope.step01]",
                logger: AllProcessListener.class.name)).size() == stepListener
        mongoUtil.find(new LogCondition(
                message: "Common Logging: step finished. [taskletJobWithListenerWithin${scope}Scope.step01]",
                logger: AllProcessListener.class.name)).size() == stepListener

        mongoUtil.find(new LogCondition(message: 'Common Logging: before chunk.',
                logger: AllProcessListener.class.name)).size() == chunkListener
        mongoUtil.find(new LogCondition(message: 'Common Logging: after chunk.',
                logger: AllProcessListener.class.name)).size() == chunkListener
        mongoUtil.find(new LogCondition(message: 'Common Logging: after chunk error.',
                logger: AllProcessListener.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: 'Common Logging: before read.',
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: ~/Common Logging: after read./,
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Common Logging: on read error.',
                logger: AllProcessListener.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: ~/Common Logging: before process./,
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: ~/Common Logging: after process./,
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Common Logging: on process error.',
                logger: AllProcessListener.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: 'Common Logging: before write. [Item size :5]',
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Common Logging: after write. [Item size :5]',
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Common Logging: on write error.',
                logger: AllProcessListener.class.name)).size() == 0

        where:
        scope     || jobListener | stepListener | chunkListener
        "Job"     || 1           | 0            | 0
        "Step"    || 0           | 1            | 1
        "Tasklet" || 0           | 1            | 1
        "All"     || 1           | 1            | 1
    }

    // Testcase 2, test no.2
    @Unroll
    def "Only specific listeners are processed (abnormal tasklet oriented job: error on #process) within #scope scope"() {
        setup:
        jobDB.deleteAll(["sales_plan_detail"] as String[])
        def inputFileName = "./files/test/input/ch04/listener/sales_plan_detail_incorrect_${process}.csv"

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/listener/taskletJobWithListener.xml',
                jobName: "taskletJobWithListenerWithin${scope}Scope",
                jobParameter: "inputFile=${inputFileName}"
        ))
        mongoUtil.waitForOutputLog(new LogCondition(
                message: ~/Closing org.springframework.context.support.ClassPathXmlApplicationContext/
        ))

        then:
        exitValue == 255
        mongoUtil.find(new LogCondition(
                message: "Common Logging: job started. [taskletJobWithListenerWithin${scope}Scope]",
                logger: AllProcessListener.class.name)).size() == jobListener
        mongoUtil.find(new LogCondition(
                message: "Common Logging: job finished. [taskletJobWithListenerWithin${scope}Scope]",
                logger: AllProcessListener.class.name)).size() == jobListener

        mongoUtil.find(new LogCondition(
                message: "Common Logging: step started. [taskletJobWithListenerWithin${scope}Scope.step01]",
                logger: AllProcessListener.class.name)).size() == stepListener
        mongoUtil.find(new LogCondition(
                message: "Common Logging: step finished. [taskletJobWithListenerWithin${scope}Scope.step01]",
                logger: AllProcessListener.class.name)).size() == stepListener

        mongoUtil.find(new LogCondition(message: 'Common Logging: before chunk.',
                logger: AllProcessListener.class.name)).size() == beforeChunk
        mongoUtil.find(new LogCondition(message: 'Common Logging: after chunk.',
                logger: AllProcessListener.class.name)).size() == afterChunk
        mongoUtil.find(new LogCondition(message: 'Common Logging: after chunk error.',
                logger: AllProcessListener.class.name)).size() == afterChunkError

        mongoUtil.find(new LogCondition(message: 'Common Logging: before read.',
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: ~/Common Logging: after read./,
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Common Logging: on read error.',
                logger: AllProcessListener.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: ~/Common Logging: before process./,
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: ~/Common Logging: after process./,
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Common Logging: on process error.',
                logger: AllProcessListener.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: 'Common Logging: before write. [Item size :5]',
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Common Logging: after write. [Item size :5]',
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Common Logging: on write error.',
                logger: AllProcessListener.class.name)).size() == 0

        where:
        scope     | process     || jobListener | stepListener | beforeChunk | afterChunk | afterChunkError
        "Job"     | "reader"    || 1           | 0            | 0           | 0          | 0
        "Job"     | "processor" || 1           | 0            | 0           | 0          | 0
        "Job"     | "writer"    || 1           | 0            | 0           | 0          | 0
        "Step"    | "reader"    || 0           | 1            | 1           | 0          | 1
        "Step"    | "processor" || 0           | 1            | 1           | 0          | 1
        "Step"    | "writer"    || 0           | 1            | 1           | 0          | 1
        "Tasklet" | "reader"    || 0           | 1            | 1           | 0          | 1
        "Tasklet" | "processor" || 0           | 1            | 1           | 0          | 1
        "Tasklet" | "writer"    || 0           | 1            | 1           | 0          | 1
        "All"     | "reader"    || 1           | 1            | 1           | 0          | 1
        "All"     | "processor" || 1           | 1            | 1           | 0          | 1
        "All"     | "writer"    || 1           | 1            | 1           | 0          | 1
    }

    // Testcase 3, test no.1
    @Unroll
    def "All listeners are processed (normal chunk oriented job) with Annotation within #scope scope"() {
        setup:
        jobDB.deleteAll(["sales_plan_detail"] as String[])
        def inputFileName = "./files/test/input/ch04/listener/sales_plan_detail_correct.csv"

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/listener/chunkJobWithListenerAnnotation.xml',
                jobName: "chunkJobWithListenerAnnotationWithin${scope}Scope",
                jobParameter: "inputFile=${inputFileName}"
        ))
        mongoUtil.waitForOutputLog(new LogCondition(
                message: ~/Closing org.springframework.context.support.ClassPathXmlApplicationContext/
        ))

        then:
        exitValue == 0
        ( mongoUtil.find(new LogCondition(
                message: "Annotation Logging: job started. [chunkJobWithListenerAnnotationWithin${scope}Scope]",
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == jobListener
        ( mongoUtil.find(new LogCondition(
                message: "Annotation Logging: job finished. [chunkJobWithListenerAnnotationWithin${scope}Scope]",
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == jobListener

        ( mongoUtil.find(new LogCondition(
                message: "Annotation Logging: step started. [chunkJobWithListenerAnnotationWithin${scope}Scope.step01]",
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == stepListener
        ( mongoUtil.find(new LogCondition(
                message: "Annotation Logging: step finished. [chunkJobWithListenerAnnotationWithin${scope}Scope.step01]",
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == stepListener

        ( mongoUtil.find(new LogCondition(message: 'Annotation Logging: before chunk.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == chunkListener
        ( mongoUtil.find(new LogCondition(message: 'Annotation Logging: after chunk.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == chunkListener
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: after chunk error.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() == 0

        ( mongoUtil.find(new LogCondition(message: 'Annotation Logging: before read.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == beforeReade
        ( mongoUtil.find(new LogCondition(message: ~/Annotation Logging: after read./,
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == afterRead
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: on read error.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() == 0

        ( mongoUtil.find(new LogCondition(message: ~/Annotation Logging: before process./,
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == itemProcessListener
        ( mongoUtil.find(new LogCondition(message: ~/Annotation Logging: after process./,
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == itemProcessListener
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: on process error.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() == 0

        ( mongoUtil.find(new LogCondition(message: 'Annotation Logging: before write. [Item size :5]',
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == ItemWriterListener
        ( mongoUtil.find(new LogCondition(message: 'Annotation Logging: after write. [Item size :5]',
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == ItemWriterListener
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: on write error.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() == 0

        where:
        scope      || jobListener | stepListener | chunkListener | beforeReade | afterRead | itemProcessListener | ItemWriterListener
        "Job"      || true        | true         | true          | true        | true      | true                | true
        "Step"     || false       | true         | true          | true        | true      | true                | true
        "Tasklet"  || false       | true         | true          | true        | true      | true                | true
        "Chunk"    || false       | true         | true          | true        | true      | true                | true
        "All"      || true        | true         | true          | true        | true      | true                | true
        "Implicit" || false       | true         | true          | true        | true      | true                | true
    }

    // Testcase 3, test no.2
    @Unroll
    def "All listeners are processed (abnormal chunk oriented job: error on #process) with Annotation within #scope scope"() {
        setup:
        jobDB.deleteAll(["sales_plan_detail"] as String[])
        def inputFileName = "./files/test/input/ch04/listener/sales_plan_detail_incorrect_${process}.csv"

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/listener/chunkJobWithListenerAnnotation.xml',
                jobName: "chunkJobWithListenerAnnotationWithin${scope}Scope",
                jobParameter: "inputFile=${inputFileName}"
        ))
        mongoUtil.waitForOutputLog(new LogCondition(
                message: ~/Closing org.springframework.context.support.ClassPathXmlApplicationContext/
        ))

        then:
        exitValue == 255
        ( mongoUtil.find(new LogCondition(
                message: "Annotation Logging: job started. [chunkJobWithListenerAnnotationWithin${scope}Scope]",
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == jobListener
        ( mongoUtil.find(new LogCondition(
                message: "Annotation Logging: job finished. [chunkJobWithListenerAnnotationWithin${scope}Scope]",
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == jobListener

        ( mongoUtil.find(new LogCondition(
                message: "Annotation Logging: step started. [chunkJobWithListenerAnnotationWithin${scope}Scope.step01]",
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == stepListener
        ( mongoUtil.find(new LogCondition(
                message: "Annotation Logging: step finished. [chunkJobWithListenerAnnotationWithin${scope}Scope.step01]",
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == stepListener

        ( mongoUtil.find(new LogCondition(message: 'Annotation Logging: before chunk.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == beforeChunk
        ( mongoUtil.find(new LogCondition(message: 'Annotation Logging: after chunk.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == afterChunk
        ( mongoUtil.find(new LogCondition(message: 'Annotation Logging: after chunk error.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == afterChunkError

        ( mongoUtil.find(new LogCondition(message: 'Annotation Logging: before read.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == beforeRead
        ( mongoUtil.find(new LogCondition(message: ~/Annotation Logging: after read./,
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == afterRead
        ( mongoUtil.find(new LogCondition(message: 'Annotation Logging: on read error.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == onReadError

        ( mongoUtil.find(new LogCondition(message: ~/Annotation Logging: before process./,
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == beforeProcess
        ( mongoUtil.find(new LogCondition(message: ~/Annotation Logging: after process./,
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == afterProcess
        ( mongoUtil.find(new LogCondition(message: 'Annotation Logging: on process error.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == onProcessError

        ( mongoUtil.find(new LogCondition(message: 'Annotation Logging: before write. [Item size :5]',
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == beforeWrite
        ( mongoUtil.find(new LogCondition(message: 'Annotation Logging: after write. [Item size :5]',
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == afterWrite
        ( mongoUtil.find(new LogCondition(message: 'Annotation Logging: on write error.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() !=0 ) == onWriteError

        where:
        scope      | process     || jobListener | stepListener | beforeChunk | afterChunk | afterChunkError | beforeRead | afterRead | onReadError | beforeProcess | afterProcess | onProcessError | beforeWrite | afterWrite | onWriteError
        "Job"      | "reader"    || true        | true         | true        | false      | true            | true       | true      | true        | false         | false        | false          | false       | false          | false
        "Job"      | "processor" || true        | true         | true        | false      | true            | true       | true      | false       | true          | true         | true           | false       | false          | false
        "Job"      | "writer"    || true        | true         | true        | false      | true            | true       | true      | false       | true          | true         | false          | true        | false          | true
        "Step"     | "reader"    || false       | true         | true        | false      | true            | true       | true      | true        | false         | false        | false          | false       | false          | false
        "Step"     | "processor" || false       | true         | true        | false      | true            | true       | true      | false       | true          | true         | true           | false       | false          | false
        "Step"     | "writer"    || false       | true         | true        | false      | true            | true       | true      | false       | true          | true         | false          | true        | false          | true
        "Tasklet"  | "reader"    || false       | true         | true        | false      | true            | true       | true      | true        | false         | false        | false          | false       | false          | false
        "Tasklet"  | "processor" || false       | true         | true        | false      | true            | true       | true      | false       | true          | true         | true           | false       | false          | false
        "Tasklet"  | "writer"    || false       | true         | true        | false      | true            | true       | true      | false       | true          | true         | false          | true        | false          | true
        "Chunk"    | "reader"    || false       | true         | true        | false      | true            | true       | true      | true        | false         | false        | false          | false       | false          | false
        "Chunk"    | "processor" || false       | true         | true        | false      | true            | true       | true      | false       | true          | true         | true           | false       | false          | false
        "Chunk"    | "writer"    || false       | true         | true        | false      | true            | true       | true      | false       | true          | true         | false          | true        | false          | true
        "All"      | "reader"    || true        | true         | true        | false      | true            | true       | true      | true        | false         | false        | false          | false       | false          | false
        "All"      | "processor" || true        | true         | true        | false      | true            | true       | true      | false       | true          | true         | true           | false       | false          | false
        "All"      | "writer"    || true        | true         | true        | false      | true            | true       | true      | false       | true          | true         | false          | true        | false          | true
        "Implicit" | "reader"    || false       | true         | true        | false      | true            | true       | true      | true        | false         | false        | false          | false       | false          | false
        "Implicit" | "processor" || false       | true         | true        | false      | true            | true       | true      | false       | true          | true         | true           | false       | false          | false
        "Implicit" | "writer"    || false       | true         | true        | false      | true            | true       | true      | false       | true          | true         | false          | true        | false          | true
    }

    // Testcase 4, test no.1
    @Unroll
    def "Only specific listeners are processed (normal tasklet oriented  job) with Annotation within #scope scope"() {
        setup:
        jobDB.deleteAll(["sales_plan_detail"] as String[])
        def inputFileName = "./files/test/input/ch04/listener/sales_plan_detail_correct.csv"

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/listener/taskletJobWithListenerAnnotation.xml',
                jobName: "taskletJobWithListenerAnnotationWithin${scope}Scope",
                jobParameter: "inputFile=${inputFileName}"
        ))
        mongoUtil.waitForOutputLog(new LogCondition(
                message: ~/Closing org.springframework.context.support.ClassPathXmlApplicationContext/
        ))

        then:
        exitValue == 0
        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: job started. [taskletJobWithListenerAnnotationWithin${scope}Scope]",
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == jobListener
        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: job finished. [taskletJobWithListenerAnnotationWithin${scope}Scope]",
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == jobListener

        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: step started. [taskletJobWithListenerAnnotationWithin${scope}Scope.step01]",
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == stepListener
        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: step finished. [taskletJobWithListenerAnnotationWithin${scope}Scope.step01]",
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == stepListener

        mongoUtil.find(new LogCondition(message: 'Annotation Logging: before chunk.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == chunkListener
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: after chunk.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == chunkListener
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: after chunk error.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: 'Annotation Logging: before read.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: ~/Annotation Logging: after read./,
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: on read error.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: ~/Annotation Logging: before process./,
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: ~/Annotation Logging: after process./,
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: on process error.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: 'Annotation Logging: before write. [Item size :5]',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: after write. [Item size :5]',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: on write error.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0

        where:
        scope      || jobListener | stepListener | chunkListener
        "Job"      || 1           | 0            | 0
        "Step"     || 0           | 1            | 1
        "Tasklet"  || 0           | 1            | 1
        "All"      || 1           | 1            | 1
        "Implicit" || 0           | 0            | 0
    }

    // Testcase 4, test no.2
    @Unroll
    def "Only specific listeners are processed (abnormal tasklet oriented job: error on #process) with Annotation within #scope scope"() {
        setup:
        jobDB.deleteAll(["sales_plan_detail"] as String[])
        def inputFileName = "./files/test/input/ch04/listener/sales_plan_detail_incorrect_${process}.csv"

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/listener/taskletJobWithListenerAnnotation.xml',
                jobName: "taskletJobWithListenerAnnotationWithin${scope}Scope",
                jobParameter: "inputFile=${inputFileName}"
        ))
        mongoUtil.waitForOutputLog(new LogCondition(
                message: ~/Closing org.springframework.context.support.ClassPathXmlApplicationContext/
        ))

        then:
        exitValue == 255
        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: job started. [taskletJobWithListenerAnnotationWithin${scope}Scope]",
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == jobListener
        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: job finished. [taskletJobWithListenerAnnotationWithin${scope}Scope]",
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == jobListener

        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: step started. [taskletJobWithListenerAnnotationWithin${scope}Scope.step01]",
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == stepListener
        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: step finished. [taskletJobWithListenerAnnotationWithin${scope}Scope.step01]",
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == stepListener

        mongoUtil.find(new LogCondition(message: 'Annotation Logging: before chunk.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == beforeChunk
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: after chunk.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == afterChunk
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: after chunk error.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == afterChunkError

        mongoUtil.find(new LogCondition(message: 'Annotation Logging: before read.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: ~/Annotation Logging: after read./,
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: on read error.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: ~/Annotation Logging: before process./,
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: ~/Annotation Logging: after process./,
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: on process error.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: 'Annotation Logging: before write. [Item size :5]',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: after write. [Item size :5]',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: on write error.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0

        where:
        scope      | process     || jobListener | stepListener | beforeChunk | afterChunk | afterChunkError
        "Job"      | "reader"    || 1           | 0            | 0           | 0          | 0
        "Job"      | "processor" || 1           | 0            | 0           | 0          | 0
        "Job"      | "writer"    || 1           | 0            | 0           | 0          | 0
        "Step"     | "reader"    || 0           | 1            | 1           | 0          | 1
        "Step"     | "processor" || 0           | 1            | 1           | 0          | 1
        "Step"     | "writer"    || 0           | 1            | 1           | 0          | 1
        "Tasklet"  | "reader"    || 0           | 1            | 1           | 0          | 1
        "Tasklet"  | "processor" || 0           | 1            | 1           | 0          | 1
        "Tasklet"  | "writer"    || 0           | 1            | 1           | 0          | 1
        "All"      | "reader"    || 1           | 1            | 1           | 0          | 1
        "All"      | "processor" || 1           | 1            | 1           | 0          | 1
        "All"      | "writer"    || 1           | 1            | 1           | 0          | 1
        "Implicit" | "reader"    || 0           | 0            | 0           | 0          | 0
        "Implicit" | "processor" || 0           | 0            | 0           | 0          | 0
        "Implicit" | "writer"    || 0           | 0            | 0           | 0          | 0
    }

    // Testcase 5, test no.1
    @Unroll
    def "All mixed listeners are processed (normal chunk oriented job) within #scope scope"() {
        setup:
        jobDB.deleteAll(["sales_plan_detail"] as String[])
        def inputFileName = "./files/test/input/ch04/listener/sales_plan_detail_correct.csv"

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/listener/chunkJobWithMixedListener.xml',
                jobName: "chunkJobWithMixedListenerWithin${scope}Scope",
                jobParameter: "inputFile=${inputFileName}"
        ))
        mongoUtil.waitForOutputLog(new LogCondition(
                message: ~/Closing org.springframework.context.support.ClassPathXmlApplicationContext/
        ))

        then:
        exitValue == 0
        // check interface implements listener
        mongoUtil.find(new LogCondition(
                message: "Common Logging: job started. [chunkJobWithMixedListenerWithin${scope}Scope]",
                logger: AllProcessListener.class.name)).size() == jobI
        mongoUtil.find(new LogCondition(
                message: "Common Logging: job finished. [chunkJobWithMixedListenerWithin${scope}Scope]",
                logger: AllProcessListener.class.name)).size() == jobI

        mongoUtil.find(new LogCondition(
                message: "Common Logging: step started. [chunkJobWithMixedListenerWithin${scope}Scope.step01]",
                logger: AllProcessListener.class.name)).size() == stepI
        mongoUtil.find(new LogCondition(
                message: "Common Logging: step finished. [chunkJobWithMixedListenerWithin${scope}Scope.step01]",
                logger: AllProcessListener.class.name)).size() == stepI

        mongoUtil.find(new LogCondition(message: 'Common Logging: before chunk.',
                logger: AllProcessListener.class.name)).size() == chunkL
        mongoUtil.find(new LogCondition(message: 'Common Logging: after chunk.',
                logger: AllProcessListener.class.name)).size() == chunkL
        mongoUtil.find(new LogCondition(message: 'Common Logging: after chunk error.',
                logger: AllProcessListener.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: 'Common Logging: before read.',
                logger: AllProcessListener.class.name)).size() == beforeReadI
        mongoUtil.find(new LogCondition(message: ~/Common Logging: after read./,
                logger: AllProcessListener.class.name)).size() == afterReadI
        mongoUtil.find(new LogCondition(message: 'Common Logging: on read error.',
                logger: AllProcessListener.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: ~/Common Logging: before process./,
                logger: AllProcessListener.class.name)).size() == processI
        mongoUtil.find(new LogCondition(message: ~/Common Logging: after process./,
                logger: AllProcessListener.class.name)).size() == processI
        mongoUtil.find(new LogCondition(message: 'Common Logging: on process error.',
                logger: AllProcessListener.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: 'Common Logging: before write. [Item size :5]',
                logger: AllProcessListener.class.name)).size() == writerI
        mongoUtil.find(new LogCondition(message: 'Common Logging: after write. [Item size :5]',
                logger: AllProcessListener.class.name)).size() == writerI
        mongoUtil.find(new LogCondition(message: 'Common Logging: on write error.',
                logger: AllProcessListener.class.name)).size() == 0

        // check annotation base listener
        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: job started. [chunkJobWithMixedListenerWithin${scope}Scope]",
                logger: AnnotationAmountCheckProcessor.class.name)).size() == jobA
        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: job finished. [chunkJobWithMixedListenerWithin${scope}Scope]",
                logger: AnnotationAmountCheckProcessor.class.name)).size() == jobA

        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: step started. [chunkJobWithMixedListenerWithin${scope}Scope.step01]",
                logger: AnnotationAmountCheckProcessor.class.name)).size() == stepA
        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: step finished. [chunkJobWithMixedListenerWithin${scope}Scope.step01]",
                logger: AnnotationAmountCheckProcessor.class.name)).size() == stepA

        mongoUtil.find(new LogCondition(message: 'Annotation Logging: before chunk.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() == chunkA
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: after chunk.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() == chunkA
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: after chunk error.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: 'Annotation Logging: before read.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() == beforeReadA
        mongoUtil.find(new LogCondition(message: ~/Annotation Logging: after read./,
                logger: AnnotationAmountCheckProcessor.class.name)).size() == afterReadA
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: on read error.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: ~/Annotation Logging: before process./,
                logger: AnnotationAmountCheckProcessor.class.name)).size() == processA
        mongoUtil.find(new LogCondition(message: ~/Annotation Logging: after process./,
                logger: AnnotationAmountCheckProcessor.class.name)).size() == processA
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: on process error.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: 'Annotation Logging: before write. [Item size :5]',
                logger: AnnotationAmountCheckProcessor.class.name)).size() == writerA
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: after write. [Item size :5]',
                logger: AnnotationAmountCheckProcessor.class.name)).size() == writerA
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: on write error.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() == 0

        // xxxI is interface implement listeners, xxxA is annotation base listeners.
        where:
        scope     || jobI | stepI | chunkL | beforeReadI | afterReadI | processI | writerI | jobA | stepA | chunkA | beforeReadA | afterReadA | processA | writerA
        "Job"     || 1    | 0     | 0      | 0           | 0          | 0        | 0       | 0    | 1     | 1      | 6            | 5          | 5       | 1
        "Step"    || 0    | 1     | 1      | 6           | 5          | 5        | 1       | 0    | 1     | 1      | 6            | 5          | 5       | 1
        "Tasklet" || 0    | 1     | 1      | 6           | 5          | 5        | 1       | 0    | 1     | 1      | 6            | 5          | 5       | 1
        "Chunk"   || 0    | 1     | 1      | 6           | 5          | 5        | 1       | 0    | 1     | 1      | 6            | 5          | 5       | 1
        "All"     || 1    | 1     | 1      | 6           | 5          | 5        | 1       | 0    | 1     | 1      | 6            | 5          | 5       | 1
    }

    // Testcase 5, test no.2
    @Unroll
    def "All mixed listeners are processed (abnormal chunk oriented job: error on #process) within #scope scope"() {
        setup:
        jobDB.deleteAll(["sales_plan_detail"] as String[])
        def inputFileName = "./files/test/input/ch04/listener/sales_plan_detail_incorrect_${process}.csv"

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/listener/chunkJobWithMixedListener.xml',
                jobName: "chunkJobWithMixedListenerWithin${scope}Scope",
                jobParameter: "inputFile=${inputFileName}"
        ))
        mongoUtil.waitForOutputLog(new LogCondition(
                message: ~/Closing org.springframework.context.support.ClassPathXmlApplicationContext/
        ))

        then:
        exitValue == 255
        // check interface implements listener
        mongoUtil.find(new LogCondition(
                message: "Common Logging: job started. [chunkJobWithMixedListenerWithin${scope}Scope]",
                logger: AllProcessListener.class.name)).size() == jobI
        mongoUtil.find(new LogCondition(
                message: "Common Logging: job finished. [chunkJobWithMixedListenerWithin${scope}Scope]",
                logger: AllProcessListener.class.name)).size() == jobI

        mongoUtil.find(new LogCondition(
                message: "Common Logging: step started. [chunkJobWithMixedListenerWithin${scope}Scope.step01]",
                logger: AllProcessListener.class.name)).size() == stepI
        mongoUtil.find(new LogCondition(
                message: "Common Logging: step finished. [chunkJobWithMixedListenerWithin${scope}Scope.step01]",
                logger: AllProcessListener.class.name)).size() == stepI

        mongoUtil.find(new LogCondition(message: 'Common Logging: before chunk.',
                logger: AllProcessListener.class.name)).size() == beforeChunkI
        mongoUtil.find(new LogCondition(message: 'Common Logging: after chunk.',
                logger: AllProcessListener.class.name)).size() == afterChunkI
        mongoUtil.find(new LogCondition(message: 'Common Logging: after chunk error.',
                logger: AllProcessListener.class.name)).size() == afterChunkErrorI

        mongoUtil.find(new LogCondition(message: 'Common Logging: before read.',
                logger: AllProcessListener.class.name)).size() == beforeReadI
        mongoUtil.find(new LogCondition(message: ~/Common Logging: after read./,
                logger: AllProcessListener.class.name)).size() == afterReadI
        mongoUtil.find(new LogCondition(message: 'Common Logging: on read error.',
                logger: AllProcessListener.class.name)).size() == onReadErrorI

        mongoUtil.find(new LogCondition(message: ~/Common Logging: before process./,
                logger: AllProcessListener.class.name)).size() == beforeProcessI
        mongoUtil.find(new LogCondition(message: ~/Common Logging: after process./,
                logger: AllProcessListener.class.name)).size() == afterProcessI
        mongoUtil.find(new LogCondition(message: 'Common Logging: on process error.',
                logger: AllProcessListener.class.name)).size() == onProcessErrorI

        mongoUtil.find(new LogCondition(message: 'Common Logging: before write. [Item size :5]',
                logger: AllProcessListener.class.name)).size() == beforeWriteI
        mongoUtil.find(new LogCondition(message: 'Common Logging: after write. [Item size :5]',
                logger: AllProcessListener.class.name)).size() == afterWriteI
        mongoUtil.find(new LogCondition(message: 'Common Logging: on write error.',
                logger: AllProcessListener.class.name)).size() == onWriteErrorI

        // check annotation base listener
        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: job started. [chunkJobWithMixedListenerWithin${scope}Scope]",
                logger: AnnotationAmountCheckProcessor.class.name)).size() == jobA
        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: job finished. [chunkJobWithMixedListenerWithin${scope}Scope]",
                logger: AnnotationAmountCheckProcessor.class.name)).size() == jobA

        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: step started. [chunkJobWithMixedListenerWithin${scope}Scope.step01]",
                logger: AnnotationAmountCheckProcessor.class.name)).size() == stepA
        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: step finished. [chunkJobWithMixedListenerWithin${scope}Scope.step01]",
                logger: AnnotationAmountCheckProcessor.class.name)).size() == stepA

        mongoUtil.find(new LogCondition(message: 'Annotation Logging: before chunk.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() == beforeChunkA
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: after chunk.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() == afterChunkA
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: after chunk error.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() == afterChunkErrorA

        mongoUtil.find(new LogCondition(message: 'Annotation Logging: before read.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() == beforeReadA
        mongoUtil.find(new LogCondition(message: ~/Annotation Logging: after read./,
                logger: AnnotationAmountCheckProcessor.class.name)).size() == afterReadA
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: on read error.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() == onReadErrorA

        mongoUtil.find(new LogCondition(message: ~/Annotation Logging: before process./,
                logger: AnnotationAmountCheckProcessor.class.name)).size() == beforeProcessA
        mongoUtil.find(new LogCondition(message: ~/Annotation Logging: after process./,
                logger: AnnotationAmountCheckProcessor.class.name)).size() == afterProcessA
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: on process error.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() == onProcessErrorA

        mongoUtil.find(new LogCondition(message: 'Annotation Logging: before write. [Item size :5]',
                logger: AnnotationAmountCheckProcessor.class.name)).size() == beforeWriteA
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: after write. [Item size :5]',
                logger: AnnotationAmountCheckProcessor.class.name)).size() == afterWriteA
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: on write error.',
                logger: AnnotationAmountCheckProcessor.class.name)).size() == onWriteErrorA

        // xxxI is interface implement listeners, xxxA is annotation base listeners.
        where:
        scope     | process     || jobI | stepI | beforeChunkI | afterChunkI | afterChunkErrorI | beforeReadI | afterReadI | onReadErrorI | beforeProcessI | afterProcessI | onProcessErrorI | beforeWriteI | afterWriteI | onWriteErrorI | jobA | stepA | beforeChunkA | afterChunkA | afterChunkErrorA | beforeReadA | afterReadA | onReadErrorA | beforeProcessA | afterProcessA | onProcessErrorA | beforeWriteA | afterWriteA | onWriteErrorA
        "Job"     | "reader"    || 1    | 0     | 0            | 0           | 0                | 0           | 0          | 0            | 0              | 0             | 0               | 0            | 0           | 0             | 0    | 1     | 1            | 0           | 1                | 4           | 3          | 1            | 0              | 0             | 0               | 0            | 0           | 0
        "Job"     | "processor" || 1    | 0     | 0            | 0           | 0                | 0           | 0          | 0            | 0              | 0             | 0               | 0            | 0           | 0             | 0    | 1     | 1            | 0           | 1                | 6           | 5          | 0            | 4              | 3             | 1               | 0            | 0           | 0
        "Job"     | "writer"    || 1    | 0     | 0            | 0           | 0                | 0           | 0          | 0            | 0              | 0             | 0               | 0            | 0           | 0             | 0    | 1     | 1            | 0           | 1                | 6           | 5          | 0            | 5              | 5             | 0               | 1            | 0           | 1
        "Step"    | "reader"    || 0    | 1     | 1            | 0           | 1                | 4           | 3          | 1            | 0              | 0             | 0               | 0            | 0           | 0             | 0    | 1     | 1            | 0           | 1                | 4           | 3          | 1            | 0              | 0             | 0               | 0            | 0           | 0
        "Step"    | "processor" || 0    | 1     | 1            | 0           | 1                | 6           | 5          | 0            | 4              | 3             | 1               | 0            | 0           | 0             | 0    | 1     | 1            | 0           | 1                | 6           | 5          | 0            | 4              | 3             | 1               | 0            | 0           | 0
        "Step"    | "writer"    || 0    | 1     | 1            | 0           | 1                | 6           | 5          | 0            | 5              | 5             | 0               | 1            | 0           | 1             | 0    | 1     | 1            | 0           | 1                | 6           | 5          | 0            | 5              | 5             | 0               | 1            | 0           | 1
        "Tasklet" | "reader"    || 0    | 1     | 1            | 0           | 1                | 4           | 3          | 1            | 0              | 0             | 0               | 0            | 0           | 0             | 0    | 1     | 1            | 0           | 1                | 4           | 3          | 1            | 0              | 0             | 0               | 0            | 0           | 0
        "Tasklet" | "processor" || 0    | 1     | 1            | 0           | 1                | 6           | 5          | 0            | 4              | 3             | 1               | 0            | 0           | 0             | 0    | 1     | 1            | 0           | 1                | 6           | 5          | 0            | 4              | 3             | 1               | 0            | 0           | 0
        "Tasklet" | "writer"    || 0    | 1     | 1            | 0           | 1                | 6           | 5          | 0            | 5              | 5             | 0               | 1            | 0           | 1             | 0    | 1     | 1            | 0           | 1                | 6           | 5          | 0            | 5              | 5             | 0               | 1            | 0           | 1
        "Chunk"   | "reader"    || 0    | 1     | 1            | 0           | 1                | 4           | 3          | 1            | 0              | 0             | 0               | 0            | 0           | 0             | 0    | 1     | 1            | 0           | 1                | 4           | 3          | 1            | 0              | 0             | 0               | 0            | 0           | 0
        "Chunk"   | "processor" || 0    | 1     | 1            | 0           | 1                | 6           | 5          | 0            | 4              | 3             | 1               | 0            | 0           | 0             | 0    | 1     | 1            | 0           | 1                | 6           | 5          | 0            | 4              | 3             | 1               | 0            | 0           | 0
        "Chunk"   | "writer"    || 0    | 1     | 1            | 0           | 1                | 6           | 5          | 0            | 5              | 5             | 0               | 1            | 0           | 1             | 0    | 1     | 1            | 0           | 1                | 6           | 5          | 0            | 5              | 5             | 0               | 1            | 0           | 1
        "All"     | "reader"    || 1    | 1     | 1            | 0           | 1                | 4           | 3          | 1            | 0              | 0             | 0               | 0            | 0           | 0             | 0    | 1     | 1            | 0           | 1                | 4           | 3          | 1            | 0              | 0             | 0               | 0            | 0           | 0
        "All"     | "processor" || 1    | 1     | 1            | 0           | 1                | 6           | 5          | 0            | 4              | 3             | 1               | 0            | 0           | 0             | 0    | 1     | 1            | 0           | 1                | 6           | 5          | 0            | 4              | 3             | 1               | 0            | 0           | 0
        "All"     | "writer"    || 1    | 1     | 1            | 0           | 1                | 6           | 5          | 0            | 5              | 5             | 0               | 1            | 0           | 1             | 0    | 1     | 1            | 0           | 1                | 6           | 5          | 0            | 5              | 5             | 0               | 1            | 0           | 1

    }

    // Testcase 6, test no.1
    @Unroll
    def "Only specific mixed listeners are processed (normal tasklet oriented  job) within #scope scope"() {
        setup:
        jobDB.deleteAll(["sales_plan_detail"] as String[])
        def inputFileName = "./files/test/input/ch04/listener/sales_plan_detail_correct.csv"

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/listener/taskletJobWithMixedListener.xml',
                jobName: "taskletJobWithMixedListenerWithin${scope}Scope",
                jobParameter: "inputFile=${inputFileName}"
        ))
        mongoUtil.waitForOutputLog(new LogCondition(
                message: ~/Closing org.springframework.context.support.ClassPathXmlApplicationContext/
        ))

        then:
        exitValue == 0
        // check interface implements listener
        mongoUtil.find(new LogCondition(
                message: "Common Logging: job started. [taskletJobWithMixedListenerWithin${scope}Scope]",
                logger: AllProcessListener.class.name)).size() == jobListener
        mongoUtil.find(new LogCondition(
                message: "Common Logging: job finished. [taskletJobWithMixedListenerWithin${scope}Scope]",
                logger: AllProcessListener.class.name)).size() == jobListener

        mongoUtil.find(new LogCondition(
                message: "Common Logging: step started. [taskletJobWithMixedListenerWithin${scope}Scope.step01]",
                logger: AllProcessListener.class.name)).size() == stepListener
        mongoUtil.find(new LogCondition(
                message: "Common Logging: step finished. [taskletJobWithMixedListenerWithin${scope}Scope.step01]",
                logger: AllProcessListener.class.name)).size() == stepListener

        mongoUtil.find(new LogCondition(message: 'Common Logging: before chunk.',
                logger: AllProcessListener.class.name)).size() == chunkListener
        mongoUtil.find(new LogCondition(message: 'Common Logging: after chunk.',
                logger: AllProcessListener.class.name)).size() == chunkListener
        mongoUtil.find(new LogCondition(message: 'Common Logging: after chunk error.',
                logger: AllProcessListener.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: 'Common Logging: before read.',
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: ~/Common Logging: after read./,
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Common Logging: on read error.',
                logger: AllProcessListener.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: ~/Common Logging: before process./,
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: ~/Common Logging: after process./,
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Common Logging: on process error.',
                logger: AllProcessListener.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: 'Common Logging: before write. [Item size :5]',
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Common Logging: after write. [Item size :5]',
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Common Logging: on write error.',
                logger: AllProcessListener.class.name)).size() == 0

        // check annotation base listener
        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: job started. [taskletJobWithMixedListenerWithin${scope}Scope]",
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: job finished. [taskletJobWithMixedListenerWithin${scope}Scope]",
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0

        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: step started. [taskletJobWithMixedListenerWithin${scope}Scope.step01]",
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: step finished. [taskletJobWithMixedListenerWithin${scope}Scope.step01]",
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: 'Annotation Logging: before chunk.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: after chunk.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: after chunk error.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: 'Annotation Logging: before read.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: ~/Annotation Logging: after read./,
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: on read error.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: ~/Annotation Logging: before process./,
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: ~/Annotation Logging: after process./,
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: on process error.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: 'Annotation Logging: before write. [Item size :5]',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: after write. [Item size :5]',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: on write error.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0

        // xxxI is interface implement listeners, xxxA is annotation base listeners.
        where:
        scope     || jobListener | stepListener | chunkListener
        "Job"     || 1           | 0            | 0
        "Step"    || 0           | 1            | 1
        "Tasklet" || 0           | 1            | 1
        "All"     || 1           | 1            | 1
    }

    // Testcase 6, test no.2
    @Unroll
    def "Only specific mixed listeners are processed (abnormal tasklet oriented job: error on #process) within #scope scope"() {
        setup:
        jobDB.deleteAll(["sales_plan_detail"] as String[])
        def inputFileName = "./files/test/input/ch04/listener/sales_plan_detail_incorrect_${process}.csv"

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/listener/taskletJobWithMixedListener.xml',
                jobName: "taskletJobWithMixedListenerWithin${scope}Scope",
                jobParameter: "inputFile=${inputFileName}"
        ))
        mongoUtil.waitForOutputLog(new LogCondition(
                message: ~/Closing org.springframework.context.support.ClassPathXmlApplicationContext/
        ))

        then:
        exitValue == 255
        // check interface implements listener
        mongoUtil.find(new LogCondition(
                message: "Common Logging: job started. [taskletJobWithMixedListenerWithin${scope}Scope]",
                logger: AllProcessListener.class.name)).size() == jobListener
        mongoUtil.find(new LogCondition(
                message: "Common Logging: job finished. [taskletJobWithMixedListenerWithin${scope}Scope]",
                logger: AllProcessListener.class.name)).size() == jobListener

        mongoUtil.find(new LogCondition(
                message: "Common Logging: step started. [taskletJobWithMixedListenerWithin${scope}Scope.step01]",
                logger: AllProcessListener.class.name)).size() == stepListener
        mongoUtil.find(new LogCondition(
                message: "Common Logging: step finished. [taskletJobWithMixedListenerWithin${scope}Scope.step01]",
                logger: AllProcessListener.class.name)).size() == stepListener

        mongoUtil.find(new LogCondition(message: 'Common Logging: before chunk.',
                logger: AllProcessListener.class.name)).size() == beforeChunk
        mongoUtil.find(new LogCondition(message: 'Common Logging: after chunk.',
                logger: AllProcessListener.class.name)).size() == afterChunk
        mongoUtil.find(new LogCondition(message: 'Common Logging: after chunk error.',
                logger: AllProcessListener.class.name)).size() == afterChunkError

        mongoUtil.find(new LogCondition(message: 'Common Logging: before read.',
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: ~/Common Logging: after read./,
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Common Logging: on read error.',
                logger: AllProcessListener.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: ~/Common Logging: before process./,
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: ~/Common Logging: after process./,
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Common Logging: on process error.',
                logger: AllProcessListener.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: 'Common Logging: before write. [Item size :5]',
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Common Logging: after write. [Item size :5]',
                logger: AllProcessListener.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Common Logging: on write error.',
                logger: AllProcessListener.class.name)).size() == 0

        // check annotation base listener
        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: job started. [taskletJobWithMixedListenerWithin${scope}Scope]",
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: job finished. [taskletJobWithMixedListenerWithin${scope}Scope]",
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0

        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: step started. [taskletJobWithMixedListenerWithin${scope}Scope.step01]",
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(
                message: "Annotation Logging: step finished. [taskletJobWithMixedListenerWithin${scope}Scope.step01]",
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: 'Annotation Logging: before chunk.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: after chunk.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: after chunk error.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: 'Annotation Logging: before read.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: ~/Annotation Logging: after read./,
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: on read error.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: ~/Annotation Logging: before process./,
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: ~/Annotation Logging: after process./,
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: on process error.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0

        mongoUtil.find(new LogCondition(message: 'Annotation Logging: before write. [Item size :5]',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: after write. [Item size :5]',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0
        mongoUtil.find(new LogCondition(message: 'Annotation Logging: on write error.',
                logger: AnnotationSalesPlanDetailRegisterTasklet.class.name)).size() == 0

        // xxxI is interface implement listeners, xxxA is annotation base listeners.
        where:
        scope     | process     || jobListener | stepListener | beforeChunk | afterChunk | afterChunkError
        "Job"     | "reader"    || 1           | 0            | 0           | 0          | 0
        "Job"     | "processor" || 1           | 0            | 0           | 0          | 0
        "Job"     | "writer"    || 1           | 0            | 0           | 0          | 0
        "Step"    | "reader"    || 0           | 1            | 1           | 0          | 1
        "Step"    | "processor" || 0           | 1            | 1           | 0          | 1
        "Step"    | "writer"    || 0           | 1            | 1           | 0          | 1
        "Tasklet" | "reader"    || 0           | 1            | 1           | 0          | 1
        "Tasklet" | "processor" || 0           | 1            | 1           | 0          | 1
        "Tasklet" | "writer"    || 0           | 1            | 1           | 0          | 1
        "All"     | "reader"    || 1           | 1            | 1           | 0          | 1
        "All"     | "processor" || 1           | 1            | 1           | 0          | 1
        "All"     | "writer"    || 1           | 1            | 1           | 0          | 1
    }

    // Testcase 7, test no.1
    def "Exception occurrs listener is processed (abnormal chunk oriented job)"() {
        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/listener/chunkJobWithErrorListener.xml',
                jobName: "chunkJobWithErrorListener"
        ))

        then:
        exitValue == 255

        def log = mongoUtil.findOne(new LogCondition(
                message: 'Encountered an error executing step chunkJobWithErrorListener.step01 in job chunkJobWithErrorListener',
                level: 'ERROR'
        ))
        log != null
        log.throwable._class.name == BeforeStepException.class.name

        mongoUtil.findOne(new LogCondition(
                logger : LoggingReader.class.name,
                message: "close method is called.")
        ) != null
        mongoUtil.findOne(new LogCondition(
                logger : LoggingWriter.class.name,
                message: "close method is called.")
        ) != null
    }

    // Testcase 8, test no.1
    def "Job Parameter Error occurrs listener is processed (abnormal chunk oriented job)"() {
        setup:
        def inputFileName = "./files/test/input/ch04/listener/sales_plan_detail_correct.csv"

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: "META-INF/jobs/ch04/listener/chunkJobWithAbortListener.xml",
                jobName: "chunkJobWithAbortListener",
                jobParameter: "inputFile=${inputFileName}"
        ))

        then:
        exitValue == 255

        def log = mongoUtil.findOne(new LogCondition(
                message: 'Encountered an error executing step chunkJobWithAbortListener.step01 in job chunkJobWithAbortListener',
                level: 'ERROR'
        ))
        log != null
        log.throwable._class.name == BeforeStepException.class.name

        mongoUtil.findOne(new LogCondition(
                logger : LoggingReader.class.name,
                message: "open method is called.")
        ) == null
        mongoUtil.findOne(new LogCondition(
                logger : LoggingWriter.class.name,
                message: "open method is called.")
        ) == null
    }

    // Testcase 8, test no.2
    def "Job Parameter Error occurrs listener is processed (abnormal tasklet oriented job)"() {
        setup:
        def inputFileName = "./files/test/input/ch04/listener/sales_plan_detail_correct.csv"

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: "META-INF/jobs/ch04/listener/taskletJobWithAbortListener.xml",
                jobName: "taskletJobWithAbortListener",
                jobParameter: "inputFile=${inputFileName}"
        ))

        then:
        exitValue == 255

        def log = mongoUtil.findOne(new LogCondition(
                message: 'Encountered an error executing step taskletJobWithAbortListener.step01 in job taskletJobWithAbortListener',
                level: 'ERROR'
        ))
        log != null
        log.throwable._class.name == BeforeStepException.class.name

        mongoUtil.findOne(new LogCondition(
                logger : LoggingReader.class.name,
                message: "close method is called.")
        ) == null
        mongoUtil.findOne(new LogCondition(
                logger : LoggingWriter.class.name,
                message: "close method is called.")
        ) == null
    }

}
