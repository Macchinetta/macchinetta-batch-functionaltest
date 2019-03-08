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

import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.model.plan.SalesPlanDetailFooter;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.batch.item.file.FlatFileFooterCallback;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

/**
 * FlatFileHeaderCallback that writing header record.
 *
 * @since 2.0.1
 */
@Component
@Scope("job")
public class WriteFooterFlatFileFooterCallback implements FlatFileFooterCallback {

    private JobExecution jobExecution;

    @BeforeJob
    public void beforeJob(JobExecution jobExecution) {
        this.jobExecution = jobExecution;
    }

    @Override
    public void writeFooter(Writer writer) throws IOException {
        @SuppressWarnings("unchecked")
        ArrayList<SalesPlanDetailFooter> footers = (ArrayList<SalesPlanDetailFooter>) this.jobExecution
                .getExecutionContext().get("footers");

        BufferedWriter bufferedWriter = new BufferedWriter(writer);
        for (SalesPlanDetailFooter footer : footers) {
            bufferedWriter.write(footer.getName() + " is " + footer.getValue());
            bufferedWriter.newLine();
            bufferedWriter.flush();
        }
    }
}
