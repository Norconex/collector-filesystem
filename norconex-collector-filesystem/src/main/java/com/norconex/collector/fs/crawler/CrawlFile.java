package com.norconex.collector.fs.crawler;

import java.net.URL;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;

import com.norconex.collector.core.ref.IReference;
import com.norconex.collector.fs.FilesystemCollectorException;

public class CrawlFile implements IReference {

    private static final long serialVersionUID = -5253640985191107355L;

    private transient FileObject fileObject;
    private CrawlStatus status;
    private String metadataChecksum;
    private String docChecksum;

    public CrawlFile(FileObject fileObjectVFS) {
        this.fileObject = fileObjectVFS;
    }

    public FileObject getFileObject() {
        return fileObject;
    }

    public void setFileObject(FileObject fileObject) {
        this.fileObject = fileObject;
    }

    public boolean isFile() {
        try {
            return getFileObject().getType() == FileType.FILE;
        } catch (FileSystemException e) {
            throw new FilesystemCollectorException(e);
        }
    }
    public boolean isFolder() {
        try {
            return getFileObject().getType() == FileType.FOLDER;
        } catch (FileSystemException e) {
            throw new FilesystemCollectorException(e);
        }
    }
    public FileObject[] listFiles() {
        try {
            return getFileObject().getChildren();
        } catch (FileSystemException e) {
            throw new FilesystemCollectorException(e);
        }
    }

    public URL getURL() {
        try {
            return getFileObject().getURL();
        } catch (FileSystemException e) {
            throw new FilesystemCollectorException(e);
        }
    }

    @Override
    public String getReference() {
        return getURL().toString();
    }
    
    public FileName getName() {
        return getFileObject().getName();
    }
    
    public FileContent getContent() {
        try {
            return getFileObject().getContent();
        } catch (FileSystemException e) {
            throw new FilesystemCollectorException(e);
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof CrawlFile)) {
            return false;
        }
        CrawlFile castOther = (CrawlFile) other;
        return new EqualsBuilder().append(fileObject,
                castOther.fileObject).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(fileObject).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE).append(
                "fileObject", fileObject).toString();
    }
    
    @Override
    public CrawlFile clone() {
        CrawlFile crawlFile = new CrawlFile(fileObject);
        return crawlFile;
    }
    
    /**
     * Gets the current crawl status.
     * @return crawl status
     */
    public CrawlStatus getStatus() {
        return status;
    }
    /**
     * Sets the current crawl status.
     * @param status crawl status
     */
    public void setStatus(CrawlStatus status) {
        this.status = status;
    }

    public String getMetadataChecksum() {
        return metadataChecksum;
    }

    public void setMetadataChecksum(String metadataChecksum) {
        this.metadataChecksum = metadataChecksum;
    }

    public String getDocChecksum() {
        return docChecksum;
    }

    public void setDocChecksum(String docChecksum) {
        this.docChecksum = docChecksum;
    }
}
