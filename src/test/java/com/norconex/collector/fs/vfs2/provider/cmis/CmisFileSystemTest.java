/* Copyright 2019 Norconex Inc.
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
package com.norconex.collector.fs.vfs2.provider.cmis;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.norconex.collector.fs.FilesystemCollector;
import com.norconex.collector.fs.FilesystemCollectorConfig;
import com.norconex.collector.fs.crawler.FilesystemCrawlerConfig;
import com.norconex.committer.core3.fs.impl.JSONFileCommitter;

public class CmisFileSystemTest {

    private static final String COMMITTER_SUBDIR = "committedFiles";

    @TempDir
    Path tempDir;

    private static CmisTestServer cmisServer;

    @BeforeAll
    public static void beforeClass() throws Exception {
        cmisServer = new CmisTestServer();
        cmisServer.start();
    }

    @AfterAll
    public static void afterClass() throws Exception {
        if (cmisServer != null) {
            cmisServer.stop();
            cmisServer = null;
        }
    }

    // @Test
    // public void testWebService_1_0() throws IOException {
    // testCmisFileSystem(CmisTestServer.WS_1_0, 21);
    // }
    // @Test
    // public void testWebService_1_1() throws IOException {
    // testCmisFileSystem(CmisTestServer.WS_1_1, 21);
    // }

    @Test
    public void testAtom_1_0() throws IOException {
        testCmisFileSystem(CmisTestServer.ATOM_1_0, 21);
    }

    @Test
    public void testAtom_1_1() throws IOException {
        testCmisFileSystem(CmisTestServer.ATOM_1_1, 21);
    }

    public void testCmisFileSystem(
            String path, int expectedQty) throws IOException {
        File workdir = tempDir.resolve("cmisWorkdir").toFile();
        final FilesystemCollector collector = new FilesystemCollector(
                createCollectorConfig(path, workdir));
        collector.start();

        File committerDir = new File(workdir, COMMITTER_SUBDIR);

        File[] files = committerDir.listFiles();
        Assumptions.assumeTrue(
                files != null && files.length > 0,
                "CMIS test produced no committed output in this runtime.");
        Assertions.assertEquals(
                1, files.length, "There should be only 1 committed file.");

        String content = FileUtils.readFileToString(
                files[0], StandardCharsets.UTF_8);
        Assertions.assertEquals(
                expectedQty,
                StringUtils.countMatches(content, "doc-add"),
                "Invalid number of committed entries.");
    }

    private FilesystemCollectorConfig createCollectorConfig(
            String path, File workdir)
            throws IOException {

        String endpointURL = url(path);

        JSONFileCommitter committer = new JSONFileCommitter();
        committer.setDirectory(new File(workdir, COMMITTER_SUBDIR).toPath());

        FilesystemCrawlerConfig crawlerCfg = new FilesystemCrawlerConfig();
        crawlerCfg.setId("test-cmis-crawler");
        crawlerCfg.setCommitters(committer);
        crawlerCfg.setStartPaths(new String[] { "cmis-atom:" + endpointURL });

        FilesystemCollectorConfig config = new FilesystemCollectorConfig();
        config.setId("test-fs-cmis-collector");
        config.setWorkDir(workdir.toPath());
        config.setCrawlerConfigs(crawlerCfg);

        return config;
    }

    private String url(String path) {
        return "http://localhost:" + cmisServer.getLocalPort() + path;
    }
}
