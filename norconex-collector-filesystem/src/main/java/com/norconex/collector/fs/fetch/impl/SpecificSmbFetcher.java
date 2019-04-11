/* Copyright 2017-2019 Norconex Inc.
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
package com.norconex.collector.fs.fetch.impl;

import java.io.IOException;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.UserAuthenticationData.Type;
import org.apache.commons.vfs2.provider.smb.SmbFileName;
import org.apache.commons.vfs2.provider.smb.SmbFileObject;
import org.apache.commons.vfs2.provider.smb.SmbFileProvider;
import org.apache.commons.vfs2.util.UserAuthenticatorUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.collector.fs.doc.FileMetadata;
import com.norconex.commons.lang.map.Properties;

import jcifs.smb.ACE;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SID;
import jcifs.smb.SmbFile;

/**
 * Used to obtain ACL from files when using SMB protocol.
 * @author Pascal Essiembre
 * @since 2.7.0
 */
/*default*/ final class SpecificSmbFetcher implements IFileSpecificMetaFetcher {

    private static final Logger LOG =
            LogManager.getLogger(SpecificSmbFetcher.class);

    private static final String ACL_PREFIX =
            FileMetadata.COLLECTOR_PREFIX + "acl.smb";
    private static final String ACE = ".ace";
    private static final String SID = ".sid";
    private static final String SID_TEXT = ".sidAsText";
    private static final String TYPE = ".type";
    private static final String TYPE_TEXT = ".typeAsText";
    private static final String DOMAIN_SID = ".domainSid";
    private static final String DOMAIN_NAME = ".domainName";
    private static final String ACCOUNT_NAME = ".accountName";

    /*default*/ SpecificSmbFetcher() {
        super();
    }

    @Override
    public void fetchFileSpecificMeta(
            FileObject fileObject, Properties metadata) {
        if (fileObject instanceof SmbFileObject) {
            SmbFileObject smbFileObject = (SmbFileObject) fileObject;
            try {
                SmbFile f = createSmbFile(smbFileObject);
                ACE[] acl = f.getSecurity();
                for (int i = 0; i < acl.length; i++) {
                    storeSID(acl, metadata);
                }
            } catch (IOException e) {
                LOG.error("Could not retreive SMB ACL data.", e);
            }
        }
    }

    private void storeSID(ACE[] acls, Properties metadata) {
        for (int i = 0; i < acls.length; i++) {
            ACE acl = acls[i];
            SID sid = acl.getSID();
            metadata.setString(key(i, ACE), acl.toString());
            metadata.setString(key(i, SID), sid.toString());
            metadata.setString(key(i, SID_TEXT), sid.toDisplayString());
            metadata.setInt(key(i, TYPE), sid.getType());
            metadata.setString(key(i, TYPE_TEXT), sid.getTypeText());
            metadata.setString(
                    key(i, DOMAIN_SID), sid.getDomainSid().toString());
            metadata.setString(key(i, DOMAIN_NAME), sid.getDomainName());
            metadata.setString(key(i, ACCOUNT_NAME), sid.getAccountName());
        }
    }
    private String key(int index, String suffix) {
        return ACL_PREFIX + "[" + index + "]" + suffix;
    }

    /*
     * Adapted from SmbFileObject since there is otherwise no way to get
     * the SmbFile from the Commons VFS sandbox SmbFile class and we need it
     * for ACL extract.  Should delete if a better way is provider by VFS.
     */
    private SmbFile createSmbFile(FileObject fileObject)
            throws IOException {

        final SmbFileName smbFileName = (SmbFileName) fileObject.getName();

        final String path = smbFileName.getUriWithoutAuth();

        UserAuthenticationData authData = null;
        SmbFile file;
        try {
            authData = UserAuthenticatorUtils.authenticate(
                    fileObject.getFileSystem().getFileSystemOptions(),
                           SmbFileProvider.AUTHENTICATOR_TYPES);

            NtlmPasswordAuthentication auth = null;
            if (authData != null) {
                auth = new NtlmPasswordAuthentication(
                        authToString(authData, UserAuthenticationData.DOMAIN,
                                smbFileName.getDomain()),
                        authToString(authData, UserAuthenticationData.USERNAME,
                                smbFileName.getUserName()),
                        authToString(authData, UserAuthenticationData.PASSWORD,
                                smbFileName.getPassword()));
            }

            // if auth == null SmbFile uses default credentials
            // ("jcifs.smb.client.domain", "?"), ("jcifs.smb.client.username", "GUEST"),
            // ("jcifs.smb.client.password", BLANK);
            // ANONYMOUS=("","","")
            file = new SmbFile(path, auth);

            if (file.isDirectory() && !file.toString().endsWith("/")) {
                file = new SmbFile(path + "/", auth);
            }
            return file;
        } finally {
            UserAuthenticatorUtils.cleanup(authData); // might be null
        }
    }

    private String authToString(
            UserAuthenticationData authData, Type type, String part) {
        return UserAuthenticatorUtils.toString(UserAuthenticatorUtils.getData(
                authData, type, UserAuthenticatorUtils.toChar(part)));
    }
}
