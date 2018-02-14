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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Model of Branch master.
 *
 * @since 5.0.0
 */
public class Branch implements Serializable {

    private static final long serialVersionUID = 1661343391022795047L;

    /**
     * Branch id.
     */
    private String branchId;

    /**
     * Branch name.
     */
    private String branchName;

    /**
     * Branch address.
     */
    private String branchAddress;

    /**
     * Branch telephone number.
     */
    private String branchTel;

    /**
     * Timestamp of created record.
     */
    private Timestamp createDate;

    /**
     * Timestamp of updated record.
     */
    private Timestamp updateDate;

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
     * Branch address.
     *
     * @return The current branch address.
     */
    public String getBranchAddress() {
        return branchAddress;
    }

    /**
     * Branch address.
     *
     * @param branchAddress New branch address.
     */
    public void setBranchAddress(String branchAddress) {
        this.branchAddress = branchAddress;
    }

    /**
     * Branch telephone number.
     *
     * @return The current branch telephone number.
     */
    public String getBranchTel() {
        return branchTel;
    }

    /**
     * Branch telephone number.
     *
     * @param branchTel New branch telephone number.
     */
    public void setBranchTel(String branchTel) {
        this.branchTel = branchTel;
    }

    /**
     * Timestamp of created record.
     *
     * @return The current timestamp of created record.
     */
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
        return "Branch{" + "branchId='" + branchId + '\'' + ", branchName='" + branchName + '\'' + ", branchAddress='"
                + branchAddress + '\'' + ", branchTel='" + branchTel + '\'' + ", createDate=" + createDate
                + ", updateDate=" + updateDate + '}';
    }

}
