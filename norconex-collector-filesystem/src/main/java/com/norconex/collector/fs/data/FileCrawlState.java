/* Copyright 2013-2014 Norconex Inc.
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
package com.norconex.collector.fs.data;

import com.norconex.collector.core.data.CrawlState;


/**
 * Represents a URL crawling status.
 * @author Pascal Essiembre
 */
public class FileCrawlState extends CrawlState { 

    private static final long serialVersionUID = 7360283251973474053L;

    public static final FileCrawlState DELETED = 
            new FileCrawlState("DELETED");
    public static final FileCrawlState NOT_FOUND =
            new FileCrawlState("NOT_FOUND");
    public static final FileCrawlState BAD_STATUS = 
            new FileCrawlState("BAD_STATUS");
    
    protected FileCrawlState(String state) {
        super(state);
    }

}