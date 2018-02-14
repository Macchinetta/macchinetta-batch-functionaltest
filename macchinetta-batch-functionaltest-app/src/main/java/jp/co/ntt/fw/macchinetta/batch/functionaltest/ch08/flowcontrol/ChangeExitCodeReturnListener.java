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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch08.flowcontrol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;

/**
 * This listener returns exit code named by JobParameter. on condition that JobParameter has same step name to this step and
 * adopt parameter's value as exit code.
 */
public class ChangeExitCodeReturnListener extends StepExecutionListenerSupport {

    private static final Logger logger = LoggerFactory.getLogger(ChangeExitCodeReturnListener.class);

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String thisStepName = stepExecution.getStepName();
        String exitCode = stepExecution.getJobParameters().getString(thisStepName);
        if (exitCode == null) {
            logger.debug("return exit code normally (decided step execution).");
            return stepExecution.getExitStatus();
        }
        logger.debug("return custom exit code [exitCode={}]", exitCode);
        return new ExitStatus(exitCode);
    }
}
