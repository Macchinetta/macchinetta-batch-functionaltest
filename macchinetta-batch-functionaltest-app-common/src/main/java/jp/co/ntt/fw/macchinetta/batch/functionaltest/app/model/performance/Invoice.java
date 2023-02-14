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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Model of Invoice.
 *
 * @since 2.0.1
 */
public class Invoice {

    /**
     * Invoice number.
     */
    private String invoiceNo;

    /**
     * Invoice date.
     */
    private Date invoiceDate;

    /**
     * Invoice amount.
     */
    private BigDecimal invoiceAmount;

    /**
     * Customer id.
     */
    private String customerId;

    /**
     * Invoice number.
     *
     * @return The current invoice number.
     */
    public String getInvoiceNo() {
        return invoiceNo;
    }

    /**
     * Invoice number.
     *
     * @param invoiceNo New invoice number.
     */
    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    /**
     * Invoice date.
     *
     * @return The current invoice date.
     */
    public Date getInvoiceDate() {
        return invoiceDate;
    }

    /**
     * Invoice date.
     *
     * @param invoiceDate New invoice date.
     */
    public void setInvoiceDate(Date invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    /**
     * Invoice amount.
     *
     * @return The current invoice amount.
     */
    public BigDecimal getInvoiceAmount() {
        return invoiceAmount;
    }

    /**
     * Invoice amount.
     *
     * @param invoiceAmount New invoice amount.
     */
    public void setInvoiceAmount(BigDecimal invoiceAmount) {
        this.invoiceAmount = invoiceAmount;
    }

    /**
     * Customer id.
     *
     * @return The current customer id.
     */
    public String getCustomerId() {
        return customerId;
    }

    /**
     * Customer id.
     *
     * @param customerId New customer id.
     */
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    /**
     * Represent a property of a bean.
     *
     * @return String of represented a property of a bean.
     */
    @Override
    public String toString() {
        return "Invoice{" + "invoiceNo='" + invoiceNo + '\'' + ", invoiceDate=" + invoiceDate + ", invoiceAmount="
                + invoiceAmount + ", customerId='" + customerId + '\'' + '}';
    }
}
