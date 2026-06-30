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
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.collector.fs.crawler.FilesystemCrawlerConfig;
import com.norconex.committer.core3.fs.impl.JSONFileCommitter;

/**
 * @author Pascal Essiembre
 */
public class FilesystemCollectorTest {

    private static final Logger LOG = LoggerFactory.getLogger(FilesystemCollectorTest.class);

    @TempDir
    Path tempDir;

    @Test
    public void testRerunInJVM() throws IOException, InterruptedException {
        FilesystemCrawlerConfig crawlerCfg = new FilesystemCrawlerConfig();
        crawlerCfg.setId("test-crawler");
        JSONFileCommitter committer = new JSONFileCommitter();
        committer.setDirectory(tempDir.resolve("commits"));
        crawlerCfg.setCommitters(committer);
        crawlerCfg.setStartPaths(new String[] { "src/site/resources/examples" });

        FilesystemCollectorConfig config = new FilesystemCollectorConfig();
        config.setId("test-fs-collector");
        config.setWorkDir(tempDir);
        config.setCrawlerConfigs(crawlerCfg);

        new FilesystemCollector(config).start();
        LOG.debug("First normal run complete.");

        new FilesystemCollector(config).start();
        LOG.debug("Second normal run complete.");
    }
}
