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
package com.norconex.collector.fs.vfs2.provider.cmis.atom;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * A file in an CMIS file system.
 */
public class CmisAtomFileObject extends AbstractFileObject<CmisAtomFileSystem> {

    private static final Logger LOG =
            LogManager.getLogger(CmisAtomFileObject.class);

    // no paging given Apache Commons VFS does not seem to support it.
    private static final int MAX_ITEMS = 1000000;

    private static final String PROP_OBJECT_TYPE_ID = "cmis:objectTypeId";
    private static final String PROP_BASE_TYPE_ID = "cmis:baseTypeId";
    private static final String PROP_LAST_MODIFICATION_DATE =
            "cmis:lastModificationDate";
    private static final String PROP_CONTENT_STREAM_LENGTH =
            "cmis:contentStreamLength";

    private Document document;

    protected CmisAtomFileObject(
            AbstractFileName name, CmisAtomFileSystem fileSystem) {
        super(name, fileSystem);
    }

    public CmisAtomSession getSession() {
        return getFileSystem().getSession();
    }

    /**
     * Attaches this file object to its file resource.
     */
    @Override
    protected void doAttach() throws Exception {
        // Defer creation of the CmisObject to here
        if (document == null) {
            document = createDocument(getName());
        }
    }

    @Override
    protected void doDetach() throws Exception {
        document = null;
    }

    @Override
    public CmisAtomFileSystem getFileSystem() {
        return (CmisAtomFileSystem) super.getFileSystem();
    }

    public Document getDocument() {
        return document;
    }

    private Document createDocument(final FileName fileName)
            throws FileSystemException {
        return getSession().getDocumentByPath(fileName.getPath());
    }

