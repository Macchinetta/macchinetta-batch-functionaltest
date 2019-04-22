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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.plan;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.exception.IncorrectCharsetNameException;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.exception.IncorrectFieldLengthException;
import org.springframework.batch.item.file.transform.FieldExtractor;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;

import java.io.UnsupportedEncodingException;

/**
 * Filed extractor for sales plan detail.
 *
 * @since 5.0.0
 */
public class SalesPlanFixedLengthFieldExtractor implements FieldExtractor<SalesPlanDetail> {
    @Override
    public Object[] extract(SalesPlanDetail item) {
        Object[] values = new Object[5];

        values[0] = fillUpSpace(item.getBranchId(), 6);
        values[1] = item.getYear();
        values[2] = item.getMonth();
        values[3] = fillUpSpace(item.getCustomerId(), 10);
        values[4] = item.getAmount();

        return values;
    }

    private String fillUpSpace(String val, int num) {
        String charsetName = "MS932";
        int len;
        try {
            len = val.getBytes(charsetName).length;
        } catch (UnsupportedEncodingException e) {
            throw new IncorrectCharsetNameException("The specified encoding is not supported. [charsetName:"
                    + charsetName + "]", e);
        }

        if (len > num) {
            throw new IncorrectFieldLengthException("The length of field is invalid. " + "[value:" + val + "][length:"
                    + len + "][expect length:" + num + "]");
        }

        if (num == len) {
            return val;
        }

        StringBuilder filledVal = new StringBuilder();
        for (int i = 0; i < (num - len); i++) {
            filledVal.append(" ");
        }
        filledVal.append(val);

        return filledVal.toString();

    }

}
