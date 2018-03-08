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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.repository;

import org.apache.ibatis.annotations.Param;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceSummary;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanSummary;

import java.util.List;

/**
 * Repository of sales summary.
 *
 * @since 5.0.0
 */
public interface SalesSummaryRepository {

    /**
     * Find summary of sales performance by year and month.
     *
     * @param year Target year.
     * @param month Target month.
     * @param dataSize Fetch size.
     * @param offset Offset.
     * @return Summary of sales performance matching the conditions.
     */
    List<SalesPerformanceSummary> findByYearAndMonth(@Param("year") int year, @Param("month") int month,
            @Param("dataSize") int dataSize, @Param("offset") int offset);

    /**
     * Count summary of sales performance by year and month.
     *
     * @param year Target year.
     * @param month Target month.
     * @return Summary of sales performance matching the conditions.
     */
    int countByYearAndMonth(@Param("year") int year, @Param("month") int month);

    /**
     * Create summary of sales plan.
     *
     * @param salesPlanSummary New summary of sales plan.
     */
    void create(SalesPlanSummary salesPlanSummary);
}
