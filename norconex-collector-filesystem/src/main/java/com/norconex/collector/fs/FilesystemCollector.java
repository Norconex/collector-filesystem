/* Copyright 2013-2014 Norconex Inc.
 * 
 * This file is part of Norconex Filesystem Collector.
 * 
 * Norconex Filesystem Collector is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Filesystem Collector is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Filesystem Collector. If not, 
 * see <http://www.gnu.org/licenses/>.
 */
package com.norconex.collector.fs;

import com.norconex.collector.core.AbstractCollector;
import com.norconex.collector.core.AbstractCollectorConfig;
import com.norconex.collector.core.AbstractCollectorLauncher;
import com.norconex.collector.core.CollectorConfigLoader;
import com.norconex.collector.core.ICollector;
import com.norconex.collector.core.ICollectorConfig;
import com.norconex.collector.core.crawler.ICrawler;
import com.norconex.collector.core.crawler.ICrawlerConfig;
import com.norconex.collector.fs.crawler.FilesystemCrawler;
import com.norconex.collector.fs.crawler.FilesystemCrawlerConfig;
 
/**
 * Main application class. In order to use it properly, you must first configure
 * it, either by providing a populated instance of 
 * {@link FilesystemCollectorConfig},
 * or by XML configuration, loaded using {@link CollectorConfigLoader}.
 * Instances of this class can hold several crawler, running at once.
 * This is convenient when there are configuration setting to be shared amongst
 * crawlers.  When you have many crawler jobs defined that have nothing
 * in common, it may be best to configure and run them separately, to facilitate
 * troubleshooting.  There is no fair rule for this, experimentation 
 * will help you.
 * @author Pascal Dimassimo
 * @author Pascal Essiembre
 */
@SuppressWarnings("nls")
public class FilesystemCollector extends AbstractCollector {

    /**
     * Creates a non-configured Filesystem collector.
     */
    public FilesystemCollector() {
        super(new FilesystemCollectorConfig());
    }
    /**
     * Creates and configure an HTTP Collector with the provided
     * configuration.
     * @param collectorConfig HTTP Collector configuration
     */
    public FilesystemCollector(FilesystemCollectorConfig collectorConfig) {
        super(collectorConfig);
    }

    /**
     * Invokes the Filesystem Collector from the command line.  
     * @param args Invoke it once without any arguments to get a 
     *    list of command-line options.
     */
    public static void main(String[] args) {
        new AbstractCollectorLauncher() {
            @Override
            protected ICollector createCollector(
                    ICollectorConfig config) {
                return new FilesystemCollector(
                        (FilesystemCollectorConfig) config);
            }
            @Override
            protected Class<? extends AbstractCollectorConfig> 
                    getCollectorConfigClass() {
                return FilesystemCollectorConfig.class;
            }
        }.launch(args);
    }

    @Override
    public FilesystemCollectorConfig getCollectorConfig() {
        return (FilesystemCollectorConfig) super.getCollectorConfig();
    }
    
    @Override
    protected ICrawler createCrawler(ICrawlerConfig config) {
        return new FilesystemCrawler((FilesystemCrawlerConfig) config);
    }
}
