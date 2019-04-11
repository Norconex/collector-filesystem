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

import java.util.Map;

import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemOptions;

public class CmisFileSystemConfigBuilder extends FileSystemConfigBuilder {
    private static final CmisFileSystemConfigBuilder INSTANCE =
            new CmisFileSystemConfigBuilder();

    private static final String PARAM_WS_URL =
            CmisFileSystemConfigBuilder.class.getName() + ".webservices.url";
    private static final String PARAM_SESSION_PARAMS =
            CmisFileSystemConfigBuilder.class.getName() + ".session.params";
    private static final String PARAM_PREFIX_FORMAT =
            CmisFileSystemConfigBuilder.class.getName() + ".prefixFormat";

    public enum PrefixFormat {
        FULL, COMPACT, NONE
    }

    public static CmisFileSystemConfigBuilder getInstance() {
        return INSTANCE;
    }

    public void setPrefixFormat(FileSystemOptions opts, PrefixFormat format) {
        setParam(opts, PARAM_PREFIX_FORMAT, format);
    }
    public PrefixFormat getPrefixFormat(FileSystemOptions opts) {
        return (PrefixFormat) getParam(opts, PARAM_PREFIX_FORMAT);
    }

    public void setSessionParam(
            FileSystemOptions opts, String paramName, String paramValue) {
        setParam(opts, paramName, paramValue);
    }
    public String getSessionParam(FileSystemOptions opts, String paramName) {
        return (String) getParam(opts, paramName);
    }

    public void setAtomURL(FileSystemOptions opts, String atomUrl) {
        setParam(opts, SessionParameter.ATOMPUB_URL, atomUrl);
    }
    public String getAtomURL(FileSystemOptions opts) {
        return (String) getParam(opts, SessionParameter.ATOMPUB_URL);
    }

    public void setWebServicesURL(FileSystemOptions opts, String wsUrl) {
        setParam(opts, PARAM_WS_URL, wsUrl);
    }
    public String getWebServicesURL(FileSystemOptions opts) {
        return (String) getParam(opts, PARAM_WS_URL);
    }

    public void setRepositoryId(FileSystemOptions opts, String repositoryId) {
        setParam(opts, SessionParameter.REPOSITORY_ID, repositoryId);
    }
    public String getRepositoryId(FileSystemOptions opts) {
        return (String) getParam(opts, SessionParameter.REPOSITORY_ID);
    }

    public void setSessionParams(
            FileSystemOptions opts, Map<String, String> params) {
        setParam(opts, PARAM_SESSION_PARAMS, params);
    }
    @SuppressWarnings("unchecked")
    public Map<String, String> getSessionParams(FileSystemOptions opts) {
        return (Map<String, String>) getParam(opts, PARAM_SESSION_PARAMS);
    }

    @Override
    protected Class<CmisFileSystem> getConfigClass() {
        return CmisFileSystem.class;
    }
}
