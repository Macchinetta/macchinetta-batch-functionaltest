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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.dbaccess.repository;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.dbaccess.SalesDTO;

import java.util.List;

/**
 * Repository of sales in database access.
 *
 * @since 5.0.1
 */
public interface SalesRepository {

    /**
     * Find all detail of sales plan.
     *
     * @return all detail of sales plan.
     **/
    List<SalesPlanDetail> findAll();

    /**
     * Update detail of sales plan.
     *
     * @param salesDTO sales dto.
     */
    int update(SalesDTO salesDTO);

    /**
     * Create detail of sales performance.
     *
     * @param salesDTO sales dto.
     */
    int create(SalesDTO salesDTO);

}
