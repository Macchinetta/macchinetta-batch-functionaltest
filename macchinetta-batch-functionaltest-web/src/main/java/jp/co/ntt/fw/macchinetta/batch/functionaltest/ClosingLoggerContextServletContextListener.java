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
package jp.co.ntt.fw.macchinetta.batch.functionaltest;

import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Stop LoggerContext in order to prevent executor-thread leak after destroying the ServletContext.
 */
public class ClosingLoggerContextServletContextListener implements ServletContextListener {

    /**
     * {@inheritDoc} Do noting in this class.
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Do nothing.
    }

    /**
     * Stop logger context.
     *
     * @param sce destroyed event of ServletContext
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();
    }
}
