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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.model.jaxb;

import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;

/**
 * Model of PhoneNumber.
 *
 * @since 2.0.1
 */
@XmlType(name = "phone-number")
public class PhoneNumber {

    /**
     * Phone number.
     */
    private String phoneNumber;

    /**
     * Phone number.
     *
     * @return The current phone number.
     */
    @XmlValue
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Phone number.
     *
     * @param phoneNumber New phone number.
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public String toString() {
        return phoneNumber;
    }
}
