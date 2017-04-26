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

import org.apache.commons.vfs2.provider.ftp.FtpFileType;
import org.apache.hadoop.fs.Path;
import org.junit.Test;

import com.norconex.commons.lang.config.XMLConfigurationUtil;

public class GenericFilesystemOptionsProviderTest {
    
    @Test
    public void testWriteRead() throws IOException {
        GenericFilesystemOptionsProvider p = 
                new GenericFilesystemOptionsProvider();
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
        
        System.out.println("Writing/Reading this: " + p);
        XMLConfigurationUtil.assertWriteRead(p);
        

        // test empty
        p = new GenericFilesystemOptionsProvider();
        System.out.println("Writing/Reading this: " + p);
        XMLConfigurationUtil.assertWriteRead(p);
        
    }
}
