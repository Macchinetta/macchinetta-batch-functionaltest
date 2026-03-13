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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.util

import java.time.LocalDateTime
import java.time.ZoneId

class LogCursor {

    String _id
    String level
    String message
    String logger
    String thread
    LocalDateTime timestamp
    ThrowableLog throwable

    static class ThrowableLog {
        Class _class
        String message
        List<StackTraceElement> stackTrace
        ThrowableLog cause
    }

    LogCursor(Map map) {
        this._id = map._id
        this.level = map.level
        this.message = map.message
        this.logger = map.logger
        this.thread = map.thread
        Date date = map.timestamp as Date
        this.timestamp = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        this.throwable = map.throwable as ThrowableLog
    }
}
