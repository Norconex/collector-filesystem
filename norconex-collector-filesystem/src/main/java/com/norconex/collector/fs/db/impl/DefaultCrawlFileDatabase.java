package com.norconex.collector.fs.db.impl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.mapdb.Serializer;

import com.norconex.collector.core.ref.store.impl.mapdb.MapDBReferenceStore;
import com.norconex.collector.fs.crawler.CrawlFile;

public class DefaultCrawlFileDatabase extends MapDBReferenceStore<CrawlFile> {

    
    public DefaultCrawlFileDatabase(String path, boolean resume) {
        super(path, resume, new CrawlFileSerializer());
    }

    @Override
    protected boolean isVanished(CrawlFile currentReference,
            CrawlFile cachedReference) {
        return false;
    }

    @Override
    protected boolean isValid(CrawlFile reference) {
        return false;
    }



    static class CrawlFileSerializer 
            implements Serializer<CrawlFile>, Serializable {
        private static final long serialVersionUID = 5344110551013165128L;
        @Override
        public void serialize(DataOutput out, CrawlFile value)
                throws IOException {
            out.writeUTF(StringUtils.defaultString(value.getReference()));
            out.writeUTF(StringUtils.defaultString(value.getDocChecksum()));
            out.writeUTF(
                    StringUtils.defaultString(value.getMetadataChecksum()));
        }
        @Override
        public CrawlFile deserialize(DataInput in, int available)
                throws IOException {
            FileSystemManager manager = VFS.getManager();
            String reference = in.readUTF();
            FileObject fileObject = manager.resolveFile(reference);
            CrawlFile file = new CrawlFile(fileObject);
            file.setDocChecksum(StringUtils.defaultString(in.readUTF(), null));
            file.setMetadataChecksum(
                    StringUtils.defaultString(in.readUTF(), null));
            return file;
        }
        @Override
        public int fixedSize() {
            return -1;
        }
    }
}
