/*
 * Copyright (C) 2018 NTT Corporation
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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.sample

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.sample.ForSampleTasklet
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.DBUnitUtil
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.JobLauncher
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.JobRequest
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.LogCondition
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.MongoUtil

class JobLauncherSample {

    final JobLauncher jobLauncher

    final MongoUtil mongoUtil

    final DBUnitUtil dbUnitUtil

    final Process asyncBatchProcess

    JobLauncherSample() {
        this.jobLauncher = new JobLauncher()
        this.asyncBatchProcess = jobLauncher.startAsyncBatchDaemon()
        this.mongoUtil = new MongoUtil()
        this.dbUnitUtil = new DBUnitUtil('admin')
    }

    static void main(String[] args) {

        def sample = new JobLauncherSample()
        try {
            sample.setup()
            sample.'launch syncJob'()
            sample.setup()
            sample.'launch asyncJob'()
        } finally {
            sample.cleanupSpec()
        }
    }

    def cleanupSpec() {
        try {
            jobLauncher.stopAsyncBatchDaemon(asyncBatchProcess)
        } catch(Exception e) {
            e.printStackTrace()
        }
        try {
            mongoUtil.close()
        } catch(Exception e) {
            e.printStackTrace()
        }
        try {
            dbUnitUtil.close()
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    def setup() {
        mongoUtil.deleteAll()
    }

    def "launch syncJob"(){
        int exitCode = jobLauncher.syncJob { JobLauncher.SyncJobArg arg ->
            arg.jobRequest = new JobRequest(
                    jobFilePath: 'META-INF/jobs/common/jobForSample.xml',
                    jobName: 'jobForSample',
                    jobParameter: "xxx=${System.currentTimeMillis()}")
            arg.env = []
        }
        def syncLog = mongoUtil.findOne(new LogCondition(logger: ForSampleTasklet.class.name, level: 'INFO'))

        assert exitCode == 0
        assert syncLog.message == 'called tasklet.'
    }

    def "launch asyncJob"() {
        def jobRequest = new JobRequest(jobName: 'jobForSample', jobParameter: "xxx=${System.currentTimeMillis()}")

        def jobSeqId = jobLauncher.registerAsyncJob(dbUnitUtil, jobRequest)

        mongoUtil.waitForOutputLog(new LogCondition(
                logger: ForSampleTasklet.class.name, level: 'INFO', message: 'called tasklet.'))

        jobLauncher.waitAsyncJob(dbUnitUtil, jobSeqId)
    }
}
