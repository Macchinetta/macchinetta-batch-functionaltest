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

import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.RestUtil

/**
 * This sample uses RestUtil.
 * CAUTION: To work this sample, deploy macchinetta-batch-functionaltest-web war file to application server.
 */
class RestUtilSample {

    static void main(String[] args) {

        final RestUtil restUtil = new RestUtil()

        def resultSubmit = restUtil.submitJob('jobForSample', "xxx=${System.currentTimeMillis()}")
        resultSubmit.with {
            assert status == 200
            assert data.jobName == 'jobForSample'
        }
        def jobExecutionId = resultSubmit.data.jobExecutionId

        def execution = restUtil.getJobExecution(jobExecutionId)
        assert execution.status == 200
        execution.jobExecution.with {
            assert jobName == 'jobForSample'
            assert status == 'COMPLETED'
            assert stepExecutions.size() == 1
            stepExecutions[0].with {
                assert stepName == 'forSample.step01'
                assert commitCount == 1
                assert readCount == 0
                assert writeCount == 0
                assert status == 'COMPLETED'
            }
        }
    }
}
