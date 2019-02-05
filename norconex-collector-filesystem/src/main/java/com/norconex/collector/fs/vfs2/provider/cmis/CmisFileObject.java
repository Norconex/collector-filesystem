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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.DocumentType;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.FolderType;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * A file in an CMIS file system.
 */
public class CmisFileObject extends AbstractFileObject<CmisFileSystem> {

    private static final Logger LOG =
            LogManager.getLogger(CmisFileObject.class);

    private CmisObject cmisObject;

    protected CmisFileObject(AbstractFileName name, CmisFileSystem fileSystem,
            FileName rootName) throws FileSystemException {
        super(name, fileSystem);
    }

    public Session getSession() {
        return getFileSystem().getSession();
    }
    public OperationContext getOperationContext() {
        return getFileSystem().getOperationContext();
    }

    /**
     * Attaches this file object to its file resource.
     */
    @Override
    protected void doAttach() throws Exception {
        // Defer creation of the CmisObject to here
        if (cmisObject == null) {
            cmisObject = createCmisObject(getName());
        }
    }

    @Override
    protected void doDetach() throws Exception {
        // file closed through content-streams
        cmisObject = null;
    }

    @Override
    public CmisFileSystem getFileSystem() {
        return (CmisFileSystem) super.getFileSystem();
    }

    public CmisObject getCmisObject() {
        return cmisObject;
    }

    // remove and merge with doAttach() instead?
    private CmisObject createCmisObject(final FileName fileName) {
        return getSession().getObjectByPath(
                fileName.getPath(), getOperationContext());
    }

    /**
     * Determines the type of the file, returns null if the file does not exist.
     */
    @Override
    protected FileType doGetType() throws Exception {
        if (cmisObject == null) {
            return null;
        }
        ObjectType type = cmisObject.getType();
        if (type instanceof DocumentType) {
            return FileType.FILE;
        }
        if (type instanceof FolderType) {
            return FileType.FOLDER;
        }
        LOG.info("Unsupported type: " + type
                + " Object: " + cmisObject.getName());
        return FileType.IMAGINARY;
    }

    /**
     * Lists the children of the file. Is only called if {@link #doGetType}
     * returns {@link FileType#FOLDER}.
     */
    @Override
    protected String[] doListChildren() throws Exception {
        List<String> children = new ArrayList<>();
        for (CmisObject childObject :
                ((Folder) cmisObject).getChildren(getOperationContext())) {
            children.add(childObject.getName());
        }
        return children.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
    }

    /**
     * Returns the size of the file content (in bytes).
     */
    @Override
    protected long doGetContentSize() throws Exception {
        if (cmisObject instanceof Document) {
            return ((Document) cmisObject).getContentStreamLength();
        }
        return -1;
    }

    /**
     * Returns the last modified time of this file.
     */
    @Override
    protected long doGetLastModifiedTime() throws Exception {
        if (cmisObject != null) {
            GregorianCalendar cal = cmisObject.getLastModificationDate();
            if (cal != null) {
                return cal.getTimeInMillis();
            }
        }
        return -1;
    }

    /**
     * Creates an input stream to read the file content from.
     */
    @Override
    protected InputStream doGetInputStream() throws Exception {
        if (cmisObject instanceof Document) {
            return ((Document) cmisObject).getContentStream().getStream();
        }
        throw null;
    }

//    /**
//     * random access
//     */
//    @Override
//    protected RandomAccessContent doGetRandomAccessContent(
//            final RandomAccessMode mode) throws Exception {
//        return new SmbFileRandomAccessContent(file, mode);
//    }
}
