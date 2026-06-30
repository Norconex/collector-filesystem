/* Copyright 2026 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package com.norconex.commons.lang.config;

import java.io.Reader;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import com.norconex.collector.core.CollectorException;
import com.norconex.commons.lang.time.DurationParser;

/**
 * Backward-compatible XML configuration helpers used by legacy filesystem
 * option parsing.
 */
public final class XMLConfigurationUtil {

    private XMLConfigurationUtil() {
    }

    public static XMLConfiguration newXMLConfiguration(Reader in) {
        try {
            XMLConfiguration xml = new XMLConfiguration();
            xml.load(in);
            return xml;
        } catch (ConfigurationException e) {
            throw new CollectorException("Cannot load XML configuration.", e);
        }
    }

    public static String[] getCSVStringArray(
            XMLConfiguration xml, String key, String[] defaultValue) {
        String value = xml.getString(key, null);
        if (value == null) {
            return defaultValue;
        }
        if (value.trim().isEmpty()) {
            return new String[0];
        }
        String[] values = value.split("\\s*,\\s*");
        return values;
    }

    public static long getDuration(
            XMLConfiguration xml, String key, long defaultValue) {
        String value = xml.getString(key, null);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return new DurationParser().parse(value).toMillis();
    }
}
