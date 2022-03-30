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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.performance;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.Invoice;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceDetail;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.cursor.Cursor;

import java.util.List;

/**
 * Repository of invoice.
 *
 * @since 2.0.1
 */
public interface InvoiceRepository {

    /**
     * Find all invoice data.
     *
     * @return All invoice data.
     */
    List<Invoice> findAll();

    /**
     * Create invoice data.
     *
     * @param invoice New invoice data.
     */
    void create(Invoice invoice);

    /**
     * Delete all invoice data.
     *
     * @return count of deleted data.
     */
    int deleteAll();

    /**
     * Get cursor for summarize invoice by branch.
     *
     * @param branchId Branch id.
     * @return Cursor for summarized invoice by branch.
     */
    Cursor<SalesPerformanceDetail> summarizeInvoice(@Param("branchId") String branchId);
}
