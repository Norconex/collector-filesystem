/* Copyright 2017-2018 Norconex Inc.
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
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.collector.core.crawler.CrawlerConfig;
import com.norconex.collector.core.filter.impl.RegexMetadataFilter;
import com.norconex.collector.fs.crawler.FilesystemCrawlerConfig;
import com.norconex.collector.fs.crawler.MockStartPathsProvider;
import com.norconex.collector.fs.option.impl.GenericFilesystemOptionsProvider;
import com.norconex.committer.core3.impl.MemoryCommitter;
import com.norconex.commons.lang.xml.XML;
import com.norconex.commons.lang.xml.XMLValidationException;

/**
 * @author Pascal Essiembre
 */
public class FilesystemCollectorConfigTest {

    @Test
    public void testWriteRead() throws IOException {
        FilesystemCollectorConfig config = new FilesystemCollectorConfig();
        config.setId("test-fs-collector");

        FilesystemCrawlerConfig crawlerCfg = new FilesystemCrawlerConfig();
        crawlerCfg.setId("myCrawler");
        crawlerCfg.setOptionsProvider(new GenericFilesystemOptionsProvider());
        crawlerCfg.setPathsFiles(new String[] { "/path/file1", "/path/file2" });
        crawlerCfg.setCommitters(new MemoryCommitter());
        crawlerCfg.setStartPaths(new String[] { "/start/file1", "/start/file2" });
        RegexMetadataFilter metaFilter = new RegexMetadataFilter();
        crawlerCfg.setStartPathsProviders(new MockStartPathsProvider());
        metaFilter.setField("field");
        metaFilter.setRegex("potato");
        crawlerCfg.setMetadataFilters(metaFilter);
        config.setCrawlerConfigs(new CrawlerConfig[] { crawlerCfg });

        XML xml = new XML("fscollector");
        config.saveToXML(xml);
        String rendered = xml.toString();
        Assertions.assertTrue(rendered.contains("test-fs-collector"));
        Assertions.assertTrue(rendered.contains("myCrawler"));
    }

    @Test
    public void testValidation() throws IOException {
        try (Reader r = new InputStreamReader(getClass().getResourceAsStream(
                "/validation/collector-fs-full.xml"))) {
            XML xml = new XML(r);
            Assertions.assertNotNull(xml.getString("@id", null));
        } catch (XMLValidationException e) {
            Assertions.fail(e.getErrors().size()
                    + " validation warnings/errors were found.");
        }
    }

    // test for: https://github.com/Norconex/collector-filesystem/issues/29
    @SuppressWarnings("unchecked")
    @Test
    public void testNulls() throws IOException {
        FilesystemCollectorConfig config = new FilesystemCollectorConfig();
        config.setId("test-fs-collector");

        FilesystemCrawlerConfig crawlerCfg = new FilesystemCrawlerConfig();
        crawlerCfg.setDataStoreEngine(null);
        crawlerCfg.setCommitters(Collections.emptyList());
        crawlerCfg.setEventListeners(Collections.emptyList());
        crawlerCfg.setDocumentChecksummer(null);
        crawlerCfg.setDocumentFetcher(null);
        crawlerCfg.setDocumentFilters(Collections.emptyList());
        crawlerCfg.setId("testing Nulls");
        crawlerCfg.setImporterConfig(null);
        crawlerCfg.setStartPaths(null);
        crawlerCfg.setStartPathsProviders();
        crawlerCfg.setMetadataChecksummer(null);
        crawlerCfg.setMetadataFetcher(null);
        crawlerCfg.setMetadataFilters(Collections.emptyList());
        crawlerCfg.setOptionsProvider(null);
        crawlerCfg.setOrphansStrategy(null);
        crawlerCfg.setPostImportProcessors(null);
        crawlerCfg.setPathsFiles(null);
        crawlerCfg.setPreImportProcessors(null);
        crawlerCfg.setReferenceFilters(Collections.emptyList());
        crawlerCfg.setSpoiledReferenceStrategizer(null);
        crawlerCfg.setStartPaths(null);
        crawlerCfg.setStartPathsProviders();
        crawlerCfg.setStopOnExceptions(Collections.emptyList());
        config.setCrawlerConfigs(crawlerCfg);

        // Should not throw NPE.
        Assertions.assertDoesNotThrow(() -> config.saveToXML(new XML("fscollector")));
    }
}
