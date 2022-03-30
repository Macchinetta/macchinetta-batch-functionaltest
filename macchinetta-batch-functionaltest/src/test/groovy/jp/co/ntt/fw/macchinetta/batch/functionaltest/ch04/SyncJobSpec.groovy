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
import org.springframework.batch.core.launch.support.CommandLineJobRunner
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.SimpleJobTasklet
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.JobLauncher
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.JobRequest
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.LogCondition
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.DBUnitUtil
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.MongoUtil
import spock.lang.Narrative
import spock.lang.Shared
import spock.lang.Specification

/**
 * Function test of synchronous job.
 *
 * @since 2.0.1
 *
 */
@Slf4j
@Narrative("""
Execute function test of synchronization job.
The test viewpoint is CommandLineJobRunner's arguments.
""")
class SyncJobSpec extends Specification {

    @Shared
            jobLauncher = new JobLauncher()

    @Shared
            adminDB = new DBUnitUtil("admin")

    @Shared
            mongoUtil = new MongoUtil()

    def setupSpec() {
        adminDB.dropAndCreateTable()
    }

    def setup() {
        log.debug("### Spec case of [{}]", this.specificationContext.currentIteration.displayName)
        mongoUtil.deleteAll()
    }

    def cleanupSpec() {
        mongoUtil.close()
    }

    def "If set the first and second arguments, the job ends normally."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/common/jobSimpleJob.xml',
                jobName: 'jobSimpleJob')
        )
        then:
        exitCode == 0
        def syncLog = mongoUtil.findOne(new LogCondition(logger: SimpleJobTasklet.class.name, level: 'INFO'))
        syncLog.message == 'called tasklet.'
    }

    def "If set only the first argument, the job abends."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/common/jobSimpleJob.xml',
                jobName: '')
        )

        then:
        exitCode == 1
        def log = mongoUtil.find(new LogCondition(
                level: 'ERROR',
                logger: CommandLineJobRunner.class.name,
                message: 'At least 2 arguments are required: JobPath/JobClass and jobIdentifier.')
        )
        log.size() > 0
    }

    def "If don't set argument, the job abends."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: '',
                jobName: '')
        )
        then:
        exitCode == 1
        def log = mongoUtil.find(new LogCondition(
                level: 'ERROR',
                logger: CommandLineJobRunner.class.name,
                message: 'At least 2 arguments are required: JobPath/JobClass and jobIdentifier.')
        )
        log.size() > 0
    }

    def "If set the first and second argument with -next option, the job ends abnormally."() {
        when:
        int exitCode = jobLauncher.syncJob(new JobRequest(
                jobFilePath: 'META-INF/jobs/common/jobSimpleJob.xml',
                jobName: 'jobSimpleJob',
                jobParameter: '-next')
        )
        then:
        exitCode == 255
        def log = mongoUtil.find(new LogCondition(
                level: 'ERROR',
                logger: CommandLineJobRunner.class.name,
                message: ~/Job Terminated in error: No job parameters incrementer found for job/)
        )
        log.size() > 0
    }
}
