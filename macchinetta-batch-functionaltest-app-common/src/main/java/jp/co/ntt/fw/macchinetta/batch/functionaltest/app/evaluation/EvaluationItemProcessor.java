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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.app.evaluation;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.evaluation.EvaluationReport;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * ItemProcessor to evaluate sales performance.
 *
 * @since 2.0.1
 */
@Component
@Scope("step")
public class EvaluationItemProcessor implements ItemProcessor<EvaluationReport, EvaluationReport> {

    /**
     * Branch name.
     */
    private String branchName;

    /**
     * Evaluate sales performance.
     *
     * @param item Data before evaluation.
     * @return Data after evaluation
     * @throws Exception {@inheritDoc}
     */
    @Override
    public EvaluationReport process(EvaluationReport item) throws Exception {

        item.setBranchName(branchName);

        try {
            item.setRatio(item.getPerformanceAmount().divide(item.getPlanAmount()));
            double percentage = item.getRatio().doubleValue() * 100.0;
            if (percentage >= 100.0) {
                item.setEvaluation("A");
            } else if (percentage >= 90.0) {
                item.setEvaluation("B");
            } else if (percentage >= 80.0) {
                item.setEvaluation("C");
            } else if (percentage >= 70.0) {
                item.setEvaluation("D");
            } else {
                item.setEvaluation("E");
            }
        } catch (ArithmeticException e) {
            item.setEvaluation("-");
        }

        return item;
    }

    /**
     * Branch name setting.
     *
     * @param branchName Branch name in step execution context.
     */
    @Value("#{stepExecutionContext['branch'].branchName}")
    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }
}
