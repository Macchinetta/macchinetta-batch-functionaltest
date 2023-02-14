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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.model.SalesPlanDetailWithProcessName;

/**
 * Add process name to sales plan details.
 *
 * @since 2.0.1
 */
@Component
public class AddProcessNameItemPorcessor implements ItemProcessor<SalesPlanDetail, SalesPlanDetailWithProcessName> {

    @Value("${processName:#{null}}")
    private String processName;

    /**
     * Add process name to sales plan details.
     *
     * @param item Sales plan detail before adding process name.
     * @return item Sales plan detail after adding process name.
     * @throws Exception {@inheritDoc}
     */
    @Override
    public SalesPlanDetailWithProcessName process(SalesPlanDetail item) throws Exception {
        SalesPlanDetailWithProcessName newItem = new SalesPlanDetailWithProcessName();
        newItem.setProcessName(processName);
        newItem.setPlan(item);
        return newItem;
    }
}
