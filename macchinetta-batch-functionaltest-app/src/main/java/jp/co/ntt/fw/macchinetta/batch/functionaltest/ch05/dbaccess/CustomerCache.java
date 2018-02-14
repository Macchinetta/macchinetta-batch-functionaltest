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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.dbaccess;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Customer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Customer data cache for Listener test.
 *
 * @since 5.0.0
 */
@Component
public class CustomerCache {

    Map<String, Customer> customerMap = new HashMap<>();

    public Customer getCustomer(String customerId) {
        return customerMap.get(customerId);
    }

    public void addCustomer(String id, Customer customer) {
        customerMap.put(id, customer);
    }
}
