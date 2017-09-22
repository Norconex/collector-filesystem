/* Copyright 2017 Norconex Inc.
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

import java.io.IOException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.norconex.collector.core.crawler.ICrawlerConfig;
import com.norconex.collector.fs.crawler.FilesystemCrawlerConfig;
import com.norconex.committer.core.impl.NilCommitter;


/**
 * @author Pascal Essiembre
 */
public class FilesystemCollectorTest {

    private static final Logger LOG = 
            LogManager.getLogger(FilesystemCollectorTest.class);
    
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();    
    
    @Test
    public void testRerunInJVM() throws IOException, InterruptedException {
        FilesystemCrawlerConfig crawlerCfg = new FilesystemCrawlerConfig();
        crawlerCfg.setId("test-crawler");
        crawlerCfg.setCommitter(new NilCommitter());
        crawlerCfg.setStartPaths(new String[] {"src/site/resources/examples"});
        crawlerCfg.setWorkDir(folder.newFolder("multiRunTest"));
        
        FilesystemCollectorConfig config = new FilesystemCollectorConfig();
        config.setId("test-fs-collector");
        config.setLogsDir(crawlerCfg.getWorkDir().getAbsolutePath());
        config.setProgressDir(crawlerCfg.getWorkDir().getAbsolutePath());
        config.setCrawlerConfigs(new ICrawlerConfig[] {crawlerCfg});

        final FilesystemCollector collector = new FilesystemCollector(config);
        collector.start(false);
        LOG.debug("First normal run complete.");

        collector.start(false);
        LOG.debug("Second normal run complete.");
    }
}
