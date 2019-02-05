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

import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemOptions;

public class CmisFileSystemConfigBuilder extends FileSystemConfigBuilder {
    private static final CmisFileSystemConfigBuilder INSTANCE =
            new CmisFileSystemConfigBuilder();

    public static CmisFileSystemConfigBuilder getInstance() {
        return INSTANCE;
    }

    public void setSessionParam(
            FileSystemOptions opts, String paramName, String paramValue) {
        setParam(opts, paramName, paramValue);
    }

    public String getSessionParam(FileSystemOptions opts, String paramName) {
        return (String) getParam(opts, paramName);
    }


    public void setAtomURL(
            FileSystemOptions opts, String atomUrl) {
        setParam(opts, SessionParameter.ATOMPUB_URL, atomUrl);
    }

    public String getAtomURL(FileSystemOptions opts) {
        return (String) getParam(opts, SessionParameter.ATOMPUB_URL);
    }


    @Override
    protected Class<CmisFileSystem> getConfigClass() {
        return CmisFileSystem.class;
    }
}
