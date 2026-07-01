/* Copyright 2013-2017 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.collector.fs;

import com.norconex.collector.core.Collector;
import com.norconex.collector.core.cmdline.CollectorCommandLauncher;
import com.norconex.collector.core.crawler.Crawler;
import com.norconex.collector.core.crawler.CrawlerConfig;
import com.norconex.collector.fs.crawler.FilesystemCrawler;
import com.norconex.collector.fs.crawler.FilesystemCrawlerConfig;

/**
 * Main application class. In order to use it properly, you must first configure
 * it, either by providing a populated instance of
 * {@link FilesystemCollectorConfig},
 * or by XML configuration, loaded using a collector config loader.
 * Instances of this class can hold several crawler, running at once.
 * This is convenient when there are configuration setting to be shared amongst
 * crawlers. When you have many crawler jobs defined that have nothing
 * in common, it may be best to configure and run them separately, to facilitate
 * troubleshooting. There is no fair rule for this, experimentation
 * will help you.
 * 
 * @author Pascal Essiembre
 */
public class FilesystemCollector extends Collector {

    /**
     * Creates a non-configured Filesystem collector.
     */
    public FilesystemCollector() {
        super(new FilesystemCollectorConfig());
    }

    /**
     * Creates and configure an HTTP Collector with the provided
     * configuration.
     * 
     * @param collectorConfig HTTP Collector configuration
     */
    public FilesystemCollector(FilesystemCollectorConfig collectorConfig) {
        super(collectorConfig);
    }

    /**
     * Invokes the Filesystem Collector from the command line.
     * 
     * @param args Invoke it once without any arguments to get a
     *             list of command-line options.
     */
    public static void main(String[] args) {
        FilesystemCollector collector = new FilesystemCollector();
        new CollectorCommandLauncher().launch(collector, args);
    }

    @Override
    public FilesystemCollectorConfig getCollectorConfig() {
        return (FilesystemCollectorConfig) super.getCollectorConfig();
    }

    @Override
    protected Crawler createCrawler(CrawlerConfig config) {
        return new FilesystemCrawler((FilesystemCrawlerConfig) config, this);
    }
}
