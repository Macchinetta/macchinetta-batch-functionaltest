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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.sample

import groovy.json.JsonSlurper
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.RESTClient

/**
 * Sample for REST-API calling.
 */
class RestSample {

    public static void main(String[] args) {
        // case.1 simple get calling with RESTClient
        def cli = new RESTClient("http://macchinetta-ci:10100/view/batch5/api/json")
        def res = cli.get(contentType: ContentType.JSON)
        assert res.status == 200

        // case.2 get and response data processing in closure with HTTPBuilder
        def http = new HTTPBuilder('http://macchinetta-ci:10100/view/batch5/api/json')
        http.get(contentType: ContentType.JSON) { resp, reader ->
            assert resp instanceof groovyx.net.http.HttpResponseDecorator
            assert resp.status == 200
            System.out << reader
            println ''
            println reader.url
        }

        // case3. get and response processing with GDK only
        def url = "http://macchinetta-ci:10100/view/batch5/api/json"
        JdkCompatibleUrl jdkCompatibleUrl = new JdkCompatibleUrl();
        def json = jdkCompatibleUrl.createUrl(url).getText("UTF-8")
        new JsonSlurper().parseText(json).jobs.each {
            println "\"${it.name}\", \"${it.color}\""
        }
    }
}
