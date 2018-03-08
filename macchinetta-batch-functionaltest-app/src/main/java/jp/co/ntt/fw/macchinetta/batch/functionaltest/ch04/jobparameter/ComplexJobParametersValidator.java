/*
 * Copyright (C) 2018 NTT Corporation
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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch04.jobparameter;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;

import java.util.Map;

/**
 * JobParametersValidator for correlation check
 *
 * @since 5.0.0
 */
public class ComplexJobParametersValidator implements JobParametersValidator {
    @Override
    public void validate(JobParameters parameters) throws JobParametersInvalidException {
        Map<String, JobParameter> params = parameters.getParameters();

        String str = params.get("str").getValue().toString();
        int num = Integer.parseInt(params.get("num").getValue().toString());

        if (str.length() > num) {
            throw new JobParametersInvalidException("The str must be less than or equal to num. [str:" + str + "][num:"
                    + num + "]");
        }
    }
}
