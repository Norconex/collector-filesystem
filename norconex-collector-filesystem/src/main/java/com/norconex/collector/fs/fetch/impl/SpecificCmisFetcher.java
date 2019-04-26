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

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeSet;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.SecondaryType;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.ExtensionLevel;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import com.norconex.collector.fs.doc.FileMetadata;
import com.norconex.collector.fs.vfs2.provider.cmis.CmisFileObject;
import com.norconex.collector.fs.vfs2.provider.cmis.CmisFileSystemConfigBuilder;
import com.norconex.collector.fs.vfs2.provider.cmis.CmisFileSystemConfigBuilder.PrefixFormat;
import com.norconex.commons.lang.map.Properties;

public class SpecificCmisFetcher implements IFileSpecificMetaFetcher {

    private static final Logger LOG =
            LogManager.getLogger(SpecificCmisFetcher.class);

    private static final String CMIS_PREFIX =
            FileMetadata.COLLECTOR_PREFIX + "cmis.";

    @Override
    public void fetchFileSpecificMeta(
            FileObject fileObject, Properties metadata) {

        if (!(fileObject instanceof CmisFileObject)) {
            return;
        }

        Context ctx = new Context((CmisFileObject) fileObject, metadata);

        fetchSystemMeta(ctx);
        if (ctx.cmisObject != null) {
            fetchCoreMeta(ctx);
            fetchAcl(ctx);
            fetchAllowableActions(ctx);
            fetchProperties(ctx);
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

    private void fetchSystemMeta(Context ctx) {
        add(ctx, "session", "locale", ctx.session.getLocale());
        add(ctx, "session", "rootFolder",
                ctx.session.getRootFolder().getName());

        RepositoryInfo repo = ctx.session.getRepositoryInfo();
        add(ctx, "session", "repository.name", repo.getName());
        add(ctx, "session", "repository.id", repo.getId());
        add(ctx, "session", "repository.productName", repo.getProductName());
        add(ctx, "session", "repository.productVersion",
                repo.getProductVersion());
        add(ctx, "session", "repository.vendorName", repo.getVendorName());
    }

    private void fetchCoreMeta(Context ctx) {
        List<ObjectId> policyIds = ctx.cmisObject.getPolicyIds();
        if (policyIds != null) {
            for (ObjectId policyId : policyIds) {
                add(ctx, null, "policyId", policyId.getId());
            }
        }

        for (ExtensionLevel lvl : ExtensionLevel.values()) {
            fetchExtensions(ctx,
                    ctx.cmisObject.getExtensions(lvl), "extension-" + lvl);
        }

        List<SecondaryType> secTypes = ctx.cmisObject.getSecondaryTypes();
        if (secTypes != null) {
            for (SecondaryType secType : secTypes) {
                add(ctx, null, "secondaryType",  secType.getDisplayName());
            }
        }

        if (ctx.cmisObject instanceof Document) {
            Document doc = (Document) ctx.cmisObject;
            add(ctx, "file", "contentUrl",  doc.getContentUrl());
            if (doc.getPaths() != null) {
                for (String path : doc.getPaths()) {
                    add(ctx, "file", "path", path);
                }
            }
        }
    }

    private void fetchProperties(Context ctx) {
        List<Property<?>> properties = ctx.cmisObject.getProperties();
        if (properties == null) {
            return;
        }
        for (Property<?> p : properties) {
            if (StringUtils.isNotBlank(p.getId())) {
                for (Object val : p.getValues()) {
                    add(ctx, "property", p.getId(), val);
                }
            }
            fetchExtensions(ctx, p.getExtensions(), "property.extension");
        }
    }

    private void fetchAllowableActions(Context ctx) {
        AllowableActions actions = ctx.cmisObject.getAllowableActions();
        if (actions == null) {
            return;
        }

        for (Action action : actions.getAllowableActions()) {
            add(ctx, null, "allowableActions", action);
        }

        fetchExtensions(ctx, actions.getExtensions(),
                "allowableActions.extension");
    }

    private void fetchAcl(Context ctx) {
        Acl acl = ctx.cmisObject.getAcl();
        if (acl == null) {
            return;
        }

        fetchExtensions(ctx, acl.getExtensions(), "acl.extension");

        Properties perms = new Properties();
        for (Ace ace : acl.getAces()) {
            for (String p: ace.getPermissions()) {
                perms.addString(p, ace.getPrincipalId());
            }
            fetchExtensions(ctx, ace.getExtensions(), "acl.ace.extension");
        }
        for (Entry<String, List<String>> en : perms.entrySet()) {
            for (String val : en.getValue()) {
                add(ctx, "acl", en.getKey(), val);
            }
        }
    }

    private void fetchExtensions(
            Context ctx, List<CmisExtensionElement> elems, String shortPrefix) {
        if (elems != null) {
            for (CmisExtensionElement elem : elems) {
                fetchExtension(ctx, elem, shortPrefix);
            }
        }
    }
    private void fetchExtension(
            Context ctx, CmisExtensionElement el, String shortPrefix) {

        // Name/Value:
        String value = el.getValue();
        if (StringUtils.isNotBlank(value)) {
            add(ctx, shortPrefix, el.getName(), value);
        }


        // Attributes:
        Map<String, String> attribs = el.getAttributes();
        if (attribs != null) {
            for (Entry<String, String> en : attribs.entrySet()) {
                add(ctx, shortPrefix + ".attr", en.getKey(), en.getValue());
            }
        }

        // Children:
        fetchExtensions(ctx, el.getChildren(), shortPrefix);
    }

    private void add(
            Context ctx, String shortPrefix, String keyName, Object value) {
        if (value == null) {
            return;
        }

        String val = null;
        if (value instanceof Calendar) {
            val = ISODateTimeFormat.dateTime().print(new DateTime(value));
        } else {
            val = Objects.toString(value, null);
        }
        if (StringUtils.isNotBlank(val)) {
            ctx.metadata.addString(key(ctx, shortPrefix, keyName), val);
        }
    }

    private String key(Context ctx, String shortPrefix, String keyName) {
        String p = StringUtils.isBlank(shortPrefix) ? "" : shortPrefix + ".";

        String key = keyName;
        key = key.replaceFirst("\\{.*\\}", "");

        PrefixFormat format = ctx.getPrefixFormat();
        if (format == PrefixFormat.FULL) {
            key = CMIS_PREFIX + p + key;
        } else if (format == PrefixFormat.COMPACT) {
            key = p + key;
        }
        return key;
    }

    private class Context {
        private final FileSystemOptions vfsOptions;
        private final CmisObject cmisObject;
        private final Properties metadata;
        private final Session session;
        private final CmisFileSystemConfigBuilder cfg =
                CmisFileSystemConfigBuilder.getInstance();
        public Context(CmisFileObject vfsFile, Properties metadata) {
            super();
            this.session = vfsFile.getSession();
            this.cmisObject = vfsFile.getCmisObject();
            this.vfsOptions = vfsFile.getFileSystem().getFileSystemOptions();
            this.metadata = metadata;
        }
        private PrefixFormat getPrefixFormat() {
            return ObjectUtils.defaultIfNull(
                    cfg.getPrefixFormat(vfsOptions), PrefixFormat.NONE);
        }
    }
}
