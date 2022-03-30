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
import org.dbunit.dataset.SortedTable
import org.springframework.batch.core.JobParametersInvalidException
import org.springframework.batch.core.launch.support.CommandLineJobRunner
import org.springframework.batch.core.step.AbstractStep
import org.springframework.beans.factory.UnsatisfiedDependencyException
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.jobparameter.AndEnvValRefInJavaTasklet
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.jobparameter.InvalidDefaultValSettingTasklet
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.jobparameter.RefInJavaTasklet
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.jobparameter.RefInXmlTasklet
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.jobparameter.RefWithDefaultInJavaTasklet
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.jobparameter.RefWithEnvValAsDefaultInJavaTasklet
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.jobparameter.RefWithDateTypeInXmlTasklet
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
 * Function test of job parameters.
 *
 * @since 2.0.1
 */
@Slf4j
@Narrative("""
Execute function test of job parameters. The test viewpoint is as follows. parameter conversion class,
passing and referencing parameters, validation of parameters, combined use of job parameters and properties.
A list of test cases is shown below.
1. Test of JobParametersConverter class.
2. Test of parameter passing and reference.
3. Test of JobParametersValidator.
4. Test of using job parameters and properties together.
""")
class JobParameterSpec extends Specification {

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


    def initCustomerMstDataSet = DBUnitUtil.createDataSet {
        customer_mst {
            customer_id | customer_name | customer_address | customer_tel | charge_branch_id | create_date | update_date
            "001" | "CustomerName001" | "CustomerAddress001" | "11111111111" | "001" | "[now]" | "[now]"
            "002" | "CustomerName002" | "CustomerAddress002" | "11111111111" | "002" | "[now]" | "[now]"
            "003" | "CustomerName003" | "CustomerAddress003" | "11111111111" | "003" | "[now]" | "[now]"
            "004" | "CustomerName004" | "CustomerAddress004" | "11111111111" | "004" | "[now]" | "[now]"
            "005" | "CustomerName005" | "CustomerAddress005" | "11111111111" | "005" | "[now]" | "[now]"
        }
    }

    // 1.1
    def "Execute the same job with synchronous launch, as a test of the parameter conversion class"() {
        setup:
        jobDBUnitUtil.insert(initCustomerMstDataSet)

        def path = "files/test/output/ch04/jobparameter/tmp/CustomerList1.csv"
        def jobRequest = new JobRequest(
                jobFilePath: 'META-INF/jobs/common/jobCustomerList01.xml',
                jobName: 'jobCustomerList01',
                jobParameter: "outputFile=" + path)

        when:
        int exitCode1 = jobLauncher.syncJob(jobRequest)
        int exitCode2 = jobLauncher.syncJob(jobRequest)

        def expectDataSet = DBUnitUtil.createDataSet {
            batch_job_execution_params {
                job_execution_id | type_cd | key_name | long_val
                1 | "LONG" | "jsr_batch_run_id" | 1
                1 | "STRING" | "outputFile" | 0
                2 | "LONG" | "jsr_batch_run_id" | 3
                2 | "STRING" | "outputFile" | 0
            }
        }
        def actualTable = adminDBUnitUtil.getIncludedColumnsTable("batch_job_execution_params",
                expectDataSet.getTableMetaData("batch_job_execution_params").getColumns())
        actualTable = new SortedTable(actualTable, ['job_execution_id', 'type_cd'] as String[])

        then:
        exitCode1 == 0
        exitCode2 == 0

        def expectITable = expectDataSet.getTable("batch_job_execution_params")
        DBUnitUtil.assertEquals(expectITable, actualTable)

        cleanup:
        Files.delete(new File(path).toPath())
    }

