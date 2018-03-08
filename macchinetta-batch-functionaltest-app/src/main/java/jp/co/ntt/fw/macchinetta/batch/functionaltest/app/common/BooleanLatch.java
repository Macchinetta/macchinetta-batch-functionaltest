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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * This is boolean latch enabled to switch signal for await and release. The difference from below description is to enable to
 * change 'await' status again.
 *
 * @see java.util.concurrent.locks.AbstractQueuedSynchronizer <table>
 *      <tr>
 *      <th>status</th>
 *      <th>thread condition</th>
 *      </tr>
 *      <tr>
 *      <td>initial</td>
 *      <td>Change to WAIT status by calling await() in the thread.</td>
 *      </tr>
 *      <tr>
 *      <td>await</td>
 *      <td>Similar to initial status.</td>
 *      </tr>
 *      <tr>
 *      <td>signalled</td>
 *      <td>Change to RUNNING at all await threads, by calling signal() in other thread.</td>
 *      </tr>
 *      <tr>
 *      <td>timeout</td>
 *      <td>By calling awaitWithTimeout(), change to WAIT status till timeout and to RUNNING after timeout in the thread. (but
 *      'status' never changes.)</td>
 *      </tr>
 *      </table>
 */
public class BooleanLatch {

    private static final BooleanLatch INSTANCE = new BooleanLatch();

    private BooleanLatch() {
        // avoid new instantiation.
    }

    public static BooleanLatch getInstance() {
        return INSTANCE;
    }

    private static class Sync extends AbstractQueuedSynchronizer {
        boolean isSignalled() {
            return getState() == 1;
        }

        @Override
        protected int tryAcquireShared(int ignore) {
            return isSignalled() ? 1 : -1;
        }

        void setModeAwait() {
            setState(0);
        }

        @Override
        protected boolean tryReleaseShared(int arg) {
            setState(1);
            return true;
        }

        void setModeRelease() {
            releaseShared(1);
        }
    }

    private final Sync sync = new Sync();

    public boolean isSignalled() {
        return sync.isSignalled();
    }

    public void signal() {
        sync.setModeRelease();
    }

    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(-1); // argument doesn't make sense.
    }

    /**
     * Await thread till timeout.
     *
     * @param timeUnit timeUnit
     * @param timeout timeout value (long)
     * @return true: when signalled by other thread till timeout, false: after timeout.
     * @throws InterruptedException interrupted in the thread.
     */
    public boolean awaitWithTimeout(TimeUnit timeUnit, long timeout) throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, timeUnit.toNanos(timeout)); // argument doesn't make sense.
    }

    /**
     * Reset to initial(await) mode.
     */
    public void initial() {
        sync.setModeAwait();
    }
}
