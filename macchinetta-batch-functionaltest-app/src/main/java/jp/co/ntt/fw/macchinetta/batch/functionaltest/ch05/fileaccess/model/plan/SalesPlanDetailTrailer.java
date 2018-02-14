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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.model.plan;

import java.math.BigDecimal;

/**
 * Model of Sales plan Detail.
 *
 * @since 5.0.0
 */
public class SalesPlanDetailTrailer extends SalesPlanDetailMultiLayoutRecord {

    /**
     * Branch ID.
     */
    private String branchId;

    /**
     * Number.
     */
    private int number;

    /**
     * total of amount.
     */
    private BigDecimal total;

    /**
     * Branch Id.
     *
     * @return The current branch id.
     */
    public String getBranchId() {
        return branchId;
    }

    /**
     * Branch Id.
     *
     * @param branchId New branch id.
     */
    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    /**
     * Number.
     *
     * @return The current number.
     */
    public int getNumber() {
        return number;
    }

    /**
     * Number.
     *
     * @param number New number.
     */
    public void setNumber(int number) {
        this.number = number;
    }

    /**
     * Total.
     *
     * @return The current total.
     */
    public BigDecimal getTotal() {
        return total;
    }

    /**
     * Total.
     *
     * @param total New total.
     */
    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    /**
     * Represent a property of a bean.
     *
     * @return String of represented a property of a bean.
     */
    @Override
    public String toString() {
        return "SalesPlanDetailTrailer{record='" + record + '\'' + ", branchId='" + branchId + '\'' + ", number="
                + number + ", total=" + total + '}';
    }
}
