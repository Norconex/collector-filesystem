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
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.ftp.FtpFileType;
import org.apache.commons.vfs2.provider.ftps.FtpsFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.hdfs.HdfsFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.http.HttpFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.ram.RamFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.webdav.WebdavFileSystemConfigBuilder;
import org.apache.commons.vfs2.util.EncryptUtil;
import org.apache.hadoop.fs.Path;

import com.norconex.collector.core.CollectorException;
import com.norconex.collector.fs.option.IFilesystemOptionsProvider;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.config.XMLConfigurationUtil;
import com.norconex.commons.lang.encrypt.EncryptionKey;
import com.norconex.commons.lang.encrypt.EncryptionUtil;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;

/**
 * <p>
 * Generic implementation of {@link IFilesystemOptionsProvider}.  This provider
 * makes configurable many Commons VFS options.  The same
 * {@link FileSystemOptions} instance is returned regardless of 
 * the {@link FileObject} supplied.  In many case, it is not necessary
 * to configure any file system options. 
 * </p>
 * <h3>Password encryption:</h3>
 * <p>
 * It is usually recommended to provide credentials here instead of doing
 * so directly on the requested URL to prevent password from appearing
 * in log entries.
 * The <code>authPassword</code> 
 * can take a password that has been encrypted using {@link EncryptionUtil}. 
 * In order for the password to be decrypted properly by the crawler, you need
 * to specify the encryption key used to encrypt it. The key can be stored
 * in a few supported locations and a combination of 
 * <code>[auth|proxy]PasswordKey</code>
 * and <code>[auth|proxy]PasswordKeySource</code> must be specified to properly
 * locate the key. The supported sources are:
 * </p> 
 * <table border="1" summary="">
 *   <tr>
 *     <th><code>[...]PasswordKeySource</code></th>
 *     <th><code>[...]PasswordKey</code></th>
 *   </tr>
 *   <tr>
 *     <td><code>key</code></td>
 *     <td>The actual encryption key.</td>
 *   </tr>
 *   <tr>
 *     <td><code>file</code></td>
 *     <td>Path to a file containing the encryption key.</td>
 *   </tr>
 *   <tr>
 *     <td><code>environment</code></td>
 *     <td>Name of an environment variable containing the key.</td>
 *   </tr>
 *   <tr>
 *     <td><code>property</code></td>
 *     <td>Name of a JVM system property containing the key.</td>
 *   </tr>
 * </table>
 * <p>
 * Still, if you insiste in having the password set on the URL, Apache
 * Commons VFS offers a way to encrypt it there using their own 
 * {@link EncryptUtil}. More info under the "Naming" section here:
 * <a href="http://commons.apache.org/proper/commons-vfs/filesystems.html">
 * http://commons.apache.org/proper/commons-vfs/filesystems.html</a>
 * </p>
 * 
 * <h3>Available Options</h3>
 * <p>
 * You will not find methods or XML configuration tags for all options 
 * supported by various file systems.  If you need to configure more
 * advanced options for your file system, consider having your own 
 * implementation of {@link IFilesystemOptionsProvider} or extend this class
 * and implement the {@link #buildOptions(FileSystemOptions)} method to
 * set additional options.
 * </p>
 * 
 * 
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;optionsProvider class="com.norconex.collector.fs.option.impl.GenericFilesystemOptionsProvider"&gt;
 *  
 *      &lt;!-- Authentication (any file system) --&gt;
 *      &lt;authDomain&gt;...&lt;/authDomain&gt;
 *      &lt;authUsername&gt;...&lt;/authUsername&gt;
 *      &lt;authPassword&gt;...&lt;/authPassword&gt;
 *      &lt;!-- Use the following if password is encrypted. --&gt;
 *      &lt;authPasswordKey&gt;(the encryption key or a reference to it)&lt;/authPasswordKey&gt;
 *      &lt;authPasswordKeySource&gt;[key|file|environment|property]&lt;/authPasswordKeySource&gt;
 *      
 *      &lt;!-- FTP (for FTPS options set ftpSecure to "true") --&gt;
 *      &lt;ftpSecure&gt;[false|true]&lt;/ftpSecure&gt;
 *      &lt;ftpConnectTimeout&gt;(milliseconds)&lt;/ftpConnectTimeout&gt;
 *      &lt;ftpControlEncoding&gt;...&lt;/ftpControlEncoding&gt;
 *      &lt;ftpDataTimeout&gt;(milliseconds)&lt;/ftpDataTimeout&gt;
 *      &lt;ftpDefaultDateFormat&gt;...&lt;/ftpDefaultDateFormat&gt;
 *      &lt;ftpFileType&gt;[ASCII|BINARY|LOCAL|EBCDIC]&lt;/ftpFileType&gt;
 *      &lt;ftpPassiveMode&gt;[false|true]&lt;/ftpPassiveMode&gt;
 *      &lt;ftpRecentDateFormat&gt;...&lt;/ftpRecentDateFormat&gt;
 *      &lt;ftpRemoteVerification&gt;[false|true]&lt;/ftpRemoteVerification&gt;
 *      &lt;ftpServerLanguageCode&gt;...&lt;/ftpServerLanguageCode&gt;
 *      &lt;ftpServerTimeZoneId&gt;...&lt;/ftpServerTimeZoneId&gt;
 *      &lt;ftpShortMonthNames&gt;(comma-separated list)&lt;/ftpShortMonthNames&gt;
 *      &lt;ftpSoTimeout&gt;(milliseconds)&lt;/ftpSoTimeout&gt;
 *      &lt;ftpUserDirIsRoot&gt;[false|true]&lt;/ftpUserDirIsRoot&gt;
 *      
 *      &lt;!-- HDFS --&gt;
 *      &lt;hdfsConfigName&gt;...&lt;/hdfsConfigName&gt;
 *      &lt;hdfsConfigPath&gt;...&lt;/hdfsConfigPath&gt;
 *      &lt;hdfsConfigURL&gt;...&lt;/hdfsConfigURL&gt;
 *      
 *      &lt;!-- HTTP (for Webdav options, set httpWebdav to "true") --&gt;
 *      &lt;httpWebdav&gt;[false|true]&lt;/httpWebdav&gt;
 *      &lt;httpConnectionTimeout&gt;(milliseconds)&lt;/httpConnectionTimeout&gt;
 *      &lt;httpFollowRedirect&gt;[false|true]&lt;/httpFollowRedirect&gt;
 *      &lt;httpMaxConnectionsPerHost&gt;...&lt;/httpMaxConnectionsPerHost&gt;
 *      &lt;httpMaxTotalConnections&gt;...&lt;/httpMaxTotalConnections&gt;
 *      &lt;httpPreemptiveAuth&gt;[false|true]&lt;/httpPreemptiveAuth&gt;
 *      &lt;httpSoTimeout&gt;(milliseconds)&lt;/httpSoTimeout&gt;
 *      &lt;httpUrlCharset&gt;...&lt;/httpUrlCharset&gt;
 *      &lt;httpUserAgent&gt;...&lt;/httpUserAgent&gt;
 *      &lt;httpWebdavCreatorName&gt;...&lt;/httpWebdavCreatorName&gt;
 *      &lt;httpWebdavVersioning&gt;[false|true]&lt;/httpWebdavVersioning&gt;
 *      
 *      &lt;!-- RAM --&gt;
 *      &lt;ramMaxSize&gt;(number of bytes)&lt;/ramMaxSize&gt;
 *
 *      &lt;!-- SFTP --&gt;
 *      &lt;sftpCompression&gt;...&lt;/sftpCompression&gt;
 *      &lt;sftpFileNameEncoding&gt;...&lt;/sftpFileNameEncoding&gt;
 *      &lt;sftpKnownHosts&gt;...&lt;/sftpKnownHosts&gt;
 *      &lt;sftpPreferredAuthentications&gt;...&lt;/sftpPreferredAuthentications&gt;
 *      &lt;sftpStrictHostKeyChecking&gt;[no|yes|ask]&lt;/sftpStrictHostKeyChecking&gt;
 *      &lt;sftpTimeout&gt;(milliseconds)&lt;/sftpTimeout&gt;
 *      &lt;sftpUserDirIsRoot&gt;[false|true]&lt;/sftpUserDirIsRoot&gt;
 *
 *  &lt;/optionsProvider&gt;
 * </pre>
 * <h4>Usage example:</h4>
 * <p>
 * The following sets the FTP settings sometimes required to get 
 * directory listings on remote servers.
 * </p>
 * <pre>
 *  &lt;optionsProvider 
 *      class="com.norconex.collector.fs.option.impl.GenericFilesystemOptionsProvider"&gt;
 *      &lt;ftpPassiveMode&gt;true&lt;/ftpPassiveMode&gt;
 *      &lt;ftpUserDirIsRoot&gt;false&lt;/ftpUserDirIsRoot&gt;
 *  &lt;/optionsProvider&gt;
 * </pre>
 * @author Pascal Essiembre
 * @since 2.7.0
 */
