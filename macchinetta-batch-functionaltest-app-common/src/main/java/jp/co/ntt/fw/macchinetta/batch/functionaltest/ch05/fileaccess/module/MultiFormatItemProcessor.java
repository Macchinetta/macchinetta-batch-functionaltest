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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.module;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.exception.IncorrectRecordClassificationException;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.model.plan.SalesPlanDetailMultiFormatRecord;

import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Item processro for multi layout file.
 *
 * @since 2.0.1
 */
public class MultiFormatItemProcessor implements ItemProcessor<SalesPlanDetailMultiFormatRecord, String> {

    @Inject
    @Named("headerDelimitedLineAggregator")
    DelimitedLineAggregator<SalesPlanDetailMultiFormatRecord> headerDelimitedLineAggregator;

    @Inject
    @Named("dataDelimitedLineAggregator")
    DelimitedLineAggregator<SalesPlanDetailMultiFormatRecord> dataDelimitedLineAggregator;

    @Inject
    @Named("trailerDelimitedLineAggregator")
    DelimitedLineAggregator<SalesPlanDetailMultiFormatRecord> trailerDelimitedLineAggregator;

    @Inject
    @Named("endDelimitedLineAggregator")
    DelimitedLineAggregator<SalesPlanDetailMultiFormatRecord> endDelimitedLineAggregator;

    @Override
    public String process(SalesPlanDetailMultiFormatRecord item) throws Exception {
        String record = item.getRecord();

        // edit item
        item.setRecord(item.getRecord() + item.getRecord());

        switch (record) {
        case "H":
            return headerDelimitedLineAggregator.aggregate(item);
        case "D":
            return dataDelimitedLineAggregator.aggregate(item);
        case "T":
            return trailerDelimitedLineAggregator.aggregate(item);
        case "E":
            return endDelimitedLineAggregator.aggregate(item);
        default:
            throw new IncorrectRecordClassificationException("Record classification is incorrect.[value:" + record
                    + "]");
        }
    }
}
