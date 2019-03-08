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
 * @since 2.0.1
 */
public class SalesPlanDetailEnd extends SalesPlanDetailMultiLayoutRecord {

    /**
     * Number of header.
     */
    private int headNum;

    /**
     * Number of trailer.
     */
    private int trailerNum;

    /**
     * total of amount.
     */
    private BigDecimal total;

    /**
     * Number of header.
     *
     * @return The current headNum.
     */
    public int getHeadNum() {
        return headNum;
    }

    /**
     * Number of header.
     *
     * @param headNum New headNum.
     */
    public void setHeadNum(int headNum) {
        this.headNum = headNum;
    }

    /**
     * Number of trailer.
     *
     * @return The current number.
     */
    public int getTrailerNum() {
        return trailerNum;
    }

    /**
     * Number of trailer.
     *
     * @param trailerNum New trailerNum.
     */
    public void setTrailerNum(int trailerNum) {
        this.trailerNum = trailerNum;
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
        return "SalesPlanDetailEnd{" + "record='" + record + '\'' + ", headNum=" + headNum + ", trailerNum="
                + trailerNum + ", total=" + total + '}';
    }
}
