/* Copyright 2010-2014 Norconex Inc.
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
package com.norconex.collector.fs.db;

public class CrawlFileDatabaseException extends RuntimeException {

    
    private static final long serialVersionUID = 5416591514078326431L;

    public CrawlFileDatabaseException() {
        super();
    }

    public CrawlFileDatabaseException(String message) {
        super(message);
    }

    public CrawlFileDatabaseException(Throwable cause) {
        super(cause);
    }

    public CrawlFileDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

}
