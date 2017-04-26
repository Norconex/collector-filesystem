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
package com.norconex.collector.fs.option;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemOptions;

import com.norconex.commons.lang.config.IXMLConfigurable;

/**
 * Provides Apache Commons VFS {@link FileSystemOptions} for a given 
 * {@link FileObject}. When possible, try to reuse options.
 * 
 * Implementors also implementing {@link IXMLConfigurable} must name their XML 
 * tag <code>optionsProvider</code> to ensure it gets loaded properly.
 * @since 2.7.0
 * @author Pascal Essiembre
 */
public interface IFilesystemOptionsProvider {

    /**
     * Provide file system options associated with a file object.
     * @param fileObject the file for which to get options
     * @return options
     */
	FileSystemOptions getFilesystemOptions(FileObject fileObject);
}
