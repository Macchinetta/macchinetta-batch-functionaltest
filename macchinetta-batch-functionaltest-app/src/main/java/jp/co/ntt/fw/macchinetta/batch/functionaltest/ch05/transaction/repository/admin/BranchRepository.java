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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.transaction.repository.admin;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.mst.Branch;

import java.util.List;

/**
 * Repository of Branch master.
 *
 * @since 5.0.0
 */
@Component("adminBranchRepository")
public interface BranchRepository {

    /**
     * Find one branch master data.
     *
     * @param branchId Branch id.
     * @return A branch master data.
     */
    Branch findOne(@Param("branchId") String branchId);

    /**
     * Find All branch master data.
     *
     * @return All branch master data.
     */
    List<Branch> findAll();

    /**
     * Create a branch.
     *
     * @param branch New branch.
     */
    void create(Branch branch);
}
