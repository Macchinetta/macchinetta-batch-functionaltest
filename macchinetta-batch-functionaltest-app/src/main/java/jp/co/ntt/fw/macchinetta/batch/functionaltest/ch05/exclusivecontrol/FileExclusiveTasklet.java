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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.model.plan.SalesPlanDetail;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.exception.FailedAcquireLockException;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.exception.FailedOtherAcquireLockException;
import jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.exclusivecontrol.model.SalesPlanDetailWithProcessName;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tasklet for exclusive control of files.
 *
 * @since 2.0.1
 */
@Component
@Scope("step")
public class FileExclusiveTasklet implements Tasklet {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(FileExclusiveTasklet.class);

    /**
     * File path to check lock.
     */
    private String targetPath = null;

    @Value("${processName}")
    private String processName;

    /**
     * Reader.
     */
    @Inject
    ItemStreamReader<SalesPlanDetail> reader;

    /**
     * Writer.
     */
    @Inject
    ItemStreamWriter<SalesPlanDetailWithProcessName> writer;

    /**
     * Perform file locking control and output data.
     * <p>
     * If it is not possible to lock it because it is already locked by another process, throw an exception of
     * FailedAcquireLockException. If the lock does not get out otherwise, throw an exception of
     * FailedOtherAcquireLockException.
     * </p>
     *
     * @param contribution Step contribution.
     * @param chunkContext Chunk context.
     * @return RepeatStatus.FINISHED
     * @throws Exception See above description.
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        FileChannel fc = null;
        FileLock fileLock = null;

        try {
            // check file lock.
            try {
                File file = new File(targetPath);
                fc = FileChannel.open(file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
                fileLock = fc.tryLock();
            } catch (IOException e) {
                logger.error("Failure other than lock acquisition", e);
                throw new FailedOtherAcquireLockException("Failure other than lock acquisition", e);
            }
            if (fileLock == null) {
                logger.error("Failed to acquire lock. [processName={}]", processName);
                throw new FailedAcquireLockException("Failed to acquire lock");
            } else {
                logger.info("Acquire lock. [processName={}]", processName);

                // The intended code for the test.
                // Waits in locked state so that another process fails to acquire the lock.
                try {
                    TimeUnit.SECONDS.sleep(3L);
                } catch (InterruptedException e) {
                    logger.warn("Interrupt occurred");
                    throw e;
                }
            }

            reader.open(chunkContext.getStepContext().getStepExecution().getExecutionContext());
            writer.open(chunkContext.getStepContext().getStepExecution().getExecutionContext());

            SalesPlanDetail item;
            List<SalesPlanDetailWithProcessName> items = new ArrayList<>();
            while ((item = reader.read()) != null) {
                SalesPlanDetailWithProcessName newItem = new SalesPlanDetailWithProcessName();
                newItem.setProcessName(processName);
                newItem.setPlan(item);
                items.add(newItem);
                if (items.size() >= 10) {
                    writer.write(items);
                    items.clear();
                }
            }
            if (items.size() > 0) {
                writer.write(items);
            }

        } finally {
            if (fileLock != null) {
                try {
                    fileLock.release();
                } catch (IOException e) {
                    logger.warn("Lock release failed.", e);
                }
            }
            if (fc != null) {
                try {
                    fc.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            try {
                writer.close();
            } catch (ItemStreamException e) {
                // ignore
            }
            try {
                reader.close();
            } catch (ItemStreamException e) {
                // ignore
            }
        }

        return RepeatStatus.FINISHED;
    }

    /**
     * File path to check lock.
     *
     * @param targetPath New file path to check lock.
     */
    @Value("#{jobParameters['outputFile']}")
    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }
}
