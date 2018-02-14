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
import org.springframework.dao.InvalidDataAccessResourceUsageException
import org.springframework.dao.TransientDataAccessResourceException
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.DBUnitUtil
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.JobLauncher
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.JobRequest
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.MongoUtil
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.LogCondition
import spock.lang.Narrative
import spock.lang.Shared
import spock.lang.Specification

/**
 * Function test of database access.
 *
 * @since 5.0.0
 */
@Slf4j
@Narrative("""
Execute function test of DB access.
Test case conditions are follows.
Case1: To confirm DB access configuration.
    No.1: [Normal] Associate SQLMapper with interface by using MapperFactoryBean.
    No.2: [Normal] Associate SQLMapper with interface by using mybatis:scan.
Case2: To confirm DB access by using ItemWriter.
    No.1: [Error] ExecutorType of SqlsessionTemplate is not "BATCH".
Case3: To confirm DB access by using ItemProcessor and ItemListener.
    No.1: [Normal] Select DB data by ItemProcessor that using Mapper interface and DB access by ItemWriter.
    No.2: [Normal] Select DB data by Listener.
Case4 : To confirm DB access by using CompositeItemWriter.
    No.1: [Normal] Two tables update success.
    No.2: [Error] Two tables rollback together.
""")
class DBAccessSpec extends Specification {

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
        jobDB.dropAndCreateTable()
        mongoUtil.deleteAll()
    }

    def cleanupSpec() {
        mongoUtil.close()
        adminDB.close()
        jobDB.close()
    }

    def initSalesPerformanceDetailDataSet = DBUnitUtil.createDataSet {
        sales_performance_detail {
            branch_id | year | month | customer_id | amount
            "000001" | 2017 | 1 | "1000000000" | 1000
            "000002" | 2017 | 1 | "2000000000" | 1000
            "000003" | 2017 | 1 | "3000000000" | 1000
        }
    }

    def initCustomerMstDataSet = DBUnitUtil.createDataSet {
        customer_mst {
            customer_id | customer_name | customer_address | customer_tel | charge_branch_id | create_date | update_date
            "1000000000" | "CustomerName001" | "CustomerAddress001" | "11111111111" | "01" | "[now]" | "[now]"
            "2000000000" | "CustomerName002" | "CustomerAddress002" | "11111111111" | "02" | "[now]" | "[now]"
            "3000000000" | "CustomerName003" | "CustomerAddress003" | "11111111111" | "03" | "[now]" | "[now]"
        }
    }

    def expectSalesPerformanceDetailDataSet = DBUnitUtil.createDataSet {
        sales_performance_summary {
            branch_id | year | month | amount
            "000001" | 2017 | 1 | 1000
            "000002" | 2017 | 1 | 1000
            "000003" | 2017 | 1 | 1000
        }
    }

    def expectSalesPLanDetailDataSet = DBUnitUtil.createDataSet {
        sales_plan_detail {
            branch_id | year | month | customer_id | amount
            "01" | 2017 | 1 | 1000000000 | 1000
            "02" | 2017 | 1 | 2000000000 | 1000
            "03" | 2017 | 1 | 3000000000 | 1000
        }
    }

    // Testcase 1 No.1
    def "DB access by using MapperFactoryBean."() {

        setup:
        jobDB.insert(initSalesPerformanceDetailDataSet)

        when:
        int exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/dbaccess/DBAccessByMapperFactoryBean.xml',
                jobName: 'DBAccessByMapperFactoryBean'))

        then:
        exitCode == 0

        def actualTable = jobDB.getTable("sales_performance_summary")
        def expectTable = expectSalesPerformanceDetailDataSet.getTable("sales_performance_summary")
        DBUnitUtil.assertEquals(expectTable, actualTable)
    }

    // Testcase 1 No.2
    def "DB access by using mybatis:scan."() {

        setup:
        jobDB.insert(initSalesPerformanceDetailDataSet)

        when:
        int exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/dbaccess/DBAccessByMaybatisScan.xml',
                jobName: 'DBAccessByMaybatisScan'))

        then:
        exitCode == 0

        def actualTable = jobDB.getTable("sales_performance_summary")
        def expectTable = expectSalesPerformanceDetailDataSet.getTable("sales_performance_summary")
        DBUnitUtil.assertEquals(expectTable, actualTable)
    }

    // Testcase 2 No.1
    def "If ExecutorType of MyBatisBatchItemWriter is not 'BATCH', the job ends abnormally."() {

        setup:
        jobDB.insert(initSalesPerformanceDetailDataSet)

        when:
        int exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/dbaccess/DBAccessByItemWriter.xml',
                jobName: 'DBAccessByItemWriter'))

        then:
        exitCode == 1

        def log = mongoUtil.find(new LogCondition(
                level: 'ERROR'
                , logger: CommandLineJobRunner.class.name
                , message: ~/SqlSessionTemplate's executor type must be BATCH/))
        log.size() > 0
        log.get(0).throwable._class.name == 'org.springframework.beans.factory.BeanCreationException'
    }

    // Testcase 3 No.1
    def "DB access and data-transform in ItemProcessor."() {

        setup:
        jobDB.insert(initCustomerMstDataSet)
        jobDB.insert(initSalesPerformanceDetailDataSet)

        when:
        int exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/dbaccess/DBAccessByItemProcessor.xml',
                jobName: 'DBAccessByItemProcessor'))

        then:
        exitCode == 0

        def log = mongoUtil.find(new LogCondition(
                level: 'INFO',
                message: ~/UpdateItemFromDBProcessor is called./))
        log.size() > 0

        def actualTable = jobDB.getTable("sales_plan_detail")
        def expectTable = expectSalesPLanDetailDataSet.getTable("sales_plan_detail")
        DBUnitUtil.assertEquals(expectTable, actualTable)
    }

    // Testcase 3 No.2
    // Select DB data by Listener.
    def "DB access in ItemListener and transform data in ItemProcessor."() {

        setup:
        jobDB.insert(initCustomerMstDataSet)
        jobDB.insert(initSalesPerformanceDetailDataSet)

        when:
        int exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/dbaccess/DBAccessByItemListener.xml',
                jobName: 'DBAccessByItemListener'))

        then:
        exitCode == 0

        def log = mongoUtil.find(new LogCondition(
                level: 'INFO',
                message: ~/CacheSetListener is called at before step./))
        log.size() > 0

        def actualTable = jobDB.getTable("sales_plan_detail")
        def expectTable = expectSalesPLanDetailDataSet.getTable("sales_plan_detail")
        DBUnitUtil.assertEquals(expectTable, actualTable)
    }

    // Testcase 3 No.3
    def "Update by mapper interface on RESUE mode and ItemWriter on BATCH mode"() {
        setup:
        jobDB.insert(initCustomerMstDataSet)

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/dbaccess/updateMapperAndItemWriterJob.xml',
                jobName: 'updateMapperAndItemWriterJob'
        ))

        then:
        exitCode == 255

        def log = mongoUtil.find(new LogCondition(
                level: 'ERROR',
                logger: AbstractStep.class.name,
                message: 'Encountered an error executing step updateMapperAndItemWriterJob.step01 in job updateMapperAndItemWriterJob'
        ))
        log.size() == 1
        log.get(0).throwable._class == TransientDataAccessResourceException.class
        log.get(0).throwable.message == 'Cannot change the ExecutorType when there is an existing transaction'
    }

    // Testcase 3 No.4
    def "Update by mapper interface on BATCH mode and ItemWriter on BATCH mode"() {
        setup:
        jobDB.insert(initCustomerMstDataSet)

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/dbaccess/updateMapperAndItemWriterBatchModeJob.xml',
                jobName: 'updateMapperAndItemWriterBatchModeJob'
        ))

        then:
        exitCode == 255

        def log = mongoUtil.find(new LogCondition(
                level: 'ERROR',
                logger: AbstractStep.class.name,
                message: 'Encountered an error executing step updateMapperAndItemWriterBatchModeJob.step01 in job updateMapperAndItemWriterBatchModeJob'
        ))
        log.size() == 1
        log.get(0).throwable._class == InvalidDataAccessResourceUsageException.class
        log.get(0).throwable.message == 'Batch execution returned invalid results. Expected 1 but number of BatchResult objects returned was 2'
    }

    // Testcase 4 No.1
    def "Using CompositeItemWriter [success]"() {
        setup:
        def expectSuccessDataSet = DBUnitUtil.createDataSet {
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                "01" | 2017 | 1 | "1000000000" | 0
                "02" | 2017 | 1 | "2000000000" | 0
                "03" | 2017 | 1 | "3000000000" | 0
                "04" | 2017 | 1 | "4000000000" | 0
                "05" | 2017 | 1 | "5000000000" | 0
                "06" | 2017 | 1 | "6000000000" | 0
                "07" | 2017 | 1 | "7000000000" | 0
                "08" | 2017 | 1 | "8000000000" | 0
                "09" | 2017 | 1 | "9000000000" | 0
            }
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "01" | 2017 | 1 | "1000000000" | 1001
                "02" | 2017 | 1 | "2000000000" | 1001
                "03" | 2017 | 1 | "3000000000" | 1001
                "04" | 2017 | 1 | "4000000000" | 1001
                "05" | 2017 | 1 | "5000000000" | 1001
                "06" | 2017 | 1 | "6000000000" | 1001
                "07" | 2017 | 1 | "7000000000" | 1001
                "08" | 2017 | 1 | "8000000000" | 1001
                "09" | 2017 | 1 | "9000000000" | 1001
            }
        }

        jobDB.insert(DBUnitUtil.createDataSet {
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "01" | 2017 | 1 | "1000000000" | 1000
                "02" | 2017 | 1 | "2000000000" | 1000
                "03" | 2017 | 1 | "3000000000" | 1000
                "04" | 2017 | 1 | "4000000000" | 1000
                "05" | 2017 | 1 | "5000000000" | 1000
                "06" | 2017 | 1 | "6000000000" | 1000
                "07" | 2017 | 1 | "7000000000" | 1000
                "08" | 2017 | 1 | "8000000000" | 1000
                "09" | 2017 | 1 | "9000000000" | 1000
            }
        })

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/dbaccess/useCompositeItemWriter.xml',
                jobName: 'useCompositeItemWriter',
                jobParameter: "errorCount=-1"
        ))

        then:
        exitCode == 0

        DBUnitUtil.assertEquals(expectSuccessDataSet.getTable("sales_plan_detail"),
                jobDB.getTable("sales_plan_detail"))
        DBUnitUtil.assertEquals(expectSuccessDataSet.getTable("sales_performance_detail"),
                jobDB.getTable("sales_performance_detail"))
    }

    // Testcase 4 No.2
    def "Using CompositeItemWriter [error]"() {
        setup:
        def expectFailDataSet = DBUnitUtil.createDataSet {
            sales_performance_detail {
                branch_id | year | month | customer_id | amount
                "01" | 2017 | 1 | "1000000000" | 0
                "02" | 2017 | 1 | "2000000000" | 0
                "03" | 2017 | 1 | "3000000000" | 0
            }
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "01" | 2017 | 1 | "1000000000" | 1001
                "02" | 2017 | 1 | "2000000000" | 1001
                "03" | 2017 | 1 | "3000000000" | 1001
                "04" | 2017 | 1 | "4000000000" | 1000
                "05" | 2017 | 1 | "5000000000" | 1000
                "06" | 2017 | 1 | "6000000000" | 1000
                "07" | 2017 | 1 | "7000000000" | 1000
                "08" | 2017 | 1 | "8000000000" | 1000
                "09" | 2017 | 1 | "9000000000" | 1000
            }
        }

        jobDB.insert(DBUnitUtil.createDataSet {
            sales_plan_detail {
                branch_id | year | month | customer_id | amount
                "01" | 2017 | 1 | "1000000000" | 1000
                "02" | 2017 | 1 | "2000000000" | 1000
                "03" | 2017 | 1 | "3000000000" | 1000
                "04" | 2017 | 1 | "4000000000" | 1000
                "05" | 2017 | 1 | "5000000000" | 1000
                "06" | 2017 | 1 | "6000000000" | 1000
                "07" | 2017 | 1 | "7000000000" | 1000
                "08" | 2017 | 1 | "8000000000" | 1000
                "09" | 2017 | 1 | "9000000000" | 1000
            }
        })

        when:
        def exitCode = launcher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/ch05/dbaccess/useCompositeItemWriter.xml',
                jobName: 'useCompositeItemWriter',
                jobParameter: "errorCount=5"
        ))

        then:
        exitCode == 255

        DBUnitUtil.assertEquals(expectFailDataSet.getTable("sales_plan_detail"),
                jobDB.getTable("sales_plan_detail"))
        DBUnitUtil.assertEquals(expectFailDataSet.getTable("sales_performance_detail"),
                jobDB.getTable("sales_performance_detail"))
    }

}
