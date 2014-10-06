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