    // 1.2
    def "Execute the same job with asynchronous launch (DB polling), as a test of the parameter conversion class"() {
        setup:
        jobDBUnitUtil.insert(initCustomerMstDataSet)
        def p = jobLauncher.startAsyncBatchDaemon()

        def path = "files/test/output/ch04/jobparameter/tmp/CustomerList2.csv"
        def jobRequest = new JobRequest(
                jobName: 'jobCustomerList01',
                jobParameter: "outputFile=" + path)

        def jobSeqId1 = jobLauncher.registerAsyncJob(adminDBUnitUtil, jobRequest)
        jobLauncher.waitAsyncJob(adminDBUnitUtil, jobSeqId1)

        def jobSeqId2 = jobLauncher.registerAsyncJob(adminDBUnitUtil, jobRequest)
        jobLauncher.waitAsyncJob(adminDBUnitUtil, jobSeqId2)

        when:
        def expectDataSet = DBUnitUtil.createDataSet {
            batch_job_execution_params {
                job_execution_id | type_cd | key_name | long_val
                1 | "LONG" | "jsr_batch_run_id" | 1
                1 | "STRING" | "outputFile" | 0
                2 | "LONG" | "jsr_batch_run_id" | 3
                2 | "STRING" | "outputFile" | 0
            }
        }
        def actualTable = adminDBUnitUtil.getIncludedColumnsTable("batch_job_execution_params",
                expectDataSet.getTableMetaData("batch_job_execution_params").getColumns())
        actualTable = new SortedTable(actualTable, ['job_execution_id', 'type_cd'] as String[])

        then:
        def expectITable = expectDataSet.getTable("batch_job_execution_params")
        DBUnitUtil.assertEquals(expectITable, actualTable)

        cleanup:
        jobLauncher.stopAsyncBatchDaemon(p)
        Files.delete(new File(path).toPath())
    }

