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

/**
 * Model of sales plan detail header.
 *
 * @since 5.0.0
 */
public class SalesPlanDetailHeader extends SalesPlanDetailMultiLayoutRecord {

    /**
     * Description.
     */
    private String description;

    /**
     * Description.
     *
     * @return The current description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Description.
     *
     * @param description New description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Represent a property of a bean.
     *
     * @return String of represented a property of a bean.
     */
    @Override
    public String toString() {
        return "SalesPlanDetailHeader{record='" + record + "', description='" + description + "'}";
    }
}
