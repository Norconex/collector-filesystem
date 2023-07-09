/* Copyright 2019-2020 Norconex Inc.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclFileAttributeView;
import java.util.Objects;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.provider.local.LocalFile;
import org.apache.commons.vfs2.provider.local.LocalFileName;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.collector.core.doc.CollectorMetadata;
import com.norconex.commons.lang.map.Properties;

public class SpecificLocalFileFetcher implements IFileSpecificMetaFetcher {

    private static final Logger LOG =
            LogManager.getLogger(SpecificLocalFileFetcher.class);

    private static final String LOCAL_FILE_PREFIX =
            CollectorMetadata.COLLECTOR_PREFIX + "localFile.";
    private static final String ACL_PREFIX = LOCAL_FILE_PREFIX + "acl.";

    @Override
    public void fetchFileSpecificMeta(
            FileObject fileObject, Properties metadata) {

        if (!(fileObject instanceof LocalFile)) {
            return;
        }

        fetchAcl((LocalFile) fileObject, metadata);

        if (LOG.isTraceEnabled()) {
            LOG.trace("METADATA:");
            LOG.trace("---------");
            for (String key : new TreeSet<>(metadata.keySet())) {
                LOG.trace("  " + StringUtils.rightPad(key, 40) + " = "
                        + StringUtils.join(metadata.getStrings(key), " | "));
            }
        }
    }

    private void fetchAcl(LocalFile localFile, Properties metadata) {
        try {
            LocalFileName localFileName = (LocalFileName) localFile.getName();
            Path file = new File(localFileName.getRootFile()
                    + localFileName.getPathDecoded()).toPath();

            AclFileAttributeView aclFileAttributes = Files.getFileAttributeView(
                    file, AclFileAttributeView.class);

            if (aclFileAttributes == null) {
                LOG.debug("No ACL file attributes on " + file);
                return;
            }

            if (aclFileAttributes.getOwner() != null
                    && aclFileAttributes.getOwner().getName() != null) {
                metadata.addString(ACL_PREFIX + "owner",
                        aclFileAttributes.getOwner().getName());
            }

            for (AclEntry aclEntry : aclFileAttributes.getAcl()) {
                String type = Objects.toString(aclEntry.type(), "[NOTYPE]");
                String principal = aclEntry.principal().getName();
                for (AclEntryPermission perm : aclEntry.permissions()) {
                    metadata.addString(ACL_PREFIX + type
                            + "." + perm.name(), principal);
                }
                for (AclEntryFlag flag : aclEntry.flags()) {
                    metadata.addString(ACL_PREFIX + type
                            + ".flag." + flag.name(), principal);
                }
            }
        } catch (IOException e) {
            LOG.error("Could not retreive ACL data.", e);
        }
    }
}
