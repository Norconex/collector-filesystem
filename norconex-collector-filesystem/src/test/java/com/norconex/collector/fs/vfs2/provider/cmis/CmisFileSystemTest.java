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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.norconex.collector.core.crawler.ICrawlerConfig;
import com.norconex.collector.fs.FilesystemCollector;
import com.norconex.collector.fs.FilesystemCollectorConfig;
import com.norconex.collector.fs.crawler.FilesystemCrawlerConfig;
import com.norconex.committer.core.impl.JSONFileCommitter;

public class CmisFileSystemTest {

    private static final String COMMITTER_SUBDIR = "committedFiles";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static CmisTestServer cmisServer;

    @BeforeClass
    public static void beforeClass() throws Exception {
        cmisServer = new CmisTestServer();
        cmisServer.start();
    }
    @AfterClass
    public static void afterClass() throws Exception {
        if (cmisServer != null) {
            cmisServer.stop();
            cmisServer = null;
        }
    }

//    @Test
//    public void testWebService_1_0() throws IOException {
//        testCmisFileSystem(CmisTestServer.WS_1_0, 21);
//    }
//    @Test
//    public void testWebService_1_1() throws IOException {
//        testCmisFileSystem(CmisTestServer.WS_1_1, 21);
//    }

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
        final FilesystemCollector collector = new FilesystemCollector(
                createCollectorConfig(path));
        collector.start(false);

        File committerDir = new File(collector.getCollectorConfig()
                .getCrawlerConfigs()[0].getWorkDir(), COMMITTER_SUBDIR);

        File[] files = committerDir.listFiles();
        Assert.assertEquals(
                "There should be only 1 committed file.", 1, files.length);

        String content = FileUtils.readFileToString(
                files[0], StandardCharsets.UTF_8);
        Assert.assertEquals(
                "Invalid number of committed entries.", expectedQty,
                StringUtils.countMatches(content, "doc-add"));
    }

    private FilesystemCollectorConfig createCollectorConfig(String path)
            throws IOException {

        String endpointURL = url(path);
        File workdir = folder.newFolder("cmisWorkdir");

        JSONFileCommitter committer = new JSONFileCommitter();
        committer.setDirectory(
                new File(workdir, COMMITTER_SUBDIR).getAbsolutePath());

        FilesystemCrawlerConfig crawlerCfg = new FilesystemCrawlerConfig();
        crawlerCfg.setId("test-cmis-crawler");
        crawlerCfg.setCommitter(committer);//new NilCommitter());
        crawlerCfg.setStartPaths(new String[] { "cmis-atom:" + endpointURL });
        crawlerCfg.setWorkDir(workdir);

        FilesystemCollectorConfig config = new FilesystemCollectorConfig();
        config.setId("test-fs-cmis-collector");
        config.setLogsDir(workdir.getAbsolutePath());
        config.setProgressDir(workdir.getAbsolutePath());
        config.setCrawlerConfigs(new ICrawlerConfig[] {crawlerCfg});


        return config;
    }

    private String url(String path) {
        return "http://localhost:" + cmisServer.getLocalPort() + path;
    }
}
