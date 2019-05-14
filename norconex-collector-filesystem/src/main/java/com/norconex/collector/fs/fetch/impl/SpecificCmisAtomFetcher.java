/* Copyright 2019 Norconex Inc.
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

import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.norconex.collector.fs.doc.FileMetadata;
import com.norconex.collector.fs.vfs2.provider.cmis.atom.CmisAtomFileObject;
import com.norconex.collector.fs.vfs2.provider.cmis.atom.CmisAtomFileSystemConfigBuilder;
import com.norconex.collector.fs.vfs2.provider.cmis.atom.CmisAtomSession;
import com.norconex.commons.lang.map.Properties;

public class SpecificCmisAtomFetcher implements IFileSpecificMetaFetcher {

    private static final Logger LOG =
            LogManager.getLogger(SpecificCmisAtomFetcher.class);

    private static final String CMIS_PREFIX =
            FileMetadata.COLLECTOR_PREFIX + "cmis.";

    @Override
    public void fetchFileSpecificMeta(
            FileObject fileObject, Properties metadata) {

        if (!(fileObject instanceof CmisAtomFileObject)) {
            return;
        }

        Context ctx = new Context((CmisAtomFileObject) fileObject, metadata);

        if (ctx.document != null) {
            fetchCoreMeta(ctx);
            fetchProperties(ctx);
            fetchAcl(ctx);
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("METADATA:");
            LOG.trace("---------");
            for (String key : new TreeSet<>(metadata.keySet())) {
                LOG.trace("  " + StringUtils.rightPad(key, 40) + " = "
                        + StringUtils.join(metadata.getStrings(key), " | "));
            }
        }
    }

    private void fetchCoreMeta(Context ctx) {
        ctx.addMetaXpath("author.name", "/entry/author/name/text()");
        ctx.addMetaXpath("id", "/entry/id/text()");
        ctx.addMetaXpath("published", "/entry/published/text()");
        ctx.addMetaXpath("title", "/entry/title/text()");
        ctx.addMetaXpath("edited", "/entry/edited/text()");
        ctx.addMetaXpath("updated", "/entry/updated/text()");
        ctx.addMetaXpath("content", "/entry/content/@src");

        ctx.addMeta("repository.id", ctx.session.getRepoId());
        ctx.addMeta("repository.name", ctx.session.getRepoName());

        String xmlTargetField = ctx.cfg.getXmlTargetField(ctx.vfsOptions);
        if (StringUtils.isNotBlank(xmlTargetField)) {
            ctx.metadata.addString(
                    xmlTargetField, ctx.fileObject.toXmlString());
        }
    }

    private void fetchProperties(Context ctx) {
        try {
            NodeList nl = ctx.session.getNodeList(ctx.document,
                    "/entry/object/properties//"
                    + "*[starts-with(local-name(), 'property')]");
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                String propId =
                        ctx.session.getString(node, "@propertyDefinitionId");
                if (StringUtils.isBlank(propId)) {
                    propId = "undefined_property";
                }
                ctx.addMeta("property." + propId,
                        ctx.session.getString(node, "value/text()"));
            }
        } catch (FileSystemException e) {
            LOG.error("Could not fetch document properties.", e);
        }
    }

    private void fetchAcl(Context ctx) {
        try {
            Properties permissions = new Properties();
            NodeList aclNl = ctx.session.getNodeList(ctx.document,
                    "/entry/object/acl/permission");
            for (int i = 0; i < aclNl.getLength(); i++) {
                Node aclNode = aclNl.item(i);
                String principalId = ctx.session.getString(
                        aclNode, "principal/principalId/text()");

                NodeList permNl = ctx.session.getNodeList(
                        aclNode, "permission/text()");
                for (int j = 0; j < permNl.getLength(); j++) {
                    Node permNode = permNl.item(j);
                    String val = permNode.getNodeValue();
                    if (StringUtils.isNotBlank(val)) {
                        permissions.addString("acl." + val, principalId);
                    }
                }
            }

            for (Entry<String, List<String>> en : permissions.entrySet()) {
                for (String val : en.getValue()) {
                    ctx.addMeta(en.getKey(), val);
                }
            }
        } catch (FileSystemException e) {
            LOG.error("Could not fetch document ACL.", e);
        }
    }

    private class Context {
        private final FileSystemOptions vfsOptions;
        private final Document document;
        private final Properties metadata;
        private final CmisAtomSession session;
        private final CmisAtomFileSystemConfigBuilder cfg =
                CmisAtomFileSystemConfigBuilder.getInstance();
        private final CmisAtomFileObject fileObject;
        public Context(CmisAtomFileObject vfsFile, Properties metadata) {
            super();
            this.fileObject = vfsFile;
            this.session = vfsFile.getSession();
            this.document = vfsFile.getDocument();
            this.vfsOptions = vfsFile.getFileSystem().getFileSystemOptions();
            this.metadata = metadata;
        }
        private void addMeta(String key, Object value) {
            String val = Objects.toString(value, null);
            if (StringUtils.isBlank(val)) {
                return;
            }
            metadata.addString(CMIS_PREFIX + key, val);
        }
        private void addMetaXpath(String key, String exp) {
            try {
                addMeta(key, session.getString(document, exp));
            } catch (FileSystemException e) {
                LOG.warn("Could not obtain " + key + " at: " + exp);
            }
        }
    }
}
