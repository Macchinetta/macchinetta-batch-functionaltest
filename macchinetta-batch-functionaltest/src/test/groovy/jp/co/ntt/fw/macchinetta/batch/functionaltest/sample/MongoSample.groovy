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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.sample

import org.slf4j.LoggerFactory
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.LogCondition
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.LogCursor
import jp.co.ntt.fw.macchinetta.batch.functionaltest.util.MongoUtil

class MongoSample {
    public static void main(String[] args) {
        // connect to MongoDB
        def mongo = new MongoUtil()
        // for primitive operation
        def col = mongo.col

        // insert
        // each one
        col.insert([name: 'AAA', type: 'X1'])
        col.insert(name: 'BBB', type: 'X1')
        col << [name: 'CCC', type: 'X2']
        // bulk
        col << [
                [name: 'DDD', type: 'Z1'],
                [name: 'EEE', type: 'Z1']
        ]


        // find
        // select
        mongo.find().each { println("find => $it") }
        // select where
        mongo.find(type: 'Z1').each { println("find(condition) => $it") }
        // select like
        mongo.find(type: ~/1/).each { println("find(condition) => $it") }

        // findOne
        println('findOne => ' + mongo.findOne())
        println('findOne(condition) => ' + mongo.findOne(name: 'AAA'))

        // count
        println('count => ' + mongo.count())
        println('count(condition) => ' + mongo.count(name: 'DDD', type: 'Z1'))

        // update
        // $set
        col.update([name: 'AAA'], [$set: [url: 'http://www.aaa.org/']])
        // $unset
        col.update([name: 'BBB'], [$unset: [type: 1]])

        // save
        col.save(name: 'FFF', type: 'X2')
        col.findOne(name: 'EEE').with {
            col.save(it << [url: 'http://eee.io/'])
        }

        // remove
        col.remove(name: 'CCC')

        col.find().each { println(it) }

        // truncate
        mongo.deleteAll()


        def logger = LoggerFactory.getLogger(MongoSample)
        logger.info('*** INFO LOG')
        logger.warn('### WARN LOG')
        logger.error('+++ ERROR LOG')

        def mapByTestno = mongo.find(new LogCondition(message:~/LOG/))

        assert mapByTestno.size() == 3

        Iterator<LogCursor> iterator = mapByTestno.iterator()
        def cursor = iterator.next()

        assert cursor.message == '*** INFO LOG'
        assert cursor.level == 'INFO'

        cursor = iterator.next()

        assert cursor.message == '### WARN LOG'
        assert cursor.level == 'WARN'

        cursor = iterator.next()

        assert cursor.message == '+++ ERROR LOG'
        assert cursor.level == 'ERROR'

        def cursorFindOne = mongo.findOne(new LogCondition(message: ~/ERROR LOG/))

        assert cursorFindOne.level == 'ERROR'
        assert cursorFindOne.message == '+++ ERROR LOG'

        def count = mongo.count(new LogCondition())

        assert count == 3L

        new Thread([run : {
            sleep(3000L)
            logger.info("%%% wait and info")
        }] as Runnable).start()

        def result = mongo.waitForOutputLog(5000L, new LogCondition(message: ~/wait and info/))

        assert result.level == 'INFO'
        assert result.message == '%%% wait and info'

        // truncate
        mongo.deleteAll()

        // assert log for throwables
        try {
            CallStacktrace.call()
        } catch (Exception e) {
            logger.error('exception occurs.', e)
        }

        def throwableCursor = mongo.findOne(new LogCondition(level: 'ERROR'))

        assert throwableCursor.throwable._class == IllegalArgumentException
        assert throwableCursor.throwable.message == 'illegalArgument message.'
        assert throwableCursor.throwable.stackTrace.size() > 0
        assert throwableCursor.throwable.cause._class == IllegalStateException
        assert throwableCursor.throwable.cause.message == 'illegalState message.'
        assert throwableCursor.throwable.stackTrace.size() > 0
        assert throwableCursor.throwable.cause.cause == null

        mongo.deleteAll()

        // destroy
        mongo.close()
    }

    static class CallStacktrace {
        static call() {
            try {
                innerCall()
            } catch (Exception e) {
                throw new IllegalArgumentException('illegalArgument message.', e)
            }
        }
        static innerCall() {
            throw new IllegalStateException("illegalState message.")
        }
    }
}
