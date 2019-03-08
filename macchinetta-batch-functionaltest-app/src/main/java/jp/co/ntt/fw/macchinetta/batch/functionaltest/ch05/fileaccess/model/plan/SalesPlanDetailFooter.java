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

import java.io.Serializable;

/**
 * Model of footer of sales plan detail.
 *
 * @since 2.0.1
 */
public class SalesPlanDetailFooter implements Serializable {

    private static final long serialVersionUID = 8831576556024645828L;

    /**
     * Name.
     */
    private String name;

    /**
     * Value.
     */
    private String value;

    /**
     * neme.
     *
     * @return The current name.
     */
    public String getName() {
        return name;
    }

    /**
     * Name.
     *
     * @param name New name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Value.
     *
     * @return The current value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Value.
     *
     * @param value New value.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Represent a property of a bean.
     *
     * @return String of represented a property of a bean.
     */
    @Override
    public String toString() {
        return "SalesPlanDetailFooter{" + "name='" + name + '\'' + ", value=" + value + '}';
    }
}
