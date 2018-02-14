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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.repository;

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Branch;
import org.apache.ibatis.annotations.Param;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.model.ExclusiveBranch;

import java.util.List;

/**
 * Repository of exclusive control.
 *
 * @since 5.0.0
 */
public interface ExclusiveControlRepository {

    /**
     * Find sales plan detail by branch_id.
     *
     * @param branchId Branch id.
     * @return Sales plan detail by branch_id.
     */
    List<SalesPlanDetail> planFindByBranch(@Param("branchId") String branchId);

    /**
     * Find one branch master data.
     *
     * @param branchId Branch id.
     * @return A branch master data.
     */
    Branch branchFindOne(@Param("branchId") String branchId);

    /**
     * Update branch master data.
     *
     * @param branch A branch master data.
     */
    int branchUpdate(Branch branch);

    /**
     * Update branch master data.
     * Update expecting exclusive control by optimistic lock.
     * </P>
     *
     * @param branch A branch master data.
     */
    int branchExclusiveUpdate(ExclusiveBranch branch);

    /**
     * Find one branch master data with NOWAIT lock.
     *
     * @param branchId Branch id.
     * @return A branch master data.
     */
    Branch branchFindOneWithNowWaitLock(@Param("branchId") String branchId);

    /**
     * Find all branch id by branch name.
     *
     * @return List of branch master data.
     */
    List<String> branchIdFindByName(@Param("branchName") String branchName);

    /**
     * Find one branch master data with NOWAIT lock.
     *
     * @param branchId   Branch id.
     * @param branchName Branch name.
     * @return A branch master data.
     */
    Branch branchFindOneByNameWithNowWaitLock(@Param("branchId") String branchId,
            @Param("branchName") String branchName);
}
