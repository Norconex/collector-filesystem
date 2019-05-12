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
import java.util.List;

import org.apache.chemistry.opencmis.client.api.DocumentType;
import org.apache.chemistry.opencmis.client.api.FolderType;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.vfs2.FileName;
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

//    private CmisObject cmisObject;
    private ObjectData data;

    protected CmisFileObject(AbstractFileName name, CmisFileSystem fileSystem) {
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
//        if (cmisObject == null) {
//            cmisObject = createCmisObject(getName());
//        }
        if (data == null) {
            data = createObjectData(getName());
        }
    }

    @Override
    protected void doDetach() throws Exception {
        // file closed through content-streams
//        cmisObject = null;
        data = null;
    }

    @Override
    public CmisFileSystem getFileSystem() {
        return (CmisFileSystem) super.getFileSystem();
    }

    public ObjectData getData() {
        return data;
    }
//    public CmisObject getCmisObject() {
//        return cmisObject;
//    }

    private ObjectData createObjectData(final FileName fileName) {
        String repoId = getSession().getRepositoryInfo().getId();
        OperationContext oc = getFileSystem().getOperationContext();
        ObjectData data = getSession().getBinding().getObjectService()
                .getObjectByPath(repoId, fileName.getPath(),
                oc.getFilterString(),
                oc.isIncludeAllowableActions(),
                oc.getIncludeRelationships(),
                oc.getRenditionFilterString(),
                oc.isIncludePolicies(),
                oc.isIncludeAcls(),
                null);
        return data;
    }

    // remove and merge with doAttach() instead?
//    private CmisObject createCmisObject(final FileName fileName) {
//
//
//START TEST
//        String repoId = getSession().getRepositoryInfo().getId();
//        OperationContext oc = getFileSystem().getOperationContext();
//
//        ObjectData data = getSession().getBinding().getObjectService().getObjectByPath(
//                repoId, fileName.getPath(),
//                oc.getFilterString(), oc.isIncludeAllowableActions(), oc.getIncludeRelationships(),
//                oc.getRenditionFilterString(), oc.isIncludePolicies(), oc.isIncludeAcls(), null);
//
//
////        final DocumentImpl doc = (DocumentImpl) ctx.cmisObject;
////      JSONConverter.convert()
////      doc.get
//      try {//(InputStream is = doc.getContentStream().getStream()) {
//          StringWriter out = new StringWriter();
//          EnhancedXMLStreamWriter xml = new EnhancedXMLStreamWriter(out);
//          XMLConverter.writeObject(xml, CmisVersion.CMIS_1_1, true, "testXXX", "myNamespace", data);
//
//          //TODO figureout version dynamically (chosing 1.1 if both are aupported.
//          //TODO add a <propertyNameType> accepting "propertyDefinitionId", "displayName", "localName"
//          // default should have collector.blah for all manually extracted stuff
//          // the rest should be stored as is...
//          //TODO add ability to prefix properties (default being as is)???
//          //TODO have option to store whole XML in a property for custom manipulation (E.g. DOMTagger).
//
//
////          for (ExtensionLevel lvl : ExtensionLevel.values()) {
////              final ExtensionLevel level = lvl;
////              XMLConverter.writeExtensions(xml, new ExtensionsData() {
////                  @Override
////                  public void setExtensions(List<CmisExtensionElement> extensions) {
////                      // TODO Auto-generated method stub
////                  }
////
////                  @Override
////                  public List<CmisExtensionElement> getExtensions() {
////                      return doc.getExtensions(level);
////                  }
////              });
////
////          }
////          LOG.info("XML: ");
////          LOG.info("-------------");
////          LOG.info(out.toString());
//
//          xml.flush();
//          xml.close();
//
//          File testFile = new File("C:\\Workspaces\\eclipse\\penske\\test\\filesystem-tests\\workdir\\shane-02\\xml-" + FileUtil.toSafeFileName(data.getId()) + ".xml");
//          try (BufferedWriter writer = new BufferedWriter(new FileWriter(testFile, false))) {
//              writer.write(out.toString());
//          } catch(Exception e) {
//              e.printStackTrace();
//          }
//
//          //add(ctx, null, "XXXXXXXXXXXXXXXXXML", out.toString());
//
//
//      } catch (XMLStreamException e) {
//          // TODO Auto-generated catch block
//          e.printStackTrace();
//      }

