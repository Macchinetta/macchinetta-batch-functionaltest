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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.model.mst;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import java.sql.Timestamp;

/**
 * Model of Customer master.
 *
 * @since 2.0.1
 */
@XmlRootElement(name = "Customer")
@XmlType(propOrder = { "customerId", "customerName", "customerAddress", "customerTel", "chargeBranchId" })
public class CustomerToJaxb {

    /**
     * Customer id.
     */
    private String customerId;

    /**
     * Customer name.
     */
    private String customerName;

    /**
     * Customer address.
     */
    private String customerAddress;

    /**
     * Customer telephone number.
     */
    private String customerTel;

    /**
     * Id of the branch in charge of the customer.
     */
    private String chargeBranchId;

    /**
     * Timestamp of created record.
     */
    private Timestamp createDate;

    /**
     * Timestamp of updated record.
     */
    private Timestamp updateDate;

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
     * Customer name.
     *
     * @return The current customer name.
     */
    public String getCustomerName() {
        return customerName;
    }

    /**
     * Customer name.
     *
     * @param customerName New customer name.
     */
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    /**
     * Customer address.
     *
     * @return The current customer address.
     */
    public String getCustomerAddress() {
        return customerAddress;
    }

    /**
     * Customer address.
     *
     * @param customerAddress New customer address.
     */
    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    /**
     * Customer telephone number.
     *
     * @return The current customer telephone number.
     */
    public String getCustomerTel() {
        return customerTel;
    }

    /**
     * Customer telephone number.
     *
     * @param customerTel New customer telephone number.
     */
    public void setCustomerTel(String customerTel) {
        this.customerTel = customerTel;
    }

    /**
     * Id of the branch in charge of the customer.
     *
     * @return The current branch id.
     */
    public String getChargeBranchId() {
        return chargeBranchId;
    }

    /**
     * Id of the branch in charge of the customer.
     *
     * @param chargeBranchId New branch id.
     */
    public void setChargeBranchId(String chargeBranchId) {
        this.chargeBranchId = chargeBranchId;
    }

    /**
     * Timestamp of created record.
     *
     * @return The current timestamp of created record.
     */
    @XmlTransient
    public Timestamp getCreateDate() {
        return createDate;
    }

    /**
     * Timestamp of created record.
     *
     * @param createDate New timestamp of created record.
     */
    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    /**
     * Timestamp of updated record.
     *
     * @return The current timestamp of updated record.
     */
    @XmlTransient
    public Timestamp getUpdateDate() {
        return updateDate;
    }

    /**
     * Timestamp of updated record.
     *
     * @param updateDate New timestamp of updated record.
     */
    public void setUpdateDate(Timestamp updateDate) {
        this.updateDate = updateDate;
    }

    /**
     * Represent a property of a bean.
     *
     * @return String of represented a property of a bean.
     */
    @Override
    public String toString() {
        return "Customer{" + "customerId='" + customerId + '\'' + ", customerName='" + customerName + '\''
                + ", customerAddress='" + customerAddress + '\'' + ", customerTel='" + customerTel + '\''
                + ", chargeBranchId='" + chargeBranchId + '\'' + ", createDate=" + createDate + ", updateDate="
                + updateDate + '}';
    }

}
