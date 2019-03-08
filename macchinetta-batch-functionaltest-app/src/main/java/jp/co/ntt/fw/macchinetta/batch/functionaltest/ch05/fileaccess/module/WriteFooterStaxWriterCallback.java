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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.module;

import org.springframework.batch.item.xml.StaxWriterCallback;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;

/**
 * StaxWriterCallback that writing footer record.
 *
 * @since 2.0.1
 */
@Component
public class WriteFooterStaxWriterCallback implements StaxWriterCallback {
    @Override
    public void write(XMLEventWriter writer) throws IOException {
        XMLEventFactory factory = XMLEventFactory.newInstance();
        try {
            writer.add(factory.createComment(" Customer list footer "));
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }
}
