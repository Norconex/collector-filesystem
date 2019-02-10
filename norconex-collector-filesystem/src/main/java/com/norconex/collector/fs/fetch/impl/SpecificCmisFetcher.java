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
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import com.norconex.collector.fs.doc.FileMetadata;
import com.norconex.collector.fs.vfs2.provider.cmis.CmisFileObject;
import com.norconex.commons.lang.map.Properties;

public class SpecificCmisFetcher implements IFileSpecificMetaFetcher {

    private static final String CMIS_PREFIX =
            FileMetadata.COLLECTOR_PREFIX + "cmis";

    //TODO get friendly URL as metadata

    @Override
    public void fetchFileSpecificMeta(
            FileObject fileObject, Properties metadata) {
        if (fileObject instanceof CmisFileObject) {
            CmisFileObject cmisFileObject = (CmisFileObject) fileObject;
            CmisObject cmisObject = cmisFileObject.getCmisObject();
            if (cmisObject != null) {
                fetchAcl(cmisObject, metadata);
                fetchAllowableActions(cmisObject, metadata);
                fetchBasicMeta(cmisObject, metadata);
                fetchProperties(cmisObject, metadata);
                fetchDocumentMeta(cmisObject, metadata);
            }
        }
    }

    private void fetchDocumentMeta(CmisObject cmisObject, Properties m) {
        if (!(cmisObject instanceof Document)) {
            return;
        }
        Document doc = (Document) cmisObject;
        add(m, CMIS_PREFIX + ".contentUrl",  doc.getContentUrl());
        if (doc.getPaths() != null) {
//            add(m, CMIS_PREFIX + ".paths",
//                    doc.getPaths().toArray(ArrayUtils.EMPTY_STRING_ARRAY));
            for (String path : doc.getPaths()) {
                add(m, CMIS_PREFIX + ".paths", path);
            }
        }
        add(m, CMIS_PREFIX + ".versionLabel",  doc.getVersionLabel());
    }


    private void fetchBasicMeta(CmisObject cmisObject, Properties m) {
        add(m, CMIS_PREFIX + ".baseTypeId", cmisObject.getBaseTypeId());
        add(m, CMIS_PREFIX + ".changeToken", cmisObject.getChangeToken());
        add(m, CMIS_PREFIX + ".createdBy", cmisObject.getCreatedBy());
        add(m, CMIS_PREFIX + ".creationDate", cmisObject.getCreationDate());
        add(m, CMIS_PREFIX + ".description", cmisObject.getDescription());
        add(m, CMIS_PREFIX + ".id", cmisObject.getId());
        add(m, CMIS_PREFIX + ".lastModifiedDate",
                cmisObject.getLastModificationDate());
        add(m, CMIS_PREFIX + ".lastModifiedBy", cmisObject.getLastModifiedBy());
        add(m, CMIS_PREFIX + ".name", cmisObject.getName());

        List<ObjectId> policyIds = cmisObject.getPolicyIds();
        if (policyIds != null) {
            for (ObjectId policyId : policyIds) {
                add(m, CMIS_PREFIX + ".policyIds", policyId.getId());
            }
        }

        add(m, CMIS_PREFIX
                + ".refreshTimestamp", cmisObject.getRefreshTimestamp());

        if (cmisObject.getType() != null) {
            add(m, CMIS_PREFIX + ".type", clean(cmisObject.getType().getId()));
        }
    }

    private void fetchProperties(CmisObject cmisObject, Properties metadata) {
        List<Property<?>> properties = cmisObject.getProperties();
        if (properties == null) {
            return;
        }
        for (Property<?> p : properties) {
            String key = CMIS_PREFIX + ".property";
            if (p.getId() != null) {
                key += "." + clean(p.getId());
            }
            //add(metadata, key, StringUtils.join(p.getValues(), ", "));
            for (Object val : p.getValues()) {
                add(metadata, key, val);
            }
        }
    }

    private void fetchAllowableActions(
            CmisObject cmisObject, Properties metadata) {
        AllowableActions actions = cmisObject.getAllowableActions();
        if (actions == null) {
            return;
        }
//        metadata.addString(CMIS_PREFIX + ".allowableActions",
//                StringUtils.join(actions.getAllowableActions(), ", "));
        for (Action action : actions.getAllowableActions()) {
            add(metadata, CMIS_PREFIX + ".allowableActions", action);
        }
    }

    private void fetchAcl(CmisObject cmisObject, Properties metadata) {
        Acl acl = cmisObject.getAcl();
        if (acl == null) {
            return;
        }
        Properties perms = new Properties();
        for (Ace ace : acl.getAces()) {
            for (String p: ace.getPermissions()) {
                perms.addString(p, ace.getPrincipalId());
            }
        }
        for (Entry<String, List<String>> en : perms.entrySet()) {
//            metadata.addString(CMIS_PREFIX + ".acl." + clean(en.getKey()),
//                    StringUtils.join(en.getValue(), ", "));
            for (String val : en.getValue()) {
                add(metadata, CMIS_PREFIX + ".acl." + clean(en.getKey()), val);
            }
        }
    }

    private String clean(String key) {
        String k = key;
        k = k.replaceFirst("\\{.*\\}", "");
        k = k.replaceFirst(".*:(.*)", "$1");
        return k;
    }

    private void add(Properties metadata, String key, Object value) {
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
            metadata.addString(key, val);
        }
    }
}