public class GenericFilesystemOptionsProvider 
        implements IFilesystemOptionsProvider, IXMLConfigurable {

    private transient FileSystemOptions options;
    private final Auth auth = new Auth();
    
    //TODO: where there are defaults, they are copied from respective
    //builders. Consider constants instead.
    //Booleans are exception: they are always false to keep with
    //Collector convention.
    
    // FTP/FTPS
    private boolean ftpSecure;
    private Integer ftpConnectTimeout;
    private String ftpControlEncoding;
    private Integer ftpDataTimeout;
    private String ftpDefaultDateFormat;
    private FtpFileType ftpFileType;
    private boolean ftpPassiveMode;
    private String ftpRecentDateFormat;
    private boolean ftpRemoteVerification;
    private String ftpServerLanguageCode;
    private String ftpServerTimeZoneId;
    private String[] ftpShortMonthNames;
    private Integer ftpSoTimeout;
    private boolean ftpUserDirIsRoot;
    
    // HDFS 
    private String hdfsConfigName;
    private Path hdfsConfigPath;
    private URL hdfsConfigURL;

    // HTTP/Webdav
    private boolean httpWebdav;
    private int httpConnectionTimeout;
    private boolean httpFollowRedirect;
    private int httpMaxConnectionsPerHost = 5;
    private int httpMaxTotalConnections = 50;
    private boolean httpPreemptiveAuth;
    private int httpSoTimeout;
    private String httpUrlCharset;
    private String httpUserAgent;
    private String httpWebdavCreatorName;
    private boolean httpWebdavVersioning;

    // RAM
    private long ramMaxSize = Integer.MAX_VALUE;
    
    // SFTP
    private String sftpCompression;
    private String sftpFileNameEncoding;
    private File sftpKnownHosts;
    private String sftpPreferredAuthentications;
    private String sftpStrictHostKeyChecking = "no";
    private int sftpTimeout;
    private boolean sftpUserDirIsRoot;

    
    public GenericFilesystemOptionsProvider() {
        super();
    }
    
    /**
     * Rebuilds file system options. By default the options are reused.
     * Calling this method asks this provider to rebuild the options
     * before using them again.
     */
    public synchronized void rebuildOptions() {
        try {
            buildOptions();
        } catch (FileSystemException e) {
            throw new CollectorException(
                    "Could not build file system options.", e);
        }
    }    
    private void buildOptions() throws FileSystemException {
        
        FileSystemOptions opts = new FileSystemOptions();

        // Generic (auth)
        if (auth.isAuthenticating()) {
            //TODO consider creating a DynamicUserAuthenticator instead
            //that would decrypt on each request to delay when the password
            //is exposed.
            DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(
                    opts, new StaticUserAuthenticator(auth.domain, 
                            auth.username, auth.getResolvedPassword()));
        }

        // FTP + FTPS
        FtpFileSystemConfigBuilder ftp;
        if (ftpSecure) {
            ftp = FtpsFileSystemConfigBuilder.getInstance();
        } else {
            ftp = FtpFileSystemConfigBuilder.getInstance();
        }
        ftp.setConnectTimeout(opts, ftpConnectTimeout);
        ftp.setControlEncoding(opts, ftpControlEncoding);
        ftp.setDataTimeout(opts, ftpDataTimeout);
        ftp.setDefaultDateFormat(opts, ftpDefaultDateFormat);
        ftp.setFileType(opts, ftpFileType);
        ftp.setPassiveMode(opts, ftpPassiveMode);
        ftp.setRecentDateFormat(opts, ftpRecentDateFormat);
        ftp.setRemoteVerification(opts, ftpRemoteVerification);
        ftp.setServerLanguageCode(opts, ftpServerLanguageCode);
        ftp.setServerTimeZoneId(opts, ftpServerTimeZoneId);
        ftp.setShortMonthNames(opts, ftpShortMonthNames);
        ftp.setSoTimeout(opts, ftpSoTimeout);
        ftp.setUserDirIsRoot(opts, ftpUserDirIsRoot);
        
        // HDFS
        HdfsFileSystemConfigBuilder hdfs =
                HdfsFileSystemConfigBuilder.getInstance();
        hdfs.setConfigName(opts, hdfsConfigName);
        hdfs.setConfigPath(opts, hdfsConfigPath);
        hdfs.setConfigURL(opts, hdfsConfigURL);
        
        // HTTP + Webdav
        HttpFileSystemConfigBuilder http;
        if (httpWebdav) {
            WebdavFileSystemConfigBuilder 
                    webdav = (WebdavFileSystemConfigBuilder) 
                            WebdavFileSystemConfigBuilder.getInstance();
            webdav.setCreatorName(opts, httpWebdavCreatorName);
            webdav.setVersioning(opts, httpWebdavVersioning);
            http = webdav;
        } else {
            http = HttpFileSystemConfigBuilder.getInstance();
        }
        http.setConnectionTimeout(opts, httpConnectionTimeout);
        http.setFollowRedirect(opts, httpFollowRedirect);
        http.setMaxConnectionsPerHost(opts, httpMaxConnectionsPerHost);
        http.setMaxTotalConnections(opts, httpMaxTotalConnections);
        http.setPreemptiveAuth(opts, httpPreemptiveAuth);
        http.setSoTimeout(opts, httpSoTimeout);
        http.setUrlCharset(opts, httpUrlCharset);
        http.setUserAgent(opts, httpUserAgent);
        
        // RAM
        RamFileSystemConfigBuilder ram = 
                RamFileSystemConfigBuilder.getInstance();
        ram.setMaxSize(opts, ramMaxSize);
        
        // SFTP
        SftpFileSystemConfigBuilder sftp =
                SftpFileSystemConfigBuilder.getInstance();
        sftp.setCompression(opts, sftpCompression);
        sftp.setFileNameEncoding(opts, sftpFileNameEncoding);
        sftp.setKnownHosts(opts, sftpKnownHosts);
        sftp.setPreferredAuthentications(opts, sftpPreferredAuthentications);
        sftp.setStrictHostKeyChecking(opts, sftpStrictHostKeyChecking);
        sftp.setTimeout(opts, sftpTimeout);
        sftp.setUserDirIsRoot(opts, sftpUserDirIsRoot);
        
        buildOptions(opts);
        this.options = opts;
    }
    
    /**
     * For subclasses to apply more advanced configuration on file system
     * options. Default implementation does nothing. 
     * @param opts file system options
     */
    protected void buildOptions(FileSystemOptions opts) {
        // do nothing
    }
    
    
    /**
     * Gets the authentication domain.
     * @return domain
     */
    public String getAuthDomain() {
        return auth.domain;
    }
    /**
     * Sets the authentication domain.
     * @param authDomain domain
     */
    public void setAuthDomain(String authDomain) {
        this.auth.domain = authDomain;
    }
    /**
     * Gets the authentication username.
     * @return username
     */
    public String getAuthUsername() {
        return auth.username;
    }
    /**
     * Sets the authentication username.
     * @param authUsername username
     */
    public void setAuthUsername(String authUsername) {
        this.auth.username = authUsername;
    }
    /**
     * Gets the authentication password.
     * @return the password
     */
    public String getAuthPassword() {
        return auth.password;
    }
    /**
     * Sets the authentication password.
     * @param authPassword password
     */
    public void setAuthPassword(String authPassword) {
        this.auth.password = authPassword;
    }
    /**
     * Gets the authentication password encryption key.
     * @return the password key or <code>null</code> if the password is not
     * encrypted.
     * @see EncryptionUtil
     */
    public EncryptionKey getAuthPasswordKey() {
        return auth.encryptionKey;
    }
    /**
     * Sets the authentication password encryption key. Only required when 
     * the password is encrypted.
     * @param authPasswordKey password key
     * @see EncryptionUtil
     */
    public void setAuthPasswordKey(EncryptionKey authPasswordKey) {
        this.auth.encryptionKey = authPasswordKey;
    }

    public Integer getFtpConnectTimeout() {
        return ftpConnectTimeout;
    }
    public void setFtpConnectTimeout(Integer ftpConnectTimeout) {
        this.ftpConnectTimeout = ftpConnectTimeout;
    }
    public String getFtpControlEncoding() {
        return ftpControlEncoding;
    }
    public void setFtpControlEncoding(String ftpControlEncoding) {
        this.ftpControlEncoding = ftpControlEncoding;
    }
    public Integer getFtpDataTimeout() {
        return ftpDataTimeout;
    }
    public void setFtpDataTimeout(Integer ftpDataTimeout) {
        this.ftpDataTimeout = ftpDataTimeout;
    }
    public String getFtpDefaultDateFormat() {
        return ftpDefaultDateFormat;
    }
    public void setFtpDefaultDateFormat(String ftpDefaultDateFormat) {
        this.ftpDefaultDateFormat = ftpDefaultDateFormat;
    }
    public FtpFileType getFtpFileType() {
        return ftpFileType;
    }
    public void setFtpFileType(FtpFileType ftpFileType) {
        this.ftpFileType = ftpFileType;
    }
    public boolean isFtpPassiveMode() {
        return ftpPassiveMode;
    }
    public void setFtpPassiveMode(boolean ftpPassiveMode) {
        this.ftpPassiveMode = ftpPassiveMode;
    }
    public String getFtpRecentDateFormat() {
        return ftpRecentDateFormat;
    }
    public void setFtpRecentDateFormat(String ftpRecentDateFormat) {
        this.ftpRecentDateFormat = ftpRecentDateFormat;
    }
    public boolean isFtpRemoteVerification() {
        return ftpRemoteVerification;
    }
    public void setFtpRemoteVerification(boolean ftpRemoteVerification) {
        this.ftpRemoteVerification = ftpRemoteVerification;
    }
    public String getFtpServerLanguageCode() {
        return ftpServerLanguageCode;
    }
    public void setFtpServerLanguageCode(String ftpServerLanguageCode) {
        this.ftpServerLanguageCode = ftpServerLanguageCode;
    }
    public String getFtpServerTimeZoneId() {
        return ftpServerTimeZoneId;
    }
    public void setFtpServerTimeZoneId(String ftpServerTimeZoneId) {
        this.ftpServerTimeZoneId = ftpServerTimeZoneId;
    }
    public String[] getFtpShortMonthNames() {
        return ftpShortMonthNames;
    }
    public void setFtpShortMonthNames(String... ftpShortMonthNames) {
        this.ftpShortMonthNames = ftpShortMonthNames;
    }
    public Integer getFtpSoTimeout() {
        return ftpSoTimeout;
    }
    public void setFtpSoTimeout(Integer ftpSoTimeout) {
        this.ftpSoTimeout = ftpSoTimeout;
    }
    public boolean isFtpUserDirIsRoot() {
        return ftpUserDirIsRoot;
    }
    public void setFtpUserDirIsRoot(boolean ftpUserDirIsRoot) {
        this.ftpUserDirIsRoot = ftpUserDirIsRoot;
    }
    public boolean isFtpSecure() {
        return ftpSecure;
    }
    public void setFtpSecure(boolean ftpSecure) {
        this.ftpSecure = ftpSecure;
    }
    
    public String getHdfsConfigName() {
        return hdfsConfigName;
    }
    public void setHdfsConfigName(String hdfsConfigName) {
        this.hdfsConfigName = hdfsConfigName;
    }
    public Path getHdfsConfigPath() {
        return hdfsConfigPath;
    }
    public void setHdfsConfigPath(Path hdfsConfigPath) {
        this.hdfsConfigPath = hdfsConfigPath;
    }
    public URL getHdfsConfigURL() {
        return hdfsConfigURL;
    }
    public void setHdfsConfigURL(URL hdfsConfigURL) {
        this.hdfsConfigURL = hdfsConfigURL;
    }
    
    public int getHttpConnectionTimeout() {
        return httpConnectionTimeout;
    }
    public void setHttpConnectionTimeout(int httpConnectionTimeout) {
        this.httpConnectionTimeout = httpConnectionTimeout;
    }
    public boolean isHttpFollowRedirect() {
        return httpFollowRedirect;
    }
    public void setHttpFollowRedirect(boolean httpFollowRedirect) {
        this.httpFollowRedirect = httpFollowRedirect;
    }
    public int getHttpMaxConnectionsPerHost() {
        return httpMaxConnectionsPerHost;
    }
    public void setHttpMaxConnectionsPerHost(int httpMaxConnectionsPerHost) {
        this.httpMaxConnectionsPerHost = httpMaxConnectionsPerHost;
    }
    public int getHttpMaxTotalConnections() {
        return httpMaxTotalConnections;
    }
    public void setHttpMaxTotalConnections(int httpMaxTotalConnections) {
        this.httpMaxTotalConnections = httpMaxTotalConnections;
    }
    public boolean isHttpPreemptiveAuth() {
        return httpPreemptiveAuth;
    }
    public void setHttpPreemptiveAuth(boolean httpPreemptiveAuth) {
        this.httpPreemptiveAuth = httpPreemptiveAuth;
    }
    public int getHttpSoTimeout() {
        return httpSoTimeout;
    }
    public void setHttpSoTimeout(int httpSoTimeout) {
        this.httpSoTimeout = httpSoTimeout;
    }
    public String getHttpUrlCharset() {
        return httpUrlCharset;
    }
    public void setHttpUrlCharset(String httpUrlCharset) {
        this.httpUrlCharset = httpUrlCharset;
    }
    public String getHttpUserAgent() {
        return httpUserAgent;
    }
    public void setHttpUserAgent(String httpUserAgent) {
        this.httpUserAgent = httpUserAgent;
    }
    public boolean isHttpWebdav() {
        return httpWebdav;
    }
    public void setHttpWebdav(boolean httpWebdav) {
        this.httpWebdav = httpWebdav;
    }
    public String getHttpWebdavCreatorName() {
        return httpWebdavCreatorName;
    }
    public void setHttpWebdavCreatorName(String httpWebdavCreatorName) {
        this.httpWebdavCreatorName = httpWebdavCreatorName;
    }
    public boolean isHttpWebdavVersioning() {
        return httpWebdavVersioning;
    }
    public void setHttpWebdavVersioning(boolean httpWebdavVersioning) {
        this.httpWebdavVersioning = httpWebdavVersioning;
    }
    
    public long getRamMaxSize() {
        return ramMaxSize;
    }
    public void setRamMaxSize(long ramMaxSize) {
        this.ramMaxSize = ramMaxSize;
    }
    
    public String getSftpCompression() {
        return sftpCompression;
    }
    public void setSftpCompression(String sftpCompression) {
        this.sftpCompression = sftpCompression;
    }
    public String getSftpFileNameEncoding() {
        return sftpFileNameEncoding;
    }
    public void setSftpFileNameEncoding(String sftpFileNameEncoding) {
        this.sftpFileNameEncoding = sftpFileNameEncoding;
    }
    public File getSftpKnownHosts() {
        return sftpKnownHosts;
    }
    public void setSftpKnownHosts(File sftpKnownHosts) {
        this.sftpKnownHosts = sftpKnownHosts;
    }
    public String getSftpPreferredAuthentications() {
        return sftpPreferredAuthentications;
    }
    public void setSftpPreferredAuthentications(
            String sftpPreferredAuthentications) {
        this.sftpPreferredAuthentications = sftpPreferredAuthentications;
    }
    public String getSftpStrictHostKeyChecking() {
        return sftpStrictHostKeyChecking;
    }
    public void setSftpStrictHostKeyChecking(String sftpStrictHostKeyChecking) {
        this.sftpStrictHostKeyChecking = sftpStrictHostKeyChecking;
    }
    public int getSftpTimeout() {
        return sftpTimeout;
    }
    public void setSftpTimeout(int sftpTimeout) {
        this.sftpTimeout = sftpTimeout;
    }
    public boolean isSftpUserDirIsRoot() {
        return sftpUserDirIsRoot;
    }
    public void setSftpUserDirIsRoot(boolean sftpUserDirIsRoot) {
        this.sftpUserDirIsRoot = sftpUserDirIsRoot;
    }

    @Override
    public synchronized FileSystemOptions getFilesystemOptions(
            FileObject fileObject) {
        if (options == null) {
            rebuildOptions();
        }
        return options;
        
    }

    @Override
    public void loadFromXML(Reader in) throws IOException {
        XMLConfiguration xml = XMLConfigurationUtil.newXMLConfiguration(in);
        loadDefaultFileSystem(xml);
        loadFTP(xml);
        loadHDFS(xml);
        loadHTTP(xml);
        loadRAM(xml);
        loadSFTP(xml);
    }
    private void loadDefaultFileSystem(XMLConfiguration xml) {
        auth.domain = xml.getString("authDomain", auth.domain);
        auth.username = xml.getString("authUsername", auth.username);
        auth.password = xml.getString("authPassword", auth.password);
        auth.setEncryptionKey(
                xml.getString("authPasswordKey", null),
                xml.getString("authPasswordKeySource", null));
    }
    private void loadFTP(XMLConfiguration xml) {
        ftpConnectTimeout = 
                xml.getInteger("ftpConnectTimeout", ftpConnectTimeout);
        ftpControlEncoding = 
                xml.getString("ftpControlEncoding", ftpControlEncoding);
        ftpDataTimeout = xml.getInteger("ftpDataTimeout", ftpDataTimeout);
        ftpDefaultDateFormat = 
                xml.getString("ftpDefaultDateFormat", ftpDefaultDateFormat);
        String type = xml.getString(
                "ftpFileType", Objects.toString(ftpFileType, null));
        if (StringUtils.isNotBlank(type)) {
            ftpFileType = FtpFileType.valueOf(type);
        }
        ftpPassiveMode = xml.getBoolean("ftpPassiveMode", ftpPassiveMode);
        ftpRecentDateFormat = 
                xml.getString("ftpRecentDateFormat", ftpRecentDateFormat);
        ftpRemoteVerification = 
                xml.getBoolean("ftpRemoteVerification", ftpRemoteVerification);
        ftpServerLanguageCode = 
                xml.getString("ftpServerLanguageCode", ftpServerLanguageCode);
        ftpServerTimeZoneId = 
                xml.getString("ftpServerTimeZoneId", ftpServerTimeZoneId);
        ftpShortMonthNames = XMLConfigurationUtil.getCSVStringArray(
                xml, "ftpShortMonthNames", ftpShortMonthNames);
        ftpSoTimeout = xml.getInteger("ftpSoTimeout", ftpSoTimeout);
        ftpUserDirIsRoot = xml.getBoolean("ftpUserDirIsRoot", ftpUserDirIsRoot);
    }
    private void loadHDFS(XMLConfiguration xml) {
        hdfsConfigName = xml.getString("hdfsConfigName", hdfsConfigName);
        String path = xml.getString("hdfsConfigPath", null);
        if (StringUtils.isNotBlank(path)) {
            hdfsConfigPath = new Path(path);
        }
        String url = xml.getString("hdfsConfigURL", null);
        if (StringUtils.isNotBlank(url)) {
            try {
                hdfsConfigURL = new URL(url);
            } catch (MalformedURLException e) {
                throw new CollectorException(
                        "Could not load HDFS config URL.", e);
            }
        }
    }
    private void loadHTTP(XMLConfiguration xml) {
        httpWebdav = xml.getBoolean("httpWebdav", httpWebdav);
        httpConnectionTimeout = 
                xml.getInteger("httpConnectionTimeout", httpConnectionTimeout);
        httpFollowRedirect = 
                xml.getBoolean("httpFollowRedirect", httpFollowRedirect);
        httpMaxConnectionsPerHost = xml.getInteger(
                "httpMaxConnectionsPerHost", httpMaxConnectionsPerHost);
        httpMaxTotalConnections = xml.getInteger(
                "httpMaxTotalConnections", httpMaxTotalConnections);
        httpPreemptiveAuth = 
                xml.getBoolean("httpPreemptiveAuth", httpPreemptiveAuth);
        httpSoTimeout = xml.getInteger("httpSoTimeout", httpSoTimeout);
        httpUrlCharset = xml.getString("httpUrlCharset", httpUrlCharset);
        httpUserAgent = xml.getString("httpUserAgent", httpUserAgent);
        httpWebdavCreatorName = 
                xml.getString("httpWebdavCreatorName", httpWebdavCreatorName);
        httpWebdavVersioning = 
                xml.getBoolean("httpWebdavVersioning", httpWebdavVersioning);
    }
    private void loadRAM(XMLConfiguration xml) {
        ramMaxSize = xml.getLong("ramMaxSize", ramMaxSize);
    }
    private void loadSFTP(XMLConfiguration xml) {
        sftpCompression = xml.getString("sftpCompression", sftpCompression);
        sftpFileNameEncoding = 
                xml.getString("sftpFileNameEncoding", sftpFileNameEncoding);
        String knownHosts = xml.getString("sftpKnownHosts", null);
        if (StringUtils.isNotBlank(knownHosts)) {
            sftpKnownHosts = new File(knownHosts);
        }
        sftpPreferredAuthentications = xml.getString(
                "sftpPreferredAuthentications", sftpPreferredAuthentications);
        sftpStrictHostKeyChecking = xml.getString(
                "sftpStrictHostKeyChecking", sftpStrictHostKeyChecking);
        sftpTimeout = xml.getInteger("sftpTimeout", sftpTimeout);
        sftpUserDirIsRoot = 
                xml.getBoolean("sftpUserDirIsRoot", sftpUserDirIsRoot);
    }
        
    @Override
    public void saveToXML(Writer out) throws IOException {
        try {
            EnhancedXMLStreamWriter writer = new EnhancedXMLStreamWriter(out);
            writer.writeStartElement("optionsProvider");
            writer.writeAttribute("class", getClass().getCanonicalName());

            saveDefaultFileSystem(writer);
            saveFTP(writer);
            saveHDFS(writer);
            saveHTTP(writer);
            saveRAM(writer);
            saveSFTP(writer);
            
            writer.writeEndElement();
            writer.flush();
            writer.close();
        } catch (XMLStreamException e) {
            throw new IOException("Cannot save as XML.", e);
        }        
    }
    private void saveDefaultFileSystem(EnhancedXMLStreamWriter writer) 
            throws XMLStreamException {
        if (auth.isAuthenticating()) {
            writer.writeElementString("authDomain", auth.domain);
            writer.writeElementString("authUsername", auth.username);
            writer.writeElementString("authPassword", auth.password);
            EncryptionKey key = auth.encryptionKey;
            if (key != null) {
                writer.writeElementString("authPasswordKey", key.getValue());
                if (key.getSource() != null) {
                    writer.writeElementString("authPasswordKeySource", 
                            key.getSource().name().toLowerCase());
                }
            }
        }
    }
    private void saveFTP(EnhancedXMLStreamWriter writer) 
            throws XMLStreamException {
        writer.writeElementInteger("ftpConnectTimeout", ftpConnectTimeout);
        writer.writeElementString("ftpControlEncoding", ftpControlEncoding);
        writer.writeElementInteger("ftpDataTimeout", ftpDataTimeout);
        writer.writeElementString("ftpDefaultDateFormat", ftpDefaultDateFormat);
        writer.writeElementString(
                "ftpFileType", Objects.toString(ftpFileType, null));
        writer.writeElementBoolean("ftpPassiveMode", ftpPassiveMode);
        writer.writeElementString("ftpRecentDateFormat", ftpRecentDateFormat);
        writer.writeElementBoolean(
                "ftpRemoteVerification", ftpRemoteVerification);
        writer.writeElementString(
                "ftpServerLanguageCode", ftpServerLanguageCode);
        writer.writeElementString("ftpServerTimeZoneId", ftpServerTimeZoneId);
        writer.writeElementString("ftpShortMonthNames", 
                StringUtils.join(ftpShortMonthNames, ","));
        writer.writeElementInteger("ftpSoTimeout", ftpSoTimeout);
        writer.writeElementBoolean("ftpUserDirIsRoot", ftpUserDirIsRoot);
    }
    private void saveHDFS(EnhancedXMLStreamWriter writer) 
            throws XMLStreamException {
        writer.writeElementString("hdfsConfigName", hdfsConfigName);
        writer.writeElementString("hdfsConfigPath", 
                Objects.toString(hdfsConfigPath, null));
        writer.writeElementString("hdfsConfigURL",
                Objects.toString(hdfsConfigURL, null));
    }
    private void saveHTTP(EnhancedXMLStreamWriter writer) 
            throws XMLStreamException {
        writer.writeElementBoolean("httpWebdav", httpWebdav);
        writer.writeElementInteger(
                "httpConnectionTimeout", httpConnectionTimeout);
        writer.writeElementBoolean("httpFollowRedirect", httpFollowRedirect);
        writer.writeElementInteger(
                "httpMaxConnectionsPerHost", httpMaxConnectionsPerHost);
        writer.writeElementInteger(
                "httpMaxTotalConnections", httpMaxTotalConnections);
        writer.writeElementBoolean("httpPreemptiveAuth", httpPreemptiveAuth);
        writer.writeElementInteger("httpSoTimeout", httpSoTimeout);
        writer.writeElementString("httpUrlCharset", httpUrlCharset);
        writer.writeElementString("httpUserAgent", httpUserAgent);
        writer.writeElementString(
                "httpWebdavCreatorName", httpWebdavCreatorName);
        writer.writeElementBoolean(
                "httpWebdavVersioning", httpWebdavVersioning);
    }
    private void saveRAM(EnhancedXMLStreamWriter writer) 
            throws XMLStreamException {
        writer.writeElementLong("ramMaxSize", ramMaxSize);
    }    
    private void saveSFTP(EnhancedXMLStreamWriter writer) 
            throws XMLStreamException {
        writer.writeElementString("sftpCompression", sftpCompression);
        writer.writeElementString("sftpFileNameEncoding", sftpFileNameEncoding);
        if (sftpKnownHosts != null) {
            writer.writeElementString(
                    "sftpKnownHosts", sftpKnownHosts.getAbsolutePath());
        }
        writer.writeElementString(
                "sftpPreferredAuthentications", sftpPreferredAuthentications);
        writer.writeElementString(
                "sftpStrictHostKeyChecking", sftpStrictHostKeyChecking);
        writer.writeElementInteger("sftpTimeout", sftpTimeout);
        writer.writeElementBoolean("sftpUserDirIsRoot", sftpUserDirIsRoot);
    }    
    
    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other, false);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, false);
    }
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(
                this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
    
    private class Auth {
        private String domain;
        private String username;
        private String password;
        private EncryptionKey encryptionKey;
        private boolean isAuthenticating() {
            return StringUtils.isNotBlank(domain) 
                    || StringUtils.isNotBlank(username)
                    || StringUtils.isNotBlank(password);
        }
        private void setEncryptionKey(
                String passwordKey, String passwordKeySource) {
            if (StringUtils.isBlank(passwordKey)) {
                return;
            }
            EncryptionKey.Source source = null;
            if (StringUtils.isNotBlank(passwordKeySource)) {
                source = EncryptionKey.Source.valueOf(
                        passwordKeySource.toUpperCase());
            }
            this.encryptionKey = new EncryptionKey(passwordKey, source);
        }
        private String getResolvedPassword() {
            if (encryptionKey == null) {
                return password;
            }
            return EncryptionUtil.decrypt(password, encryptionKey);
        }
        @Override
        public boolean equals(final Object other) {
            return EqualsBuilder.reflectionEquals(this, other, false);
        }
        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this, false);
        }
        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(
                    this, ToStringStyle.SHORT_PREFIX_STYLE);
        } 
    }
}
