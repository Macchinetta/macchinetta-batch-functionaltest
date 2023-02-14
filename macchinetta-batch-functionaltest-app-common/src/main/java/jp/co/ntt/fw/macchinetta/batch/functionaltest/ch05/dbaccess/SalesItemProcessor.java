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

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.SalesPerformanceDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;

import java.math.BigDecimal;

/**
 * Execute business logic.
 * <p>
 * <ul>
 * <li>Update amount on sales plan detail.</li>
 * <li>Crete new sales performance detail</li>
 * </ul>
 * </p>
 */
@Component
@Scope("step")
public class SalesItemProcessor implements ItemProcessor<SalesPlanDetail, SalesDTO> {

    /**
     * Processed count.
     */
    private int count = 0;

    @Value("#{jobParameters['errorCount'] ?: 0}")
    private int errorCount;

    /**
     * Execute business logic
     * 
     * @param item input sales plan detail.
     * @return SalesDTO with sales plan detail and slaes performance detail.
     */
    @Override
    public SalesDTO process(SalesPlanDetail item) throws Exception {
        count++;
        SalesDTO salesDTO = new SalesDTO();
        SalesPerformanceDetail spd = new SalesPerformanceDetail();
        spd.setBranchId(item.getBranchId());
        spd.setYear(item.getYear());
        spd.setMonth(item.getMonth());
        spd.setCustomerId(item.getCustomerId());
        if (count == errorCount) {
            spd.setAmount(new BigDecimal(12345678901L));
        } else {
            spd.setAmount(new BigDecimal(0L));
        }
        salesDTO.setSalesPerformanceDetail(spd);
        item.setAmount(item.getAmount().add(new BigDecimal(1L)));
        salesDTO.setSalesPlanDetail(item);
        return salesDTO;
    }
}
