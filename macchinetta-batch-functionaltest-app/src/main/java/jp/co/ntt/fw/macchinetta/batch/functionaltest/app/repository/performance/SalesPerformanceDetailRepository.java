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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.performance;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceSummary;

import java.util.List;

/**
 * Repository of detail of sales performance.
 *
 * @since 2.0.1
 */
public interface SalesPerformanceDetailRepository {

    /**
     * Find all detail of sales performance.
     *
     * @return all detail of sales performance.
     */
    List<SalesPerformanceDetail> findAll();

    /**
     * Create detail of sales performance.
     *
     * @param salesPerformanceDetail New detail of sales performance.
     */
    void create(SalesPerformanceDetail salesPerformanceDetail);

    /**
     * Delete all detail of sales performance.
     *
     * @return count of deleted data.
     */
    int deleteAll();

    /**
     * Summarize details of sales performance.
     *
     * @return Summary data summarizing details of sales performance.
     */
    List<SalesPerformanceSummary> summarizeDetails();

}
