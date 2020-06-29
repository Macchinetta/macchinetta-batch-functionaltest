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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch06.inputvalidation.model.plan;

import org.springframework.batch.item.ItemCountAware;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Model of Sales plan Detail.
 *
 * @since 2.0.1
 */
public class VerificationSalesPlanDetail implements ItemCountAware {

    private int count;

    /**
     * Branch ID.
     */
    @NotEmpty
    @Size(min = 1, max = 6)
    private String branchId;

    /**
     * Year.
     */
    @NotNull
    @Min(1)
    @Max(9999)
    private int year;

    /**
     * Month.
     */
    @NotNull
    @Min(1)
    @Max(12)
    private int month;

    /**
     * Customer ID.
     */
    @NotEmpty
    @Size(min = 1, max = 10)
    private String customerId;

    /**
     * Amount.
     */
    @NotNull
    @DecimalMin("0")
    @DecimalMax("9999999999")
    private BigDecimal amount;

    /**
     * Count.
     *
     * @return The current count.
     */
    public int getCount() {
        return count;
    }

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

    @Override
    public void setItemCount(int count) {
        this.count = count;
    }

    /**
     * Represent a property of a bean.
     *
     * @return String of represented a property of a bean.
     */
    @Override
    public String toString() {
        return "VerificationSalesPlanDetail{" + "count='" + count + '\'' + ", branchId='" + branchId + '\'' + ", year="
                + year + ", month=" + month + ", customerId='" + customerId + '\'' + ", amount=" + amount + '}';
    }
}
