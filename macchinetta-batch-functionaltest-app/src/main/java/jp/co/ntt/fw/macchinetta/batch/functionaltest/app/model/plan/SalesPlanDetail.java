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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan;

import java.math.BigDecimal;

/**
 * Model of Sales plan Detail.
 *
 * @since 5.0.0
 */
public class SalesPlanDetail {

    /**
     * Branch ID.
     */
    private String branchId;

    /**
     * Year.
     */
    private int year;

    /**
     * Month.
     */
    private int month;

    /**
     * Customer ID.
     */
    private String customerId;

    /**
     * Amount.
     */
    private BigDecimal amount;

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
     * Year.
     *
     * @return The current year.
     */
    public int getYear() {
        return year;
    }

    /**
     * Year.
     *
     * @param year New year.
     */
    public void setYear(int year) {
        this.year = year;
    }

    /**
     * Month.
     *
     * @return The current month.
     */
    public int getMonth() {
        return month;
    }

    /**
     * Month.
     *
     * @param month New month.
     */
    public void setMonth(int month) {
        this.month = month;
    }

    /**
     * Customer ID.
     *
     * @return The current customer id.
     */
    public String getCustomerId() {
        return customerId;
    }

    /**
     * Customer ID.
     *
     * @param customerId New customer id.
     */
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    /**
     * Amount.
     *
     * @return The current amount.
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Amount.
     *
     * @param amount New amount.
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * Represent a property of a bean.
     *
     * @return String of represented a property of a bean.
     */
    @Override
    public String toString() {
        return "SalesPlanDetail{" + "branchId='" + branchId + '\'' + ", year=" + year + ", month=" + month
                + ", customerId='" + customerId + '\'' + ", amount=" + amount + '}';
    }
}
