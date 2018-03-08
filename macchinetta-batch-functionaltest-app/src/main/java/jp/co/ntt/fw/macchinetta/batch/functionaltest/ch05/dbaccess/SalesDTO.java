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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.dbaccess;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;

import java.io.Serializable;

/**
 * Sales DTO.
 */
public class SalesDTO implements Serializable {

    private static final long serialVersionUID = 1803409914904129847L;

    /**
     * Sales plan detail.
     */
    private SalesPlanDetail salesPlanDetail;

    /**
     * Sales performance detail.
     */
    private SalesPerformanceDetail salesPerformanceDetail;

    /**
     * Sales plan detail.
     *
     * @return The current of sales plan detail.
     */
    public SalesPlanDetail getSalesPlanDetail() {
        return salesPlanDetail;
    }

    /**
     * Sales plan detail.
     *
     * @param salesPlanDetail New sales plan detail.
     */
    public void setSalesPlanDetail(SalesPlanDetail salesPlanDetail) {
        this.salesPlanDetail = salesPlanDetail;
    }

    /**
     * Sales performance detail.
     *
     * @return The current of sales performance detail.
     */
    public SalesPerformanceDetail getSalesPerformanceDetail() {
        return salesPerformanceDetail;
    }

    /**
     * Sales performance detail.
     *
     * @param salesPerformanceDetail New sales performance detail.
     */
    public void setSalesPerformanceDetail(SalesPerformanceDetail salesPerformanceDetail) {
        this.salesPerformanceDetail = salesPerformanceDetail;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SalesDTO{");
        sb.append("salesPlanDetail=").append(salesPlanDetail);
        sb.append(", salesPerformanceDetail=").append(salesPerformanceDetail);
        sb.append('}');
        return sb.toString();
    }
}
