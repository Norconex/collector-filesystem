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

import java.util.Collection;

import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileSystem;

/**
 * An CMIS file system.
 */
public class CmisFileSystem extends AbstractFileSystem {

    private final Session session;
    private final OperationContext operationContext;

    protected CmisFileSystem(
            final FileName rootName,
            final FileObject parentLayer,
            final Session session,
            final OperationContext operationContext,
            final FileSystemOptions fileSystemOptions) {
        super(rootName, parentLayer, fileSystemOptions);
        this.session = session;
        this.operationContext = operationContext;
    }

    /**
     * Gets OpenCmis session.
     * @return session
     */
    public Session getSession() {
        return session;
    }
    /**
     * Gets OpenCmis operation context.
     * @return operation context
     */
    public OperationContext getOperationContext() {
        return operationContext;
    }


    /**
     * Creates a file object.
     */
    @Override
    protected FileObject createFile(final AbstractFileName name)
            throws FileSystemException {
        return new CmisFileObject(name, this);
    }

    /**
     * Returns the capabilities of this file system.
     */
    @Override
    protected void addCapabilities(final Collection<Capability> caps) {
        caps.addAll(CmisFileProvider.CAPABILITIES);
    }
}