    public String toXmlString() {
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(document), new StreamResult(sw));
            return sw.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error converting to String", ex);
        }
    }

    /**
     * Determines the type of the file, returns null if the file does not exist.
     */
    @Override
    protected FileType doGetType() throws Exception {
        String type = getPropertyValue(PROP_OBJECT_TYPE_ID);
        if ("cmis:folder".equalsIgnoreCase(type)) {
            return FileType.FOLDER;
        }
        if ("cmis:document".equalsIgnoreCase(type)) {
            return FileType.FILE;
        }

        type = getPropertyValue(PROP_BASE_TYPE_ID);
        if ("cmis:folder".equalsIgnoreCase(type)) {
            return FileType.FOLDER;
        }
        if ("cmis:document".equalsIgnoreCase(type)) {
            return FileType.FILE;
        }

        LOG.info("Unsupported file type: " + type + " File: " + getName());
        return FileType.IMAGINARY;
    }

    /**
     * Lists the children of the file. Is only called if {@link #doGetType}
     * returns {@link FileType#FOLDER}.
     */
    @Override
    protected String[] doListChildren() throws Exception {
        CmisAtomSession session = getSession();

        List<String> children = new ArrayList<>();

        String childrenURL = session.getString(document,
                "/entry/link[@rel='down' and "
              + "@type='application/atom+xml;type=feed']/@href");

        if (StringUtils.isBlank(childrenURL)) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        childrenURL += childrenURL.contains("?") ? "&" : "?";
        childrenURL += "&includeAllowableActions=false&includeRelationships=none"
                + "&renditionFilter=cmis%3Anone&includePathSegment=true"
                + "&maxItems=" + MAX_ITEMS + "&skipCount=0&filter=cmis%3Anone";

        Document childrenDoc = session.getDocument(childrenURL);

        if (session.getInt(childrenDoc, "/feed/numItems", -1) > MAX_ITEMS) {
            LOG.warn("TOO many items under " + getName().getPathDecoded()
                    + ". Will only process the first " + MAX_ITEMS);
        }
        NodeList nl = session.getNodeList(
                childrenDoc, "/feed/entry/pathSegment/text()");
        for (int i = 0; i < nl.getLength(); i++) {
            String fileName = nl.item(i).getNodeValue();
            if (StringUtils.isNotBlank(fileName)) {
                children.add(fileName);
            }
//            Node pathSegment = nl.item(i);
//            System.out.println("PATH SEGMENT " + i + ": " + pathSegment.getNodeValue());
        }

//        <cmisra:numItems>11</cmisra:numItems>
//
//        System.out.println("CHILDREN XML:\n" + toString(childrenXML));

//        childrenURL += "filter=&orderBy


//System.out.println("CHILDREN URL: " + childrenURL);

////        for (CmisObject childObject :
////                ((Folder) cmisObject).getChildren(getOperationContext())) {
////            children.add(childObject.getName());
////        }
//
////        ObjectType.
//        ObjectType type =
//                getSession().getObjectFactory().getTypeFromObjectData(data);
//        Folder folder = new FolderImpl((SessionImpl) getSession(), type, data, getFileSystem().getOperationContext());
//
//        for (CmisObject childObject : folder.getChildren(getOperationContext())) {
//            children.add(childObject.getName());
//        }
        return children.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
    }

    /**
     * Returns the size of the file content (in bytes).
     */
    @Override
    protected long doGetContentSize() throws Exception {
        return NumberUtils.toLong(getPropertyValue(PROP_CONTENT_STREAM_LENGTH), -1);

//        String size = getPropertyValue(PROP_CONTENT_STREAM_LENGTH);
//        if (StringUtils.isNotEmpty(size)) {
//            return NumberUtils.toLong(str)
//        }
//
//
////        if (cmisObject instanceof Document) {
////            return ((Document) cmisObject).getContentStreamLength();
////        }
//        return -1;
////        BigInteger bigInt = getPropertyValue(PropertyIds.CONTENT_STREAM_LENGTH);
////        return bigInt == null ? (long) -1 : bigInt.longValue();
    }

    /**
     * Returns the last modified time of this file.
     */
    @Override
    protected long doGetLastModifiedTime() throws Exception {
        String date = getPropertyValue(PROP_LAST_MODIFICATION_DATE);
        if (StringUtils.isNotEmpty(date)) {
            return DateTime.parse(date).getMillis();
        }

//        GregorianCalendar cal = getPropertyValue(PropertyIds.LAST_MODIFICATION_DATE);
//        if (cal != null) {
//            return cal.getTimeInMillis();
//        }
        return -1;
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
        String contentURL = getSession().getString(document, "/entry/content/@src");
        if (StringUtils.isBlank(contentURL)) {
            LOG.warn("Content URL could not be found for " + getName());
            return null;
        }
        return getSession().getStream(contentURL);



        //   /entry/content
//        <atom:content src="https://alfrescodev.penske.com/alfresco/cmisatom/dfc849c1-1205-40bb-b2c3-e49f28c61524/content/2016_Observed_US_Holidays.pdf?id=workspace%3A%2F%2FSpacesStore%2Ff0fe6185-be62-4bd9-92bb-319cbf59647a%3B1.0" type="application/pdf"/>


//        if (cmisObject instanceof Document) {
//            return ((Document) cmisObject).getContentStream().getStream();
//        }
//        throw null;


        // get the stream
//        ContentStream contentStream =
//                getSession().getContentStreamByPath(getName().getPath());
//        if (contentStream == null) {
//            return null;
//        }
//        // the AtomPub binding doesn't return a file name
//        // -> get the file name from properties, if present
//        String filename = contentStream.getFileName();
//        if (filename == null) {
//            filename = getPropertyValue(PropertyIds.CONTENT_STREAM_FILE_NAME);
//        }
//
//        long lengthLong = (contentStream.getBigLength() == null ? -1 : contentStream.getBigLength().longValue());
//
//        // convert and return stream object
//        return getSession().getObjectFactory().createContentStream(filename, lengthLong, contentStream.getMimeType(),
//                contentStream.getStream(), contentStream instanceof PartialContentStream).getStream();
//
//
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

//    @SuppressWarnings("unchecked")
//    private <T> PropertyData<T> getPropertyData(String id) {
//        return (PropertyData<T>) data.getProperties().getProperties().get(id);
//    }
//
//    @SuppressWarnings("unchecked")
//    private <T> T getPropertyValue(String id) {
//        PropertyData<T> property = getPropertyData(id);
//        if (property == null) {
//            return null;
//        }
//        // explicit cast needed by the Sun compiler
//        return property.getFirstValue();
//    }

    private String getPropertyValue(String propertyDefId)
            throws FileSystemException {
        return getSession().getString(document, "/entry/object/properties/"
                + "*[starts-with(local-name(), 'property')]"
                + "[@propertyDefinitionId='"
                + propertyDefId + "']/value/text()");
    }

}
