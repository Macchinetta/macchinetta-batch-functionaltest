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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.util

import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient
import org.springframework.core.io.ClassPathResource

class RestUtil {

    final RESTClient restClient

    RestUtil() {
        def conf = new ConfigSlurper().parse(
                new ClassPathResource('rest-config.groovy').URL).restutil
        this.restClient = new RESTClient("${conf.schema ?: 'http'}://${conf.host}:${conf.port}/${conf.appName ?: 'macchinetta-batch-functionaltest-web'}/${conf.rootPath ?: 'api/v1/job'}/")
    }

    RestSubmitResult submitJob(String jobName, String jobParams = null) {
        assert jobName != null
        def bodyMap = [:]
        if (jobParams != null) {
            bodyMap['jobParams'] = jobParams
        }

        def res = restClient.post(path: jobName, contentType: ContentType.JSON, body: bodyMap)
        def result = new RestSubmitResult()
        result.status = res?.status
        def data = res?.data
        if (data != null && data instanceof Map) {
            result.data = new RestSubmitResult.JobExecutionData(data as Map)
        }
        result
    }

    def stopJob(long jobExecutionId) {
        assert jobExecutionId > 0
        putJob("stop/${jobExecutionId}")
    }

    def restartJob(long jobExecutionId) {
        assert jobExecutionId > 0
        putJob("restart/${jobExecutionId}")
    }

    def putJob(String path) {
        assert path != null
        def rest = restClient.put(path: path)
        def result = new RestJobExecution()
        result.status = rest?.status
        result.jobExecution = retrieveJobExecution(rest?.data)
        result
    }

    def getJobExecution(Long jobExecutionId) {
        def res = restClient.get(path: jobExecutionId, contentType: ContentType.JSON)
        def result = new RestJobExecution()
        result.status = res?.status
        def data = res?.data
        if (data != null && data instanceof Map) {
            result.jobExecution = new RestJobExecution.JobExecution(data as Map)
        }
        result
    }

    def get(String param) {
        restClient.get(path: param, contentType: ContentType.JSON)
    }

    RestJobExecution.JobExecution retrieveJobExecution(Object data) {
        if (data == null || !(data instanceof Map)) {
            return null
        }
        def map = data as Map
        def execution = new RestJobExecution.JobExecution()
        execution.jobExecutionId = map['jobExecutionId'] as long
        execution
    }
}
