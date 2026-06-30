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
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.norconex.collector.fs.FilesystemCollector;
import com.norconex.collector.fs.FilesystemCollectorConfig;
import com.norconex.collector.fs.crawler.FilesystemCrawlerConfig;
import com.norconex.committer.core3.UpsertRequest;
import com.norconex.committer.core3.fs.impl.JSONFileCommitter;

public class CmisFileSystemTest {

    private static final String COMMITTER_SUBDIR = "committedFiles";
    private static final String XML_FATAL_LINE = "[Fatal Error] :1:1: Content is not allowed in prolog.";

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
        runWithFilteredStderr(collector::start);

        File committerDir = new File(workdir, COMMITTER_SUBDIR);

        File[] files = committerDir.listFiles();
        Assertions.assertTrue(files != null && files.length > 0,
                "CMIS test produced no committed output in this runtime."
                        + " Workdir files: " + listPaths(workdir));
        Assertions.assertEquals(
                1, files.length, "There should be only 1 committed file.");

        String content = FileUtils.readFileToString(
                files[0], StandardCharsets.UTF_8);
        Assertions.assertEquals(
                expectedQty,
                StringUtils.countMatches(content, "\"upsert\""),
                "Invalid number of committed entries.");
    }

    private FilesystemCollectorConfig createCollectorConfig(
            String path, File workdir)
            throws IOException {

        String endpointURL = url(path);

        JSONFileCommitter committer = new MetadataOnlyJsonCommitter();
        committer.setDirectory(new File(workdir, COMMITTER_SUBDIR).toPath());

        FilesystemCrawlerConfig crawlerCfg = new FilesystemCrawlerConfig();
        crawlerCfg.setId("test-cmis-crawler");
        crawlerCfg.setDocumentChecksummer(null);
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

    private String listPaths(File baseDir) {
        if (baseDir == null || !baseDir.exists()) {
            return "<missing>";
        }
        Collection<File> files = FileUtils.listFiles(baseDir, null, true);
        if (files.isEmpty()) {
            return "<empty>";
        }
        StringBuilder sb = new StringBuilder();
        for (File f : files) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(baseDir.toPath().relativize(f.toPath()));
        }
        return sb.toString();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static void runWithFilteredStderr(ThrowingRunnable action)
            throws IOException {
        PrintStream originalErr = System.err;
        java.io.ByteArrayOutputStream captured = new java.io.ByteArrayOutputStream();
        try (PrintStream tempErr = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
            System.setErr(tempErr);
            try {
                action.run();
            } catch (Exception e) {
                throw new IOException("Could not start collector.", e);
            }
        } finally {
            System.setErr(originalErr);
        }

        String errText = captured.toString(Charset.defaultCharset());
        for (String line : errText.split("\\R")) {
            if (!line.contains(XML_FATAL_LINE) && !line.isBlank()) {
                originalErr.println(line);
            }
        }
    }

    private static class MetadataOnlyJsonCommitter extends JSONFileCommitter {
        @Override
        protected synchronized void writeUpsert(
                Writer writer, UpsertRequest upsertRequest) throws IOException {

            if (!first) {
                writer.write(',');
                newLine(writer);
            }

            JSONObject doc = new JSONObject();
            doc.put("reference", upsertRequest.getReference());
            doc.put("metadata", new JSONObject(upsertRequest.getMetadata()));

            JSONObject upsertObj = new JSONObject();
            upsertObj.put("upsert", doc);
            if (getIndent() > -1) {
                writer.write(upsertObj.toString(getIndent()));
            } else {
                writer.write(upsertObj.toString());
            }

            first = false;
        }

        private boolean first = true;

        private void newLine(Writer writer) throws IOException {
            if (getIndent() > -1) {
                writer.write('\n');
            }
        }
    }
}
