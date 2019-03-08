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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.asyncjobwithdb;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.performance.Invoice;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.repository.performance.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Single transaction task.
 *
 * @since 2.0.1
 */
@Component
public class SingleTranTask implements Tasklet {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(SingleTranTask.class);

    /**
     * Invoice repository.
     */
    @Inject
    InvoiceRepository invoiceRepository;

    /**
     * Create invoices in a single transaction.
     *
     * @param contribution Step contribution.
     * @param chunkContext Chunk context.
     * @return Repeat status (FINISHED).
     * @throws Exception Exception that occurred.
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        Random random = new Random();

        for (int i = 0; i < 1000; i++) {
            Invoice invoice = new Invoice();
            String invoiceNo = String.format("invoice-%04d", i + 1);
            invoice.setInvoiceNo(invoiceNo);
            invoice.setInvoiceDate(new Date());
            invoice.setInvoiceAmount(new BigDecimal(random.nextInt(10000000)));
            invoice.setCustomerId("dummy");
            invoiceRepository.create(invoice);

            // Emulate another process.
            TimeUnit.MILLISECONDS.sleep(500L);

            // logging.
            logger.debug("Invoice added. [invoice no:{}]", invoiceNo);
        }

        return RepeatStatus.FINISHED;
    }
}
