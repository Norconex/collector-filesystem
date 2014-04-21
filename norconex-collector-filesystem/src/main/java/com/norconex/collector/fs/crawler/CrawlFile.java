package com.norconex.collector.fs.crawler;

import java.io.Serializable;
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

import com.norconex.collector.fs.FilesystemCollectorException;

public class CrawlFile implements Serializable {

    private static final long serialVersionUID = -5253640985191107355L;

    private FileObject fileObjectVFS;

    public CrawlFile(FileObject fileObjectVFS) {
        this.fileObjectVFS = fileObjectVFS;
    }

    public FileObject getFileObjectVFS() {
        return fileObjectVFS;
    }

    public void setFileObjectVFS(FileObject fileObjectVFS) {
        this.fileObjectVFS = fileObjectVFS;
    }

    public boolean isFile() {
        try {
            return getFileObjectVFS().getType() == FileType.FILE;
        } catch (FileSystemException e) {
            throw new FilesystemCollectorException(e);
        }
    }
    public boolean isFolder() {
        try {
            return getFileObjectVFS().getType() == FileType.FOLDER;
        } catch (FileSystemException e) {
            throw new FilesystemCollectorException(e);
        }
    }
    public FileObject[] listFiles() {
        try {
            return getFileObjectVFS().getChildren();
        } catch (FileSystemException e) {
            throw new FilesystemCollectorException(e);
        }
    }

    public URL getURL() {
        try {
            return getFileObjectVFS().getURL();
        } catch (FileSystemException e) {
            throw new FilesystemCollectorException(e);
        }
    }

    public FileName getName() {
        return getFileObjectVFS().getName();
    }
    
    public FileContent getContent() {
        try {
            return getFileObjectVFS().getContent();
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
        return new EqualsBuilder().append(fileObjectVFS,
                castOther.fileObjectVFS).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(fileObjectVFS).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE).append(
                "fileObjectVFS", fileObjectVFS).toString();
    }
}
