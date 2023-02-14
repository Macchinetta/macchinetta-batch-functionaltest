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

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Model of Customer.
 *
 * @since 2.0.1
 */
@XmlRootElement(name = "customer")
public class Customer {

    /**
     * Name.
     */
    private String name;

    /**
     * Phone numbers.
     */
    private List<PhoneNumber> phoneNumbers = new ArrayList<>();

    /**
     * Name.
     *
     * @return The current name.
     */
    @XmlElement
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
     * Phone numbers.
     *
     * @return The current phone numbers.
     */
    @XmlElement(name = "phone-number")
    @XmlElementWrapper(name = "phoneNumbers")
    public List<PhoneNumber> getPhoneNumbers() {
        return phoneNumbers;
    }

    /**
     * Phone numbers.
     *
     * @param phoneNumbers New phone numbers.
     */
    public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    @Override
    public String toString() {
        return "Customer{" + "name='" + name + '\'' + ", phoneNumbers='" + phoneNumbers + '\'' + '}';
    }
}
