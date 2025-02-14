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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.repository.mst;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.model.mst.CustomerToJaxb;
import org.apache.ibatis.cursor.Cursor;

/**
 * Repository of customer master.
 *
 * @since 2.0.1
 */
public interface CustomerRepository {
    /**
     * Fetch customer master data by cursor.
     *
     * @return Cursor for customer master data.
     */
    Cursor<CustomerToJaxb> findAll();
}
