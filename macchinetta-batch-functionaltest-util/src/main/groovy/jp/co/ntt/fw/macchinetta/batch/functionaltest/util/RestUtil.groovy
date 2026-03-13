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

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.springframework.core.io.ClassPathResource

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class RestUtil {

    final String baseUrl
    final HttpClient httpClient = HttpClient.newHttpClient()
    final JsonSlurper jsonSlurper = new JsonSlurper()

    RestUtil() {
        def conf = new ConfigSlurper().parse(
                new ClassPathResource('rest-config.groovy').URL).restutil
        this.baseUrl = "${conf.schema ?: 'http'}://${conf.host}:${conf.port}/${conf.appName ?: 'macchinetta-batch-functionaltest-web'}/${conf.rootPath ?: 'api/v1/job'}"
    }

    RestSubmitResult submitJob(String jobName, String jobParams = null) {
        assert jobName != null
        def bodyMap = [:]
        if (jobParams != null) {
            bodyMap['jobParams'] = jobParams
        }
        String jsonBody = new JsonBuilder(bodyMap).toString()
        def url = "${baseUrl}/${jobName}"

        def request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build()

        def response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        def statusCode = response.statusCode()
        def parsedData = parseJsonSafe(response.body())

        def result = new RestSubmitResult()
        result.status = statusCode
        if (parsedData instanceof Map) {
            result.data = new RestSubmitResult.JobExecutionData(parsedData)
        }
        return result
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
        def url = "${baseUrl}/${path}"
        def request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build()

        def response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        def statusCode = response.statusCode()
        def parsedData = parseJsonSafe(response.body())

        def result = new RestJobExecution()
        result.status = statusCode
        result.jobExecution = retrieveJobExecution(parsedData)
        return result
    }

    RestJobExecution getJobExecution(Long jobExecutionId) {
        def url = "${baseUrl}/${jobExecutionId}"
        def request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build()

        def response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        def statusCode = response.statusCode()
        def parsedData = parseJsonSafe(response.body())

        def result = new RestJobExecution()
        result.status = statusCode
        if (parsedData instanceof Map) {
            result.jobExecution = new RestJobExecution.JobExecution(parsedData)
        }
        return result
    }

    def get(String param) {
        def cleanBase = baseUrl.endsWith('/') ? baseUrl[0..-2] : baseUrl
        def url = "${cleanBase}/${param}"
        def request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build()

        def response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        def statusCode = response.statusCode()
        def responseBody = response.body()

        def parsedData = parseJsonSafe(responseBody)
        if (parsedData == null) {
            parsedData = responseBody
        }

        return [status: statusCode, data: parsedData]
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

    private Object parseJsonSafe(String body) {
        try {
            return jsonSlurper.parseText(body)
        } catch (Exception e) {
            return body
        }
    }
}