    // 2.1
    def "Set parameters from the command line and refer to it in the XML file (bean definition)"() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/jobparameter/jobRefWithDateTypeInXml.xml',
                jobName: 'jobRefWithDateTypeInXml',
                jobParameter: "str=Hello num=123 date=2016-01-02_12:34:56"))

        then:
        exitCode == 0
        def cursorFindOne = mongoUtil.findOne(
                new LogCondition(
                        logger: RefWithDateTypeInXmlTasklet.class.name,
                        level: 'INFO'
                ))
        cursorFindOne.message == 'str = Hello, num = 123, date = 2016-01-02_12:34:56'
    }

    // 2.2
    def "Set parameters from the command line and refer to it in the Java program"() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/jobparameter/jobRefInJava.xml',
                jobName: 'jobRefInJava',
                jobParameter: "str=Hello num=123"))

        then:
        exitCode == 0
        def cursorFindOne = mongoUtil.findOne(
                new LogCondition(
                        logger: RefInJavaTasklet.class.name,
                        level: 'INFO'
                ))
        cursorFindOne.message == 'str = Hello, num = 123'
    }

    // 2.3
    def "Set invalid format parameters from the command line and refer to it in the Java program"() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/jobparameter/jobRefInJava.xml',
                jobName: 'jobRefInJava',
                jobParameter: "str:Hello num:123"))

        then:
        exitCode == 255
        def cursorFindOne = mongoUtil.findOne(
                new LogCondition(
                        logger: AbstractStep.class.name,
                        level: 'ERROR'
                ))
        cursorFindOne.message == 'Encountered an error executing step jobRefInJava.step01 in job jobRefInJava'
        cursorFindOne.throwable._class == UnsatisfiedDependencyException
        cursorFindOne.throwable.stackTrace.size() > 0
        cursorFindOne.throwable.message.contains("Unsatisfied dependency expressed through field 'num'")
    }

    // 2.4
    def "Set parameters by redirecting file to standard input and refer to it in the Java program"() {
        setup:
        def file = new File("files/test/output/ch04/jobparameter/tmp/ch04_args.txt")
        def path = file.toPath()
        Files.deleteIfExists(path)
        file.createNewFile()
        file.text = """str=Hello
num=123
"""

        when:
        def p = new ProcessBuilder("java", "-cp", "target/dependency/*",
                "org.springframework.batch.core.launch.support.CommandLineJobRunner",
                "/META-INF/jobs/ch04/jobparameter/jobRefInJava.xml", "jobRefInJava").redirectInput(file).start()
        p.consumeProcessOutput(System.out as OutputStream, System.err as OutputStream)
        p.waitFor()

        then:
        p.exitValue() == 0
        def cursorFindOne = mongoUtil.findOne(
                new LogCondition(
                        logger: RefInJavaTasklet.class.name,
                        level: 'INFO'
                ))
        cursorFindOne.message == 'str = Hello, num = 123'

        cleanup:
        Files.delete(path)
    }

    // 2.5
    def "Set parameters including unreferenced keys from the command line and refer to it in the Java program"() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/jobparameter/jobRefInJava.xml',
                jobName: 'jobRefInJava',
                jobParameter: "str=Hello num=123 str2=World"))

        then:
        exitCode == 0
        def cursorFindOne = mongoUtil.findOne(
                new LogCondition(
                        logger: RefInJavaTasklet.class.name,
                        level: 'INFO'
                ))
        cursorFindOne.message == 'str = Hello, num = 123'
    }

    // 3.1
    def "Execute job without specifying required parameters, mandatory error occurs"() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/jobparameter/jobDefaultJobParametersValidator.xml',
                jobName: 'jobDefaultJobParametersValidator',
                jobParameter: "num=123"))

        then:
        exitCode == 255
        def cursorFindOne = mongoUtil.findOne(
                new LogCondition(
                        logger: CommandLineJobRunner.class.name,
                        level: 'ERROR'
                ))
        cursorFindOne.message == 'Job Terminated in error: The JobParameters do not contain required keys: [str]'
        cursorFindOne.throwable._class == JobParametersInvalidException
        cursorFindOne.throwable.message == "The JobParameters do not contain required keys: [str]"
    }

    // 3.2
    def "Execute job specifying unexpected parameters, invalid parameter error occurs"() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/jobparameter/jobDefaultJobParametersValidator.xml',
                jobName: 'jobDefaultJobParametersValidator',
                jobParameter: "str=Hello num=123 str2=World"))

        then:
        exitCode == 255
        def cursorFindOne = mongoUtil.findOne(
                new LogCondition(
                        logger: CommandLineJobRunner.class.name,
                        level: 'ERROR'
                ))
        cursorFindOne.message == 'Job Terminated in error: ' +
                'The JobParameters contains keys that are not explicitly optional or required: [str2]'
        cursorFindOne.throwable._class == JobParametersInvalidException
        cursorFindOne.throwable.message == "The JobParameters contains keys that are not explicitly optional or required: [str2]"
    }

    // 3.3
    def "Execute job specifying only required parameters, job is successful completion"() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/jobparameter/jobDefaultJobParametersValidator.xml',
                jobName: 'jobDefaultJobParametersValidator',
                jobParameter: "str=Hello"))

        then:
        exitCode == 0
        def cursorFindOne = mongoUtil.findOne(
                new LogCondition(
                        logger: RefWithDefaultInJavaTasklet.class.name,
                        level: 'INFO'
                ))
        cursorFindOne.message == 'str = Hello, num = 999, str2 = defaultStr2'
    }

    // 3.4
    def "Execute job specifying required parameters and optional parameters, job is successful completion"() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/jobparameter/jobDefaultJobParametersValidator.xml',
                jobName: 'jobDefaultJobParametersValidator',
                jobParameter: "str=Hello num=123"))

        then:
        exitCode == 0
        def cursorFindOne = mongoUtil.findOne(
                new LogCondition(
                        logger: RefWithDefaultInJavaTasklet.class.name,
                        level: 'INFO'
                ))
        cursorFindOne.message == 'str = Hello, num = 123, str2 = defaultStr2'
    }

    // 3.5
    def "Execute job specifying parameters with incorrect correlation, correlation error occurs"() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/jobparameter/jobComplexJobParametersValidator.xml',
                jobName: 'jobComplexJobParametersValidator',
                jobParameter: "str=Hello num=4"))

        then:
        exitCode == 255
        def cursorFindOne = mongoUtil.findOne(
                new LogCondition(
                        logger: CommandLineJobRunner.class.name,
                        level: 'ERROR'
                ))
        cursorFindOne.message == 'Job Terminated in error: ' +
                'The str must be less than or equal to num. [str:Hello][num:4]'
        cursorFindOne.throwable._class == JobParametersInvalidException
        cursorFindOne.throwable.message == "The str must be less than or equal to num. [str:Hello][num:4]"
    }

    // 3.6
    def "Execute job specifying parameters with correct correlation, job is successful completion"() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch04/jobparameter/jobComplexJobParametersValidator.xml',
                jobName: 'jobComplexJobParametersValidator',
                jobParameter: "str=Hello num=5"))

        then:
        exitCode == 0
        def cursorFindOne = mongoUtil.findOne(
                new LogCondition(
                        logger: RefWithDefaultInJavaTasklet.class.name,
                        level: 'INFO'
                ))
        cursorFindOne.message == 'str = Hello, num = 5, str2 = defaultStr2'
    }

    // 4.1
    def "Set both job parameters and environment variable with different values, and refer to it in the XML file (bean definition)"() {
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

    // 4.2
    def "Set both job parameters and environment variable with different values, and refer to it in the Java program"() {
        when:
        int exitCode = jobLauncher.syncJob { JobLauncher.SyncJobArg arg ->
            arg.jobRequest = new JobRequest(
                    jobFilePath: 'META-INF/jobs/ch04/jobparameter/jobAndEnvValRefInJava.xml',
                    jobName: 'jobAndEnvValRefInJava',
                    jobParameter: "num=123")
            arg.env = ['str=Hello']
        }

        then:
        exitCode == 0
        def cursorFindOne = mongoUtil.findOne(
                new LogCondition(
                        logger: AndEnvValRefInJavaTasklet.class.name,
                        level: 'INFO'
                ))
        cursorFindOne.message == 'str = Hello, num = 123'
    }

    // 4.3
    def "Set both job parameters and environment variable with different values, and refer to it in the XML file (bean definition) with specify environment variable as default value"() {
        when:
        int exitCode = jobLauncher.syncJob { JobLauncher.SyncJobArg arg ->
            arg.jobRequest = new JobRequest(
                    jobFilePath: 'META-INF/jobs/ch04/jobparameter/jobRefWithEnvValAsDefaultInXml.xml',
                    jobName: 'jobRefWithEnvValAsDefaultInXml',
                    jobParameter: "str=Hello num=123")
            arg.env = ['envstr=World', 'envnum=456']
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

    // 4.4
    def "Set both job parameters and environment variable with different values, and refer to it in the Java program with specify environment variable as default value"() {
        when:
        int exitCode = jobLauncher.syncJob { JobLauncher.SyncJobArg arg ->
            arg.jobRequest = new JobRequest(
                    jobFilePath: 'META-INF/jobs/ch04/jobparameter/jobRefWithEnvValAsDefaultInJava.xml',
                    jobName: 'jobRefWithEnvValAsDefaultInJava',
                    jobParameter: "str=Hello num=123")
            arg.env = ['envstr=World', 'envnum=456']
        }

        then:
        exitCode == 0
        def cursorFindOne = mongoUtil.findOne(
                new LogCondition(
                        logger: RefWithEnvValAsDefaultInJavaTasklet.class.name,
                        level: 'INFO'
                ))
        cursorFindOne.message == 'str = Hello, num = 123'
    }

    // 4.5
    def "Set environment variable, and refer to it in the XML file (bean definition) with specify environment variable as default value"() {
        when:
        int exitCode = jobLauncher.syncJob { JobLauncher.SyncJobArg arg ->
            arg.jobRequest = new JobRequest(
                    jobFilePath: 'META-INF/jobs/ch04/jobparameter/jobRefWithEnvValAsDefaultInXml.xml',
                    jobName: 'jobRefWithEnvValAsDefaultInXml')
            arg.env = ['envstr=World', 'envnum=456']
        }

        then:
        exitCode == 0
        def cursorFindOne = mongoUtil.findOne(
                new LogCondition(
                        logger: RefInXmlTasklet.class.name,
                        level: 'INFO'
                ))
        cursorFindOne.message == 'str = World, num = 456'
    }

    // 4.6
    def "Set environment variable, and refer to it in the Java program with specify environment variable as default value"() {
        when:
        int exitCode = jobLauncher.syncJob { JobLauncher.SyncJobArg arg ->
            arg.jobRequest = new JobRequest(
                    jobFilePath: 'META-INF/jobs/ch04/jobparameter/jobRefWithEnvValAsDefaultInJava.xml',
                    jobName: 'jobRefWithEnvValAsDefaultInJava')
            arg.env = ['envstr=World', 'envnum=456']
        }

        then:
        exitCode == 0
        def cursorFindOne = mongoUtil.findOne(
                new LogCondition(
                        logger: RefWithEnvValAsDefaultInJavaTasklet.class.name,
                        level: 'INFO'
                ))
        cursorFindOne.message == 'str = World, num = 456'
    }

    // 4.7
    def "Set both job parameters and environment variable with different values, and refer to it in the Java program with specify invalid format default value"() {
        when:
        int exitCode = jobLauncher.syncJob { JobLauncher.SyncJobArg arg ->
            arg.jobRequest = new JobRequest(
                    jobFilePath: 'META-INF/jobs/ch04/jobparameter/jobInvalidDefaultValSetting.xml',
                    jobName: 'jobInvalidDefaultValSetting')
            arg.env = ['envstr=World']
        }

        then:
        exitCode == 0
        def cursorFindOne = mongoUtil.findOne(
                new LogCondition(
                        logger: InvalidDefaultValSettingTasklet.class.name,
                        level: 'INFO'
                ))
        cursorFindOne.message == 'str = null'
    }
}
