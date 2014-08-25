package com.norconex.collector.fs.crawler;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CrawlFileTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void test_reference_with_anchor() throws Exception {
        
        String name = "TOR01-#4728186-v1-DM_Stat_-_Autonomy_server_estimate.XLS";
        File file = new File(tempFolder.getRoot(), name);
        
        FileSystemManager manager = VFS.getManager();
        FileObject fo = manager.resolveFile(file.toString());
        CrawlFile crawlFile = new CrawlFile(fo);
        
        assertEquals(name, FilenameUtils.getName(crawlFile.getReference()));
    }

}
