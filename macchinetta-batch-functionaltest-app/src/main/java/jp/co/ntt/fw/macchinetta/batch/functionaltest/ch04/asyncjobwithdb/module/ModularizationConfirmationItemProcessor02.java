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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.asyncjobwithdb.module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

/**
 * Item processor for confirmation of modularization.
 *
 * @since 5.0.0
 */
public class ModularizationConfirmationItemProcessor02 implements ItemProcessor<Object, Object> {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(ModularizationConfirmationItemProcessor02.class);

    /**
     * {@inheritDoc}
     *
     * @param item {@inheritDoc}
     * @return {@inheritDoc}
     * @throws Exception {@inheritDoc}
     */
    @Override
    public Object process(Object item) throws Exception {

        logger.info("Processing on ModularizationConfirmationItemProcessor02.");

        return item;
    }
}
