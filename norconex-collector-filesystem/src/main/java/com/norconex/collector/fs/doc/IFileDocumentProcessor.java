/* Copyright 2014 Norconex Inc.
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
package com.norconex.collector.fs.doc;

import java.io.Serializable;

import org.apache.commons.vfs2.FileSystemManager;

/**
 * Custom processing (optional) performed on a document.  Can be used 
 * just before of after a document has been imported.  
 * @author Pascal Essiembre
 */
public interface IFileDocumentProcessor extends Serializable {

	/**
	 * Processes a document.
	 * @param fileManager file system manager
	 * @param doc the document
	 */
    void processDocument(FileSystemManager fileManager,  FileDocument doc);
}
