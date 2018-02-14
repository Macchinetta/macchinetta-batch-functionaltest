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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.model;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;

/**
 * Model of sales plan detail with process name.
 *
 * @since 5.0.0
 */
public class SalesPlanDetailWithProcessName {

    /**
     * Process name.
     */
    private String processName;

    /**
     * Sales plan detail.
     */
    private SalesPlanDetail plan;

    /**
     * Process name.
     *
     * @return The current of process name.
     */
    public String getProcessName() {
        return processName;
    }

    /**
     * Process name.
     *
     * @param processName New process name.
     */
    public void setProcessName(String processName) {
        this.processName = processName;
    }

    /**
     * Sales plan detail.
     *
     * @return The current of sales plan detail.
     */
    public SalesPlanDetail getPlan() {
        return plan;
    }

    /**
     * Sales plan detail.
     *
     * @param plan New sales plan detail.
     */
    public void setPlan(SalesPlanDetail plan) {
        this.plan = plan;
    }

    @Override
    public String toString() {
        return "SalesPlanDetailWithProcessName{" + "processName='" + processName + '\'' + ", plan=" + plan + '}';
    }
}
