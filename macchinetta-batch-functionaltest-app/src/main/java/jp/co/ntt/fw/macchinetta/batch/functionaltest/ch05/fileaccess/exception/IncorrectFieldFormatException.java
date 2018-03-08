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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.exception;

import org.springframework.batch.item.file.transform.FlatFileFormatException;

/**
 * Exception indicating that an incorrect format field have been found while parsing a file.
 *
 * @since 5.0.0
 */
public class IncorrectFieldFormatException extends FlatFileFormatException {
    public IncorrectFieldFormatException(String mes, Throwable cause) {
        super(mes, cause);
    }
}
