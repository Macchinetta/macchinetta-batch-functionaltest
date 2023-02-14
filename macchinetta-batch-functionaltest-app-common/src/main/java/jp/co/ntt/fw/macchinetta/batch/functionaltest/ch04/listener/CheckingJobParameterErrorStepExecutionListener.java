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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.listener;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * StepExecutionListener checking job parameter error.
 *
 * @since 2.0.1
 */
@Component
@Scope("step")
public class CheckingJobParameterErrorStepExecutionListener implements StepExecutionListener {

    /**
     * InputFile.
     */
    @Value("#{jobParameters['inputFile']}")
    private File inputFile;

    /**
     * OutputFile.
     */
    @Value("#{jobParameters['outputFile']}")
    private File outputFile;

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeStep(StepExecution stepExecution) {
        if (inputFile == null) {
            throw new BeforeStepException("The input file must be not null.");
        }
        else if (outputFile == null) {
            throw new BeforeStepException("The output file must be not null.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        // do nothing.
        return null;
    }
}
