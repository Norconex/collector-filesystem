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
package com.norconex.collector.fs.fetch;

import org.apache.commons.vfs2.FileObject;

import com.norconex.collector.core.data.CrawlState;
import com.norconex.commons.lang.map.Properties;

/**
 * Fetches a document metadata (e.g., document properties). 
 * @author Pascal Essiembre
 * @since 2.7.0
 */
public interface IFileMetadataFetcher {

    /**
     * Fetches the document metadata for a path and stores it in the 
     * provided {@link Properties}.
     * @param fileObject the file object representing the document
     * @param metadata recipient for storing metadata
     * @return crawl state
     */
    CrawlState fetchMetadada(FileObject fileObject, Properties metadata);
	
}
