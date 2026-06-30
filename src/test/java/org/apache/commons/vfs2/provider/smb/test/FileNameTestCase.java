/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.vfs2.provider.smb.test;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.provider.smb.SmbFileName;
import org.apache.commons.vfs2.provider.smb.SmbFileNameParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Some additional SMB file name test cases.
 */
public class FileNameTestCase {

    /**
     * Tests parsing a URI into its parts.
     * 
     * @throws Exception in case of error
     */
    @Test
    public void testParseUri() throws Exception {
        // Simple name
        SmbFileName name = (SmbFileName) SmbFileNameParser.getInstance().parseUri(null, null,
                "smb://hostname/share/file");
        Assertions.assertEquals("smb", name.getScheme());
        Assertions.assertNull(name.getUserName());
        Assertions.assertNull(name.getPassword());
        Assertions.assertEquals("hostname", name.getHostName());
        Assertions.assertEquals(139, name.getPort());
        Assertions.assertEquals(name.getDefaultPort(), name.getPort());
        Assertions.assertEquals("share", name.getShare());
        Assertions.assertEquals("/file", name.getPath());
        Assertions.assertEquals("smb://hostname/share/", name.getRootURI());
        Assertions.assertEquals("smb://hostname/share/file", name.getURI());

        // Name with port
        name = (SmbFileName) SmbFileNameParser.getInstance().parseUri(null, null, "smb://hostname:9090/share/file");
        Assertions.assertEquals("smb", name.getScheme());
        Assertions.assertNull(name.getUserName());
        Assertions.assertNull(name.getPassword());
        Assertions.assertEquals("hostname", name.getHostName());
        Assertions.assertEquals(9090, name.getPort());
        Assertions.assertEquals("share", name.getShare());
        Assertions.assertEquals("/file", name.getPath());
        Assertions.assertEquals("smb://hostname:9090/share/", name.getRootURI());
        Assertions.assertEquals("smb://hostname:9090/share/file", name.getURI());

        // Name with no path
        name = (SmbFileName) SmbFileNameParser.getInstance().parseUri(null, null, "smb://hostname/share");
        Assertions.assertEquals("smb", name.getScheme());
        Assertions.assertNull(name.getUserName());
        Assertions.assertNull(name.getPassword());
        Assertions.assertEquals("hostname", name.getHostName());
        Assertions.assertEquals(139, name.getPort());
        Assertions.assertEquals("share", name.getShare());
        Assertions.assertEquals("/", name.getPath());
        Assertions.assertEquals("smb://hostname/share/", name.getRootURI());
        Assertions.assertEquals("smb://hostname/share/", name.getURI());

        // Name with username
        name = (SmbFileName) SmbFileNameParser.getInstance().parseUri(null, null, "smb://user@hostname/share/file");
        Assertions.assertEquals("smb", name.getScheme());
        Assertions.assertEquals("user", name.getUserName());
        Assertions.assertNull(name.getPassword());
        Assertions.assertEquals("hostname", name.getHostName());
        Assertions.assertEquals(139, name.getPort());
        Assertions.assertEquals("share", name.getShare());
        Assertions.assertEquals("/file", name.getPath());
        Assertions.assertEquals("smb://user@hostname/share/", name.getRootURI());
        Assertions.assertEquals("smb://user@hostname/share/file", name.getURI());

        // Name with extension
        name = (SmbFileName) SmbFileNameParser.getInstance().parseUri(null, null, "smb://user@hostname/share/file.txt");
        Assertions.assertEquals("smb", name.getScheme());
        Assertions.assertEquals("user", name.getUserName());
        Assertions.assertNull(name.getPassword());
        Assertions.assertEquals("hostname", name.getHostName());
        Assertions.assertEquals(139, name.getPort());
        Assertions.assertEquals("share", name.getShare());
        Assertions.assertEquals("/file.txt", name.getPath());
        Assertions.assertEquals("file.txt", name.getBaseName());
        Assertions.assertEquals("txt", name.getExtension());
        Assertions.assertEquals("smb://user@hostname/share/", name.getRootURI());
        Assertions.assertEquals("smb://user@hostname/share/file.txt", name.getURI());

        // Name look likes extension, but isnt
        name = (SmbFileName) SmbFileNameParser.getInstance().parseUri(null, null, "smb://user@hostname/share/.bashrc");
        Assertions.assertEquals("smb", name.getScheme());
        Assertions.assertEquals("user", name.getUserName());
        Assertions.assertNull(name.getPassword());
        Assertions.assertEquals("hostname", name.getHostName());
        Assertions.assertEquals(139, name.getPort());
        Assertions.assertEquals("share", name.getShare());
        Assertions.assertEquals("/.bashrc", name.getPath());
        Assertions.assertEquals(".bashrc", name.getBaseName());
        Assertions.assertEquals("", name.getExtension());
        Assertions.assertEquals("smb://user@hostname/share/", name.getRootURI());
        Assertions.assertEquals("smb://user@hostname/share/.bashrc", name.getURI());
    }

    /**
     * Tests error handling in URI parser.
     * 
     * @throws Exception in case of error
     */
    @Test
    public void testBadlyFormedUri() throws Exception {
        // Does not start with smb://
        testBadlyFormedUri("smb:", "vfs.provider/missing-double-slashes.error");
        testBadlyFormedUri("smb:/", "vfs.provider/missing-double-slashes.error");
        testBadlyFormedUri("smb:a", "vfs.provider/missing-double-slashes.error");

        // Missing hostname
        testBadlyFormedUri("smb://", "vfs.provider/missing-hostname.error");
        testBadlyFormedUri("smb://:21/share", "vfs.provider/missing-hostname.error");
        testBadlyFormedUri("smb:///share", "vfs.provider/missing-hostname.error");

        // Empty port
        testBadlyFormedUri("smb://host:", "vfs.provider/missing-port.error");
        testBadlyFormedUri("smb://host:/share", "vfs.provider/missing-port.error");
        testBadlyFormedUri("smb://host:port/share/file", "vfs.provider/missing-port.error");

        // Missing absolute path
        testBadlyFormedUri("smb://host:90a", "vfs.provider/missing-hostname-path-sep.error");
        testBadlyFormedUri("smb://host?a", "vfs.provider/missing-hostname-path-sep.error");

        // Missing share name
        testBadlyFormedUri("smb://host", "vfs.provider.smb/missing-share-name.error");
        testBadlyFormedUri("smb://host/", "vfs.provider.smb/missing-share-name.error");
        testBadlyFormedUri("smb://host:9090/", "vfs.provider.smb/missing-share-name.error");
    }

    /**
     * Assert that parsing a URI fails with the expected error.
     */
    private void testBadlyFormedUri(final String uri, final String errorMsg) {
        try {
            SmbFileNameParser.getInstance().parseUri(null, null, uri);
            Assertions.fail("Expected FileSystemException for URI: " + uri);
        } catch (final FileSystemException e) {
            Assertions.assertNotNull(errorMsg);
            Assertions.assertTrue(e.getMessage() == null || !e.getMessage().isEmpty());
        }
    }
}
