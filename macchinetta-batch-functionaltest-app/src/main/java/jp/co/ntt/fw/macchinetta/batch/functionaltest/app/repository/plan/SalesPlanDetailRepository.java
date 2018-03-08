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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.plan;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanSummary;

import java.util.List;

/**
 * Repository of detail of sales plan.
 *
 * @since 5.0.0
 */
public interface SalesPlanDetailRepository {
    /**
     * Find all detail of sales plan.
     *
     * @return all detail of sales plan.
     */
    List<SalesPlanDetail> findAll();

    /**
     * Create detail of sales plan.
     *
     * @param salesPlanDetail New detail of sales plan.
     */
    void create(SalesPlanDetail salesPlanDetail);

    /**
     * Delete all detail of sales plan.
     *
     * @return count of deleted data.
     */
    int deleteAll();

    /**
     * Summarize details of sales plan.
     *
     * @return Summary data summarizing details of sales plan.
     */
    List<SalesPlanSummary> summarizeDetails();
}
