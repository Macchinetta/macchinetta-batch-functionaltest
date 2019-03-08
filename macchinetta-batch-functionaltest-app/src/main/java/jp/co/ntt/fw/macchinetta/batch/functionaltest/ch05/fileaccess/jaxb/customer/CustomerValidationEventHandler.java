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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.ch05.fileaccess.jaxb.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

/**
 * ValidationEventHandler that handle errors during validation.
 *
 * @since 2.0.1
 */
@Component
public class CustomerValidationEventHandler implements ValidationEventHandler {
    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(CustomerValidationEventHandler.class);

    @Override
    public boolean handleEvent(ValidationEvent event) {
        logger.error("[EVENT [SEVERITY:{}] [MESSAGE:{}] [LINKED EXCEPTION:{}] [LOCATOR: "
                + "[LINE NUMBER:{}] [COLUMN NUMBER:{}] [OFFSET:{}] [OBJECT:{}] [NODE:{}] [URL:{}] ] ]", event
                .getSeverity(), event.getMessage(), event.getLinkedException(), event.getLocator().getLineNumber(),
                event.getLocator().getColumnNumber(), event.getLocator().getOffset(), event.getLocator().getObject(),
                event.getLocator().getNode(), event.getLocator().getURL());
        return false;
    }
}
