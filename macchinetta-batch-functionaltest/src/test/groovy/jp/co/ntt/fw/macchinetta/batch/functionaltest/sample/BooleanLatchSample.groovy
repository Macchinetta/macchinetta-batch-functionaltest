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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.sample

import jp.co.ntt.fw.macchinetta.batch.functionaltest.app.common.BooleanLatch

import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class BooleanLatchSample {

    final BooleanLatch booleanLatch = BooleanLatch.getInstance()
    final executor = Executors.newFixedThreadPool(10) as ThreadPoolExecutor

    static void main(String[] args) {
        new BooleanLatchSample().execute()
    }

    def execute() {
        try {
            doSignal()
        } finally {
            executor.shutdown()
        }
    }

    def doSignal() {
        assert !booleanLatch.isSignalled()
        def service = new ExecutorCompletionService(executor)
        10.times {
            service.submit {
                booleanLatch.await()
            }
        }
        Thread.sleep(5000L)
        assert executor.activeCount == 10

        println 'signal.'
        booleanLatch.signal()

        10.times {
            service.take().get() // wait for service compilation.
        }

        assert booleanLatch.isSignalled()
        assert executor.activeCount == 0
        assert executor.completedTaskCount == 10

        println 'reset to initial again.'
        booleanLatch.initial()
        assert !booleanLatch.isSignalled()

        10.times {
            service.submit {
                booleanLatch.awaitWithTimeout(TimeUnit.MILLISECONDS, 500L)
            }
        }

        println 'wait till timeout.'
        Thread.sleep(3000L)

        10.times {
            assert !service.take().get() // confirm to timeout.
        }

        assert executor.activeCount == 0
        assert executor.completedTaskCount == 20
        assert !booleanLatch.isSignalled() // not signalled, but timeout occurred.

        println 'reset to initial again.'
        booleanLatch.initial()
        assert !booleanLatch.isSignalled()

        10.times {
            service.submit {
                booleanLatch.awaitWithTimeout(TimeUnit.MILLISECONDS, 50000000L)
            }
        }

        for (;;) {
            if (executor.activeCount == 10) { // waiting for submit completely.
                break
            }
            Thread.sleep(50L)
        }

        println 'send signal suddenly.'
        booleanLatch.signal()

        10.times {
            assert service.take().get() // confirm to receive signal until timeout.
        }
        assert executor.activeCount == 0
        assert executor.completedTaskCount == 30
    }

}
