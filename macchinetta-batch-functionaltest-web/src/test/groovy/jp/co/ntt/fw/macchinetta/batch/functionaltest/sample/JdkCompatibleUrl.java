/*
 * Copyright (C) 2024 NTT Corporation
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
package jp.co.ntt.fw.macchinetta.batch.functionaltest.sample;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * This is a harness class to maintain compatibility with URL classes
 * whose API specifications have been changed since JDK20.
 */
public class JdkCompatibleUrl {

    private static final int URL_SPEC_CHANGE_VERSION = 20;

    /**
     * Create a URL object from the specified string.
     * NOTE: The behavior of this method depends on the JDK version.
     * Because the URL class has been changed since JDK20.
     *
     * @param urlString presentation of URL
     * @return URL object
     */
    public URL createUrl(String urlString) {
        if (determineJdkMajorVersion() < URL_SPEC_CHANGE_VERSION) {
            return createUrlLessThanJdk20(urlString);
        } else {
            return createUrlOrMoreJdk20(urlString);
        }
    }

    int determineJdkMajorVersion() {
        String version = System.getProperty("java.version");
        String[] versionParts = version.split("\\.");
        return Integer.parseInt(versionParts[0]);
    }

    URL createUrlLessThanJdk20(String urlString) {
        try {
            return ReflectionUtils
                    .accessibleConstructor(URL.class, String.class)
                    .newInstance(urlString);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    URL createUrlOrMoreJdk20(String urlString) {
        try {
            return URI.create(urlString).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}