//        JSONConverter.convert(object, typeCache, propertyMode, succinct, dateTimeFormat)
//
//
////STOP TEST
//
//
//
//
//
//
//
//
//        return getSession().getObjectByPath(
//                fileName.getPath(), getOperationContext());
//    }

    /**
     * Determines the type of the file, returns null if the file does not exist.
     */
    @Override
    protected FileType doGetType() throws Exception {
        if (data == null) {
            return null;
        }
        ObjectType type =
                getSession().getObjectFactory().getTypeFromObjectData(data);

        if (type instanceof DocumentType) {
            return FileType.FILE;
        }
        if (type instanceof FolderType) {
            return FileType.FOLDER;
        }


        String name = "";
        PropertyData<?> p = data.getProperties().getProperties().get(PropertyIds.NAME);
        if (p != null) {
            name = (String) p.getFirstValue();
        }

        LOG.info("Unsupported type: " + type + " Object: " + name);
        return FileType.IMAGINARY;


//        if (cmisObject == null) {
//            return null;
//        }
//        ObjectType type = cmisObject.getType();
//        if (type instanceof DocumentType) {
//            return FileType.FILE;
//        }
//        if (type instanceof FolderType) {
//            return FileType.FOLDER;
//        }
//        LOG.info("Unsupported type: " + type
//                + " Object: " + cmisObject.getName());
//        return FileType.IMAGINARY;
    }

    /**
     * Lists the children of the file. Is only called if {@link #doGetType}
     * returns {@link FileType#FOLDER}.
     */
    @Override
    protected String[] doListChildren() throws Exception {
        List<String> children = new ArrayList<>();
//        for (CmisObject childObject :
//                ((Folder) cmisObject).getChildren(getOperationContext())) {
//            children.add(childObject.getName());
//        }
        return children.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
    }

    /**
     * Returns the size of the file content (in bytes).
     */
    @Override
    protected long doGetContentSize() throws Exception {
//        if (cmisObject instanceof Document) {
//            return ((Document) cmisObject).getContentStreamLength();
//        }
        return -1;
    }

    /**
     * Returns the last modified time of this file.
     */
    @Override
    protected long doGetLastModifiedTime() throws Exception {
        return getPropertyValue(PropertyIds.LAST_MODIFICATION_DATE);
  //      PropertyIds.LAST_MODIFICATION_DATE
//        data.getProperties().getProperties().get("blah").
//        if (cmisObject != null) {
//            GregorianCalendar cal = cmisObject.getLastModificationDate();
//            if (cal != null) {
//                return cal.getTimeInMillis();
//            }
//        }
//        return -1;
    }

    /**
     * Creates an input stream to read the file content from.
     */
    @Override
    protected InputStream doGetInputStream() throws Exception {
//        if (cmisObject instanceof Document) {
//            return ((Document) cmisObject).getContentStream().getStream();
//        }
        throw null;
    }


//
//    @Override
//    public ContentStream getContentStream() {
//        return getContentStream(null, null, null);
//    }
//
//    @Override
//    public ContentStream getContentStream(BigInteger offset, BigInteger length) {
//        return getContentStream(null, offset, length);
//    }
//
//    @Override
//    public ContentStream getContentStream(String streamId) {
//        return getContentStream(streamId, null, null);
//    }
//
//    @Override
//    public ContentStream getContentStream(String streamId, BigInteger offset, BigInteger length) {
//        // get the stream
//        ContentStream contentStream = getSession().getContentStream(this, streamId, offset, length);
//
//        if (contentStream == null) {
//            return null;
//        }
//
//        // the AtomPub binding doesn't return a file name
//        // -> get the file name from properties, if present
//        String filename = contentStream.getFileName();
//        if (filename == null) {
//            filename = getContentStreamFileName();
//        }
//
//        long lengthLong = (contentStream.getBigLength() == null ? -1 : contentStream.getBigLength().longValue());
//
//        // convert and return stream object
//        return getSession().getObjectFactory().createContentStream(filename, lengthLong, contentStream.getMimeType(),
//                contentStream.getStream(), contentStream instanceof PartialContentStream);
//    }



//    /**
//     * random access
//     */
//    @Override
//    protected RandomAccessContent doGetRandomAccessContent(
//            final RandomAccessMode mode) throws Exception {
//        return new SmbFileRandomAccessContent(file, mode);
//    }

    @SuppressWarnings("unchecked")
    private <T> PropertyData<T> getPropertyData(String id) {
        return (PropertyData<T>) data.getProperties().getProperties().get(id);
    }

    @SuppressWarnings("unchecked")
    private <T> T getPropertyValue(String id) {
        PropertyData<T> property = getPropertyData(id);
        if (property == null) {
            return null;
        }
        // explicit cast needed by the Sun compiler
        return property.getFirstValue();
    }
}
