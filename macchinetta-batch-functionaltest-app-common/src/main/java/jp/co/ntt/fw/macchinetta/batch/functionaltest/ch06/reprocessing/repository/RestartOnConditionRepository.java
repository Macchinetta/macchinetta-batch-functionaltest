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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.reprocessing.repository;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;
import org.apache.ibatis.cursor.Cursor;

/**
 * Repository of detail of sales plan with restartable on condition basis.
 *
 * @since 2.0.1
 */
public interface RestartOnConditionRepository {

    /**
     * Find details of sales plan with zero or less amount.
     *
     * @return details of sales plan that match the criteria.
     */
    Cursor<SalesPlanDetail> findByZeroOrLessAmount();

    /**
     * Update detail of sales plan.
     *
     * @param salesPlanDetail New detail of sales plan.
     */
    void update(SalesPlanDetail salesPlanDetail);
}
