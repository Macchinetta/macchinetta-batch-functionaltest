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
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobOperator
import org.springframework.batch.core.launch.support.CommandLineJobRunner
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher
import org.springframework.context.ApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch07.jobmanagement.JobMonitor
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch07.jobmanagement.MessageGettingTasklet
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.DBUnitUtil
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.JobLauncher
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.JobRequest
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.LogCondition
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.MongoUtil
import spock.lang.Narrative
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.TimeUnit

/**
* Function test of job management.
*
* @since 2.0.1
*/
@Slf4j
@Narrative("""
A list of test cases is shown below.
1. Test job status management
2. Testing exit code customization
3. Double start prevention : double start prevention is not provided. Therefore, it is excluded from the test object.
4. Logging : Macchinetta Batch 2.x There is no specific function, so do not subject to the test.
5. Message management : Macchinetta Batch 2.x There is no specific function, so do not subject to the test.
""")
class JobManagementSpec extends Specification {

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
        log.debug("### Spec case of [{}]", this.specificationContext.currentIteration.displayName)
        mongoUtil.deleteAll()
    }

    def cleanupSpec() {
        mongoUtil.close()
        adminDB.close()
        jobDB.close()
    }

    // Testcase 1, test no.1
    def "Job status reference by JobExplore"() {
        setup:
        jobDB.deleteAll(["sales_plan_detail", "sales_plan_summary"] as String[])
        jobDB.insert(DBUnitUtil.createDataSet({
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "0001"    | 2016 | 12    | "C0001"     | 1000
                "0001"    | 2016 | 12    | "C0002"     | 2000
                "0001"    | 2016 | 12    | "C0003"     | 3000
                "0002"    | 2016 | 12    | "C0011"     | 1100
                "0002"    | 2016 | 12    | "C0012"     | 1200
                "0002"    | 2016 | 12    | "C0013"     | 1300
                "0003"    | 2016 | 12    | "C0011"     | 1500
                "0003"    | 2016 | 12    | "C0012"     | 1000
                "0004"    | 2016 | 12    | "C0013"     | 1000
                "0005"    | 2016 | 12    | "C0014"     | 1000
                "0006"    | 2016 | 12    | "C0015"     | 1000
                "0007"    | 2016 | 12    | "C0016"     | 1000
                "0008"    | 2016 | 12    | "C0017"     | 1000
                "0009"    | 2016 | 12    | "C0018"     | 1000
                "0010"    | 2016 | 12    | "C0019"     | 1000
                "0011"    | 2016 | 12    | "C0020"     | 1000
                "0012"    | 2016 | 12    | "C0021"     | 1000
            }
        }))
        ApplicationContext context
        def configurationType = System.getProperty('configurationType')
        if ("javaconfig".equals(configurationType)) {
            context = new AnnotationConfigApplicationContext(jp.co.ntt.fw.macchinetta.batch.functionaltest.jobs.common.JobSalesPlan02Config.class)
        } else {
            context = new ClassPathXmlApplicationContext('META-INF/jobs/common/jobSalesPlan02.xml')
        }
        def jobOperator = (JobOperator) context.getBean("jobOperator")
        def jobExplorer = (JobExplorer) context.getBean("jobExplorer")

        when:
        def properties = new Properties()
        properties.setProperty("param1", "dummy1")
        properties.setProperty("param2", "dummy2")
        def jobExecutionId = jobOperator.start('jobSalesPlan02', properties)
        def jobExecution = jobExplorer.getJobExecution(jobExecutionId)

        then:
        jobExecution != null
        jobExecution.jobInstance.jobName == 'jobSalesPlan02'
        !jobExecution.jobParameters.empty
        jobExecution.jobParameters.getString('param1') == 'dummy1'
        jobExecution.jobParameters.getString('param2') == 'dummy2'
        jobExecution.exitStatus == ExitStatus.COMPLETED

        jobExecution.stepExecutions.size() == 1
        def stepExecution = jobExecution.stepExecutions.first()
        stepExecution.commitCount == 2
        stepExecution.readCount == 12
        stepExecution.writeCount == 12
        stepExecution.rollbackCount == 0
        stepExecution.exitStatus == ExitStatus.COMPLETED
        stepExecution.status == BatchStatus.COMPLETED

        cleanup:
        context.close()
    }

    // Testcase 1, test no.1
    @Requires({ os.linux })
    def "Job status reference by JobExplore (Java App)(linux)"() {
        setup:
        adminDB.dropAndCreateTable()
        jobDB.deleteAll(["sales_plan_detail", "sales_plan_summary"] as String[])
        jobDB.insert(DBUnitUtil.createDataSet({
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "0001"    | 2016 | 12    | "C0001"     | 1000
                "0001"    | 2016 | 12    | "C0002"     | 2000
                "0001"    | 2016 | 12    | "C0003"     | 3000
                "0002"    | 2016 | 12    | "C0011"     | 1100
                "0002"    | 2016 | 12    | "C0012"     | 1200
                "0002"    | 2016 | 12    | "C0013"     | 1300
                "0003"    | 2016 | 12    | "C0011"     | 1500
                "0003"    | 2016 | 12    | "C0012"     | 1000
                "0004"    | 2016 | 12    | "C0013"     | 1000
                "0005"    | 2016 | 12    | "C0014"     | 1000
                "0006"    | 2016 | 12    | "C0015"     | 1000
                "0007"    | 2016 | 12    | "C0016"     | 1000
                "0008"    | 2016 | 12    | "C0017"     | 1000
                "0009"    | 2016 | 12    | "C0018"     | 1000
                "0010"    | 2016 | 12    | "C0019"     | 1000
                "0011"    | 2016 | 12    | "C0020"     | 1000
                "0012"    | 2016 | 12    | "C0021"     | 1000
            }
        }))
        ApplicationContext context
        def configurationType = System.getProperty('configurationType')
        if ("javaconfig".equals(configurationType)) {
            context = new AnnotationConfigApplicationContext(jp.co.ntt.fw.macchinetta.batch.functionaltest.jobs.common.JobSalesPlan02Config.class)
        } else {
            context = new ClassPathXmlApplicationContext('META-INF/jobs/common/jobSalesPlan02.xml')
        }
        def jobOperator = (JobOperator) context.getBean("jobOperator")

        when:
        def properties = new Properties()
        properties.setProperty("param1", "dummy1")
        properties.setProperty("param2", "dummy2")
        def jobExecutionId = jobOperator.start('jobSalesPlan02', properties)
        def command = "java -cp target/*:target/dependency/* jp.co.ntt.fw.macchinetta.batch.functionaltest.ch07.jobmanagement.JobMonitor ${jobExecutionId} ${configurationType}"

        def p = JobLauncher.executeProcess(command)
        p.waitForOrKill(TimeUnit.MINUTES.toMillis(2))

        then:
        p.exitValue() == 0
        mongoUtil.find(new LogCondition(
               logger: JobMonitor.class.name,
                level: 'INFO',
                message: 'Job Name : jobSalesPlan02'
        )).size() == 1
        mongoUtil.find(new LogCondition(
                logger: JobMonitor.class.name,
                level: 'INFO',
                message: 'Step Name : jobSalesPlan02.step01'
        )).size() == 1
        cleanup:
        context.close()
    }
    
    // Testcase 1, test no.1
    @Requires({ os.windows })
    def "Job status reference by JobExplore (Java App)(windows)"() {
        setup:
        adminDB.dropAndCreateTable()
        jobDB.deleteAll(["sales_plan_detail", "sales_plan_summary"] as String[])
        jobDB.insert(DBUnitUtil.createDataSet({
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "0001"    | 2016 | 12    | "C0001"     | 1000
                "0001"    | 2016 | 12    | "C0002"     | 2000
                "0001"    | 2016 | 12    | "C0003"     | 3000
                "0002"    | 2016 | 12    | "C0011"     | 1100
                "0002"    | 2016 | 12    | "C0012"     | 1200
                "0002"    | 2016 | 12    | "C0013"     | 1300
                "0003"    | 2016 | 12    | "C0011"     | 1500
                "0003"    | 2016 | 12    | "C0012"     | 1000
                "0004"    | 2016 | 12    | "C0013"     | 1000
                "0005"    | 2016 | 12    | "C0014"     | 1000
                "0006"    | 2016 | 12    | "C0015"     | 1000
                "0007"    | 2016 | 12    | "C0016"     | 1000
                "0008"    | 2016 | 12    | "C0017"     | 1000
                "0009"    | 2016 | 12    | "C0018"     | 1000
                "0010"    | 2016 | 12    | "C0019"     | 1000
                "0011"    | 2016 | 12    | "C0020"     | 1000
                "0012"    | 2016 | 12    | "C0021"     | 1000
            }
        }))
        ApplicationContext context
        def configurationType = System.getProperty('configurationType')
        if ("javaconfig".equals(configurationType)) {
            context = new AnnotationConfigApplicationContext(jp.co.ntt.fw.macchinetta.batch.functionaltest.jobs.common.JobSalesPlan02Config.class)
        } else {
            context = new ClassPathXmlApplicationContext('META-INF/jobs/common/jobSalesPlan02.xml')
        }
        def jobOperator = (JobOperator) context.getBean("jobOperator")

        when:
        def properties = new Properties()
        properties.setProperty("param1", "dummy1")
        properties.setProperty("param2", "dummy2")
        def jobExecutionId = jobOperator.start('jobSalesPlan02', properties)
        def command = "java -cp target/*;target/dependency/* jp.co.ntt.fw.macchinetta.batch.functionaltest.ch07.jobmanagement.JobMonitor ${jobExecutionId} ${configurationType}"

        def p = JobLauncher.executeProcess(command)
        p.waitForOrKill(TimeUnit.MINUTES.toMillis(2))

        then:
        p.exitValue() == 0
        mongoUtil.find(new LogCondition(
               logger: JobMonitor.class.name,
                level: 'INFO',
                message: 'Job Name : jobSalesPlan02'
        )).size() == 1
        mongoUtil.find(new LogCondition(
                logger: JobMonitor.class.name,
                level: 'INFO',
                message: 'Step Name : jobSalesPlan02.step01'
        )).size() == 1
        cleanup:
        context.close()
    }

    // Testcase 1, test no.2
    def "Job status reference by SQL direct issue"() {
        setup:
        adminDB.dropAndCreateTable() // Reconstruct to simplify the verification procedure.
        jobDB.deleteAll(["sales_plan_detail", "sales_plan_summary"] as String[])
        jobDB.insert(DBUnitUtil.createDataSet({
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "0001"    | 2016 | 12    | "C0001"     | 1000
                "0001"    | 2016 | 12    | "C0002"     | 2000
                "0001"    | 2016 | 12    | "C0003"     | 3000
                "0002"    | 2016 | 12    | "C0011"     | 1100
                "0002"    | 2016 | 12    | "C0012"     | 1200
                "0002"    | 2016 | 12    | "C0013"     | 1300
                "0003"    | 2016 | 12    | "C0011"     | 1500
                "0003"    | 2016 | 12    | "C0012"     | 1000
                "0004"    | 2016 | 12    | "C0013"     | 1000
                "0005"    | 2016 | 12    | "C0014"     | 1000
                "0006"    | 2016 | 12    | "C0015"     | 1000
                "0007"    | 2016 | 12    | "C0016"     | 1000
                "0008"    | 2016 | 12    | "C0017"     | 1000
                "0009"    | 2016 | 12    | "C0018"     | 1000
                "0010"    | 2016 | 12    | "C0019"     | 1000
                "0011"    | 2016 | 12    | "C0020"     | 1000
                "0012"    | 2016 | 12    | "C0021"     | 1000
                "0013"    | 2016 | 12    | "C0022"     | 1000
                "0014"    | 2016 | 12    | "C0023"     | 1000
                "0015"    | 2016 | 12    | "C0024"     | 1000
            }
        }))

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: launcher.getBeanDefinitionPath('jobSalesPlan02'),
                jobName: 'jobSalesPlan02',
                jobParameter: 'param3=dummy3 param4=dummy4'
        ))

        then:
        exitValue == 0

        // In this case, the ID of the job which was finally executed is obtained.
        def jobExecutions = adminDB.createQueryTable("job_executions",
                "SELECT T1.* FROM batch_job_execution T1 " +
                        "INNER JOIN batch_job_instance T2 ON T1.job_instance_id = T2.job_instance_id " +
                        "WHERE T2.job_name = 'jobSalesPlan02' ORDER BY T1.end_time DESC")
        jobExecutions.rowCount >= 1
        jobExecutions.getValue(0, 'exit_code') == 'COMPLETED'
        def jobExecutionId = jobExecutions.getValue(0, "job_execution_id")

        def jobParameters = adminDB.createQueryTable("job_parameters",
                "SELECT * FROM batch_job_execution_params WHERE job_execution_id = ${jobExecutionId}")
        def params = [:]
        for (int i in 0..<jobParameters.rowCount) {
            params.put(jobParameters.getValue(i, 'parameter_name'), jobParameters.getValue(i, 'parameter_value'))
        }
        params.get('param3') == 'dummy3'
        params.get('param4') == 'dummy4'

        def stepExecutions = adminDB.createQueryTable("step_executions",
                "SELECT * FROM batch_step_execution WHERE job_execution_id = ${jobExecutionId}" +
                        " ORDER BY step_execution_id")
        stepExecutions.rowCount == 1
        stepExecutions.getValue(0, 'commit_count') == 2
        stepExecutions.getValue(0, 'read_count') == 15
        stepExecutions.getValue(0, 'write_count') == 15
        stepExecutions.getValue(0, 'rollback_count') == 0
        stepExecutions.getValue(0, 'exit_code') == 'COMPLETED'
        stepExecutions.getValue(0, 'status') == 'COMPLETED'
    }

    // Testcase 1, test no.3
    def "Stop job by CommandLineJobRunner (specify job name)"() {
        setup:
        adminDB.dropAndCreateTable() // Reconstruct to simplify the verification procedure.
        setupInitialData()
        def command = "java -cp target/dependency/* ${CommandLineJobRunner.class.name} " +
                launcher.getBeanDefinitionPath('jobSalesPlan02WithLongTermTask') + " jobSalesPlan02WithLongTermTask interval=10000"

        when:
        def p = JobLauncher.executeProcess(command)
        mongoUtil.waitForOutputLog(new LogCondition(message: 'job started. [JobName:jobSalesPlan02WithLongTermTask]'))
        sleep(1000L)    // wait 1 seconds.
        def stopExitValue = launcher.syncJob(new JobRequest(
                jobFilePath: launcher.getBeanDefinitionPath('jobSalesPlan02WithLongTermTask'),
                jobName: 'jobSalesPlan02WithLongTermTask',
                jobParameter: '-stop'
        ))
        p.waitFor()

        then:
        p.exitValue() == 255
        stopExitValue == 0
        def jobExecutions = adminDB.createQueryTable("job_executions",
                "SELECT * FROM batch_job_execution ORDER BY job_execution_id")
        jobExecutions.rowCount == 1
        jobExecutions.getValue(0, 'exit_code') == 'STOPPED'
    }

    // Testcase 1, test no.4
    def "Stop job by CommandLineJobRunner (specify job execution id)"() {
        setup:
        adminDB.dropAndCreateTable() // Reconstruct to simplify the verification procedure.
        setupInitialData()
        ApplicationContext context
        def configurationType = System.getProperty('configurationType')
        if ("javaconfig".equals(configurationType)) {
            context = new AnnotationConfigApplicationContext(jp.co.ntt.fw.macchinetta.batch.functionaltest.jobs.ch07.jobmanagement.JobSalesPlan02WithLongTermTaskConfig.class)
        } else {
            context = new ClassPathXmlApplicationContext('META-INF/jobs/ch07/jobmanagement/jobSalesPlan02WithLongTermTask.xml')
        }
        def jobOperator = (JobOperator) context.getBean("jobOperator")
        def jobLauncher = (TaskExecutorJobLauncher) context.getBean("jobLauncher")
        def taskExecutor = new ThreadPoolTaskExecutor()
        taskExecutor.initialize()
        jobLauncher.setTaskExecutor(taskExecutor)

        when:
        def properties = new Properties()
        properties.setProperty("interval", "10000")
        def jobExecutionId = jobOperator.start("jobSalesPlan02WithLongTermTask", properties)
        mongoUtil.waitForOutputLog(new LogCondition(message: 'job started. [JobName:jobSalesPlan02WithLongTermTask]'))
        sleep(1000)

        def stopExitValue = launcher.syncJob(new JobRequest(
                jobFilePath: launcher.getBeanDefinitionPath('jobSalesPlan02WithLongTermTask'),
                jobName: jobExecutionId,
                jobParameter: '-stop'
        ))
        def sql = "SELECT * FROM batch_job_execution WHERE job_execution_id = ${jobExecutionId} AND end_time IS NOT NULL"
        def jobExecutions = adminDB.createQueryTable("job_executions", sql)
        while (jobExecutions.rowCount == 0) {
            sleep(500L)
            jobExecutions = adminDB.createQueryTable("job_executions", sql)
        }
        then:
        stopExitValue == 0
        jobExecutions.rowCount == 1
        jobExecutions.getValue(0, 'exit_code') == 'STOPPED'

        cleanup:
        context.close()
    }

    // Testcase 1, test no.5
    def "Job stoppage by JobOperator"() {
        setup:
        adminDB.dropAndCreateTable() // Reconstruct to simplify the verification procedure.
        setupInitialData()
        ApplicationContext context
        def configurationType = System.getProperty('configurationType')
        if ("javaconfig".equals(configurationType)) {
            context = new AnnotationConfigApplicationContext(jp.co.ntt.fw.macchinetta.batch.functionaltest.jobs.ch07.jobmanagement.JobSalesPlan02WithLongTermTaskConfig.class)
        } else {
            context = new ClassPathXmlApplicationContext('META-INF/jobs/ch07/jobmanagement/jobSalesPlan02WithLongTermTask.xml')
        }
        def jobOperator = (JobOperator) context.getBean("jobOperator")
        def jobLauncher = (TaskExecutorJobLauncher) context.getBean("jobLauncher")
        def taskExecutor = new ThreadPoolTaskExecutor()
        taskExecutor.initialize()
        jobLauncher.setTaskExecutor(taskExecutor)

        when:
        def properties = new Properties()
        def jobExecutionId = jobOperator.start("jobSalesPlan02WithLongTermTask", properties)
        mongoUtil.waitForOutputLog(new LogCondition(message: 'job started. [JobName:jobSalesPlan02WithLongTermTask]'))
        sleep(1000)
        jobOperator.stop(jobExecutionId)
        def sql = "SELECT * FROM batch_job_execution WHERE job_execution_id = ${jobExecutionId} AND end_time IS NOT NULL"
        def jobExecutions = adminDB.createQueryTable("job_executions", sql)
        while (jobExecutions.rowCount == 0) {
            sleep(500L)
            jobExecutions = adminDB.createQueryTable("job_executions", sql)
        }

        then:
        jobExecutions.rowCount == 1
        jobExecutions.getValue(0, 'exit_code') == 'STOPPED'

        cleanup:
        context.close()
    }

    // Testcase 1, test no.6-9
    def "Execute job stop by execution status"() {
        setup:
        log.debug("Execute job stop by execution status({})", beforeStatus.name())
        adminDB.dropAndCreateTable() // Reconstruct to simplify the verification procedure.
        setupInitialData()
        launcher.syncJob(new JobRequest(
                jobFilePath: launcher.getBeanDefinitionPath('jobSalesPlan02WithLongTermTask'),
                jobName: 'jobSalesPlan02WithLongTermTask',
                jobParameter: 'interval=0'
        ))
        mongoUtil.deleteAll()

        when:
        adminDB.update(DBUnitUtil.createDataSet({
            batch_job_execution {
                job_execution_id | status              | end_time
                1                | beforeStatus.name() | endTime
            }
        }))
        def stopExitValue = launcher.syncJob(new JobRequest(
                jobFilePath: launcher.getBeanDefinitionPath('jobSalesPlan02WithLongTermTask'),
                jobName: 'jobSalesPlan02WithLongTermTask',
                jobParameter: '-stop'
        ))

        then:
        stopExitValue == exitValue
        def actualJobExecution = adminDB.getTable("batch_job_execution")
        actualJobExecution.getValue(0, 'status') == afterStatus.name()
        def terminateLogs = mongoUtil.find(new LogCondition(message: 'Job Terminated in error: No running execution found for job=jobSalesPlan02WithLongTermTask'))
        terminateLogs.size() == logCount

        where:
        beforeStatus          | endTime  || exitValue | afterStatus            | logCount
        BatchStatus.COMPLETED | '[now]'  || 255       | BatchStatus.COMPLETED  | 1         // test no.6
        BatchStatus.FAILED    | '[now]'  || 255       | BatchStatus.FAILED     | 1         // test no.7
        BatchStatus.STOPPED   | '[now]'  || 255       | BatchStatus.STOPPED    | 1         // test no.8
        BatchStatus.STARTING  | '[null]' || 0         | BatchStatus.STOPPING   | 0         // test no.9

    }


    // Testcase 2, test no.1
    def "Confirm customization of exit code by chunk"() {
        setup:
        adminDB.dropAndCreateTable() // Reconstruct to simplify the verification procedure.
        jobDB.deleteAll(["sales_plan_detail", "sales_plan_summary"] as String[])
        jobDB.insert(DBUnitUtil.createDataSet({
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "0001"    | 2016 | 12    | "C0001"     | 1000
                "0001"    | 2016 | 12    | "C0002"     | 2000
                "0001"    | 2016 | 12    | "C0003"     | 3000
                "0002"    | 2016 | 12    | "C0011"     | 1100
                "0002"    | 2016 | 12    | "C0012"     | 1200
                "0002"    | 2016 | 12    | "C0013"     | 1300
                "0003"    | 2016 | 12    | "C0011"     | -1500
                "0003"    | 2016 | 12    | "C0012"     | 1000
                "0004"    | 2016 | 12    | "C0013"     | 1000
                "0005"    | 2016 | 12    | "C0014"     | 1000
                "0006"    | 2016 | 12    | "C0015"     | 1000
                "0007"    | 2016 | 12    | "C0016"     | -1000
                "0008"    | 2016 | 12    | "C0017"     | 1000
                "0009"    | 2016 | 12    | "C0018"     | 1000
                "0010"    | 2016 | 12    | "C0019"     | 1000
                "0011"    | 2016 | 12    | "C0020"     | 1000
                "0012"    | 2016 | 12    | "C0021"     | 1000
            }
        }))

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: launcher.getBeanDefinitionPath('customizedJobExitCodeChunkJob'),
                jobName: 'customizedJobExitCodeChunkJob'
        ))

        then:
        exitValue == 100
        def jobExecution = adminDB.getTable("batch_job_execution")
        jobExecution.getValue(0, 'status') == 'COMPLETED'
        jobExecution.getValue(0, 'exit_code') == 'JOB COMPLETED WITH SKIPS'
        def stepExecution = adminDB.getTable("batch_step_execution")
        stepExecution.getValue(0, 'status') == 'COMPLETED'
        stepExecution.getValue(0, 'exit_code') == 'STEP COMPLETED WITH SKIPS'

    }

    // Testcase 2, test no.2
    def "Confirm customization of exit code by tasklet"() {
        setup:
        adminDB.dropAndCreateTable() // Reconstruct to simplify the verification procedure.
        jobDB.deleteAll(["sales_plan_detail", "sales_plan_summary"] as String[])
        jobDB.insert(DBUnitUtil.createDataSet({
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "0001"    | 2016 | 12    | "C0001"     | 1000
                "0001"    | 2016 | 12    | "C0002"     | 2000
                "0001"    | 2016 | 12    | "C0003"     | 3000
                "0002"    | 2016 | 12    | "C0011"     | 1100
                "0002"    | 2016 | 12    | "C0012"     | 1200
                "0002"    | 2016 | 12    | "C0013"     | 1300
                "0003"    | 2016 | 12    | "C0011"     | -1500
                "0003"    | 2016 | 12    | "C0012"     | 1000
                "0004"    | 2016 | 12    | "C0013"     | 1000
                "0005"    | 2016 | 12    | "C0014"     | 1000
                "0006"    | 2016 | 12    | "C0015"     | 1000
                "0007"    | 2016 | 12    | "C0016"     | -1000
                "0008"    | 2016 | 12    | "C0017"     | 1000
                "0009"    | 2016 | 12    | "C0018"     | 1000
                "0010"    | 2016 | 12    | "C0019"     | 1000
                "0011"    | 2016 | 12    | "C0020"     | 1000
                "0012"    | 2016 | 12    | "C0021"     | 1000
            }
        }))

        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: launcher.getBeanDefinitionPath('customizedJobExitCodeTaskletJob'),
                jobName: 'customizedJobExitCodeTaskletJob'
        ))

        then:
        exitValue == 100
        def jobExecution = adminDB.getTable("batch_job_execution")
        jobExecution.getValue(0, 'status') == 'COMPLETED'
        jobExecution.getValue(0, 'exit_code') == 'JOB COMPLETED WITH SKIPS'
        def stepExecution = adminDB.getTable("batch_step_execution")
        stepExecution.getValue(0, 'status') == 'COMPLETED'
        stepExecution.getValue(0, 'exit_code') == 'STEP COMPLETED WITH SKIPS'
    }

    // Testcase 2, test no.3
    def "Confirmation of exit code mapping (normal termination)"() {
        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: launcher.getBeanDefinitionPath('emulateLongTaskJob'),
                jobName: 'emulateLongTaskJob'
        ))

        then:
        exitValue == 0
    }

    // Testcase 2, test no.4
    def "Confirmation of exit code mapping (abnormal termination)"() {
        when:
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: launcher.getBeanDefinitionPath('emulateLongTaskJob'),
                jobName: 'emulateLongTaskJob',
                jobParameter: 'shouldFail=true'
        ))

        then:
        mongoUtil.findOne(new LogCondition(message: 'job started. [JobName:emulateLongTaskJob]')) != null
        exitValue == 250
    }

    // Testcase 2, test no.5
    def "Confirmation of exit code mapping (stop)"() {
        setup:
        def command = "java -cp target/dependency/* ${CommandLineJobRunner.class.name} " +
                launcher.getBeanDefinitionPath('emulateLongTaskJob') + " emulateLongTaskJob interval=10000"

        when:
        def p = JobLauncher.executeProcess(command)
        mongoUtil.waitForOutputLog(new LogCondition(message: 'job started. [JobName:emulateLongTaskJob]'))
        sleep(1000L)    // wait 1 seconds.
        def exitValue = launcher.syncJob(new JobRequest(
                jobFilePath: launcher.getBeanDefinitionPath('emulateLongTaskJob'),
                jobName: 'emulateLongTaskJob',
                jobParameter: '-stop'
        ))
        p.waitFor()

        then:
        p.exitValue() == 200
        exitValue == 0
    }

    // Testcase 5, test no.1
    def "Message getting by MessageSource"() {
        when:
        int exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: launcher.getBeanDefinitionPath('getMessageJob'),
                jobName: 'getMessageJob'))

        then:
        exitCode == 0
        def cursorFindOne = mongoUtil.findOne(
                new LogCondition(
                        logger: MessageGettingTasklet.class.name,
                        level: 'INFO'
                ))
        cursorFindOne.message == 'This message is Info-Level. Test Message'
    }

    // utils
    def setupInitialData() {
        jobDB.deleteAll(["sales_plan_detail", "sales_plan_summary"] as String[])
        jobDB.insert(DBUnitUtil.createDataSet({
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "0001"    | 2016 | 12    | "C0001"     | 1000
                "0001"    | 2016 | 12    | "C0002"     | 2000
                "0001"    | 2016 | 12    | "C0003"     | 3000
                "0002"    | 2016 | 12    | "C0011"     | 1100
                "0002"    | 2016 | 12    | "C0012"     | 1200
                "0002"    | 2016 | 12    | "C0013"     | 1300
                "0003"    | 2016 | 12    | "C0011"     | 1500
                "0003"    | 2016 | 12    | "C0012"     | 1000
                "0004"    | 2016 | 12    | "C0013"     | 1000
                "0005"    | 2016 | 12    | "C0014"     | 1000
                "0006"    | 2016 | 12    | "C0015"     | 1000
                "0007"    | 2016 | 12    | "C0016"     | 1000
                "0008"    | 2016 | 12    | "C0017"     | 1000
                "0009"    | 2016 | 12    | "C0018"     | 1000
                "0010"    | 2016 | 12    | "C0019"     | 1000
                "0011"    | 2016 | 12    | "C0020"     | 1000
                "0012"    | 2016 | 12    | "C0021"     | 1000
            }
        }))
    }
}
