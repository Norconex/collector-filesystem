/* Copyright 2017 Norconex Inc.
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
package com.norconex.collector.fs.option.impl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import org.apache.commons.vfs2.provider.ftp.FtpFileType;
import org.apache.hadoop.fs.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GenericFilesystemOptionsProviderTest {

    @Test
    public void testWriteRead() throws IOException {
        GenericFilesystemOptionsProvider p = new GenericFilesystemOptionsProvider();
        p.setAuthUsername("JoeUser");
        p.setAuthPassword("JoePassword");
        p.setAuthDomain("JoeDomain");

        p.setFtpConnectTimeout(123);
        p.setFtpControlEncoding("encoding");
        p.setFtpDataTimeout(456);
        p.setFtpDefaultDateFormat("defFormat");
        p.setFtpFileType(FtpFileType.BINARY);
        p.setFtpPassiveMode(true);
        p.setFtpRecentDateFormat("recentFormat");
        p.setFtpRemoteVerification(true);
        p.setFtpServerLanguageCode("langCode");
        p.setFtpServerTimeZoneId("zoneId");
        p.setFtpShortMonthNames("jan", "feb");
        p.setFtpSoTimeout(789);
        p.setFtpUserDirIsRoot(true);

        p.setHdfsConfigName("ConfigName");
        p.setHdfsConfigPath(new Path("configPath"));
        p.setHdfsConfigURL(new URL("http://url.com"));

        p.setHttpConnectionTimeout(123);
        p.setHttpFollowRedirect(true);
        p.setHttpMaxConnectionsPerHost(456);
        p.setHttpMaxTotalConnections(789);
        p.setHttpPreemptiveAuth(true);
        p.setHttpSoTimeout(321);
        p.setHttpUrlCharset("UTF-8");
        p.setHttpUserAgent("userAgent");

        p.setRamMaxSize(1234L);

        p.setSftpCompression("blah");
        p.setSftpFileNameEncoding("enc");
        p.setSftpKnownHosts(new File("/tmp").getAbsoluteFile());
        p.setSftpPreferredAuthentications("pref");
        p.setSftpStrictHostKeyChecking("yes");
        p.setSftpTimeout(5678);
        p.setSftpUserDirIsRoot(true);

        p.setCmisRepositoryId("repoID");
        p.setCmisXmlTargetField("xmlField");

        Assertions.assertEquals("JoeUser", p.getAuthUsername());
        Assertions.assertEquals("JoePassword", p.getAuthPassword());
        Assertions.assertEquals("JoeDomain", p.getAuthDomain());
        Assertions.assertEquals(123, p.getFtpConnectTimeout());
        Assertions.assertEquals("encoding", p.getFtpControlEncoding());
        Assertions.assertEquals(456, p.getFtpDataTimeout());
        Assertions.assertEquals("defFormat", p.getFtpDefaultDateFormat());
        Assertions.assertEquals(FtpFileType.BINARY, p.getFtpFileType());
        Assertions.assertTrue(p.isFtpPassiveMode());
        Assertions.assertEquals("recentFormat", p.getFtpRecentDateFormat());
        Assertions.assertTrue(p.isFtpRemoteVerification());
        Assertions.assertEquals("langCode", p.getFtpServerLanguageCode());
        Assertions.assertEquals("zoneId", p.getFtpServerTimeZoneId());
        Assertions.assertTrue(Arrays.equals(
                new String[] { "jan", "feb" }, p.getFtpShortMonthNames()));
        Assertions.assertEquals(789, p.getFtpSoTimeout());
        Assertions.assertTrue(p.isFtpUserDirIsRoot());
        Assertions.assertEquals("ConfigName", p.getHdfsConfigName());
        Assertions.assertEquals(new Path("configPath"), p.getHdfsConfigPath());
        Assertions.assertEquals(new URL("http://url.com"), p.getHdfsConfigURL());
        Assertions.assertEquals(123, p.getHttpConnectionTimeout());
        Assertions.assertTrue(p.isHttpFollowRedirect());
        Assertions.assertEquals(456, p.getHttpMaxConnectionsPerHost());
        Assertions.assertEquals(789, p.getHttpMaxTotalConnections());
        Assertions.assertTrue(p.isHttpPreemptiveAuth());
        Assertions.assertEquals(321, p.getHttpSoTimeout());
        Assertions.assertEquals("UTF-8", p.getHttpUrlCharset());
        Assertions.assertEquals("userAgent", p.getHttpUserAgent());
        Assertions.assertEquals(1234L, p.getRamMaxSize());
        Assertions.assertEquals("blah", p.getSftpCompression());
        Assertions.assertEquals("enc", p.getSftpFileNameEncoding());
        Assertions.assertEquals(new File("/tmp").getAbsoluteFile(), p.getSftpKnownHosts());
        Assertions.assertEquals("pref", p.getSftpPreferredAuthentications());
        Assertions.assertEquals("yes", p.getSftpStrictHostKeyChecking());
        Assertions.assertEquals(5678, p.getSftpTimeout());
        Assertions.assertTrue(p.isSftpUserDirIsRoot());
        Assertions.assertEquals("repoID", p.getCmisRepositoryId());
        Assertions.assertEquals("xmlField", p.getCmisXmlTargetField());

        // test empty
        p = new GenericFilesystemOptionsProvider();
        Assertions.assertNull(p.getAuthPasswordKey());

    }
}
