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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.multiple;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.parallelandmultiple.repository.SalesSummaryRepository;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * Divide it into a certain number of data.
 *
 * @since 2.0.1
 */
@Component
@Scope("step")
public class SalesDataPartitioner implements Partitioner {

    /**
     * Repository Sales summary.
     */
    @Inject
    SalesSummaryRepository repository;

    /**
     * Year.
     */
    private int year;

    /**
     * Month
     */
    private int month;

    /**
     * Create a patition to divide it into a certain number of data.
     *
     * @param gridSize Division number.
     * @return Map of partitioner.
     */
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        Map<String, ExecutionContext> map = new HashMap<>();
        int count = repository.countByYearAndMonth(year, month);
        int dataSize = (count / gridSize) + 1;
        int offset = 0;

        for (int i = 0; i < gridSize; i++) {
            ExecutionContext context = new ExecutionContext();
            context.putInt("dataSize", dataSize);
            context.putInt("offset", offset);
            offset += dataSize;
            map.put("partition:" + i, context);
        }

        return map;
    }

    /**
     * year.
     *
     * @param year New year.
     */
    @Value("#{jobParameters['year']}")
    public void setYear(int year) {
        this.year = year;
    }

    /**
     * month.
     *
     * @param month New month.
     */
    @Value("#{jobParameters['month']}")
    public void setMonth(int month) {
        this.month = month;
    }
}
