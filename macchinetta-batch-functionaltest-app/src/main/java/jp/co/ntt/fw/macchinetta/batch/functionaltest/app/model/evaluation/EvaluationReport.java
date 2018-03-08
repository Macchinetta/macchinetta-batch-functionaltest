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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.evaluation;

import java.math.BigDecimal;

/**
 * Model of evaluation report.
 *
 * @since 5.0.0
 */
public class EvaluationReport {

    /**
     * Branch id.
     */
    private String branchId;

    /**
     * Branch name.
     */
    private String branchName;

    /**
     * Evaluation.
     */
    private String evaluation;

    /**
     * Actual ratio.
     */
    private BigDecimal ratio;

    /**
     * Reporting year.
     */
    private int year;

    /**
     * Reporting month.
     */
    private int month;

    /**
     * Amount of sales plan.
     */
    private BigDecimal planAmount;

    /**
     * Amount of sales performance.
     */
    private BigDecimal performanceAmount;

    /**
     * Branch id.
     *
     * @return The current branch id.
     */
    public String getBranchId() {
        return branchId;
    }

    /**
     * Branch id.
     *
     * @param branchId New branch id.
     */
    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    /**
     * Branch name.
     *
     * @return The current branch name.
     */
    public String getBranchName() {
        return branchName;
    }

    /**
     * Branch name.
     *
     * @param branchName New branch name.
     */
    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    /**
     * Evaluation.
     *
     * @return The evaluation.
     */
    public String getEvaluation() {
        return evaluation;
    }

    /**
     * Evaluation.
     *
     * @param evaluation New evaluation.
     */
    public void setEvaluation(String evaluation) {
        this.evaluation = evaluation;
    }

    /**
     * Actual ratio.
     *
     * @return The current actual ratio.
     */
    public BigDecimal getRatio() {
        return ratio;
    }

    /**
     * Actual ratio.
     *
     * @param ratio New actual ratio.
     */
    public void setRatio(BigDecimal ratio) {
        this.ratio = ratio;
    }

    /**
     * Reporting year.
     *
     * @return The current reporting year.
     */
    public int getYear() {
        return year;
    }

    /**
     * Reporting year.
     *
     * @param year New reporting year.
     */
    public void setYear(int year) {
        this.year = year;
    }

    /**
     * Reporting month.
     *
     * @return The current reporting month.
     */
    public int getMonth() {
        return month;
    }

    /**
     * Reporting month.
     *
     * @param month New reporting month.
     */
    public void setMonth(int month) {
        this.month = month;
    }

    /**
     * Amount of sales plan.
     *
     * @return The current amount of sales plan.
     */
    public BigDecimal getPlanAmount() {
        return planAmount;
    }

    /**
     * Amount of sales plan.
     *
     * @param planAmount New amount of sales plan.
     */
    public void setPlanAmount(BigDecimal planAmount) {
        this.planAmount = planAmount;
    }

    /**
     * Amount of sales performance.
     *
     * @return The current amount of sales performance.
     */
    public BigDecimal getPerformanceAmount() {
        return performanceAmount;
    }

    /**
     * Amount of sales performance.
     *
     * @param ratio New amount of sales performance.
     */
    public void performanceAmount(BigDecimal performanceAmount) {
        this.performanceAmount = performanceAmount;
    }

    /**
     * Represent a property of a bean.
     *
     * @return String of represented a property of a bean.
     */
    @Override
    public String toString() {
        return "EvaluationReport{" + "branchId='" + branchId + '\'' + ", branchName='" + branchName + '\''
                + ", evaluation='" + evaluation + '\'' + ", ratio=" + ratio + ", year=" + year + ", month=" + month
                + ", planAmount=" + planAmount + ", performanceAmount=" + performanceAmount + '}';
    }
}
