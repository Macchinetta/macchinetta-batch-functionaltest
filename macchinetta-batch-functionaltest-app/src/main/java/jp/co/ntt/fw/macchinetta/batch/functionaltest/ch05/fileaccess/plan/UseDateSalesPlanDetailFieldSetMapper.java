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

import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.exception.IncorrectFieldFormatException;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.model.plan.UseDateSalesPlanDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Filed set mapper for sales plan detail.
 *
 * @since 5.0.0
 */
@Component
public class UseDateSalesPlanDetailFieldSetMapper implements FieldSetMapper<UseDateSalesPlanDetail> {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(UseDateSalesPlanDetailFieldSetMapper.class);

    /**
     * {@inheritDoc}
     *
     * @param fieldSet {@inheritDoc}
     * @return Sales performance detail.
     * @throws BindException {@inheritDoc}
     */
    @Override
    public UseDateSalesPlanDetail mapFieldSet(FieldSet fieldSet) throws BindException {
        logger.info("FieldSet : {" + "branchId='" + fieldSet.readString("branchId") + "', " + "date='"
                + fieldSet.readString("date") + "', " + "customerId='" + fieldSet.readString("customerId") + "', "
                + "amount='" + fieldSet.readString("amount") + "'}");

        UseDateSalesPlanDetail item = new UseDateSalesPlanDetail();

        item.setBranchId(fieldSet.readString("branchId"));

        DateFormat japaneseFormat = new SimpleDateFormat("GGGGy年M月d日", new Locale("ja", "JP", "JP"));
        try {
            item.setDate(japaneseFormat.parse(fieldSet.readString("date")));
        } catch (ParseException e) {
            throw new IncorrectFieldFormatException("The format of field date is invalid. " + "[value:"
                    + fieldSet.readString("date") + "]", e);
        }

        item.setCustomerId(fieldSet.readString("customerId"));

        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setParseBigDecimal(true);
        try {
            item.setAmount((BigDecimal) decimalFormat.parse(fieldSet.readString("amount")));
        } catch (ParseException e) {
            throw new IncorrectFieldFormatException("The format of field amount is invalid. " + "[value:"
                    + fieldSet.readString("date") + "]", e);
        }

        return item;
    }
}
