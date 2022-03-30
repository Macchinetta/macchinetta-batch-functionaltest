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

import org.apache.commons.io.FileUtils

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Utility for files.
 */
class FileUtil {

    /**
     * Copy UTF-8 file with specified charset.
     * @param target File to be copied.
     * @param charset Charset of copy file.
     * @return Copy file.
     */
    static File copyWithCharset(File target, Charset charset) {
        String copyFileName = charset.toString() + "_" + target.name
        File copyFile = new File(target.parent, copyFileName)
        String fileContents = FileUtils.readFileToString(target, StandardCharsets.UTF_8)
        FileUtils.write(copyFile, fileContents, charset)
        return copyFile
    }
}
