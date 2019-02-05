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
package com.norconex.collector.fs.vfs2.provider.cmis;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.UserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.AbstractLayeredFileProvider;
import org.apache.commons.vfs2.provider.LayeredFileName;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * A provider for CMIS file systems.
 */
public class CmisFileProvider extends AbstractLayeredFileProvider {

    private static final Logger LOG =
            LogManager.getLogger(CmisFileProvider.class);

    private static final UserAuthenticationData.Type[] AUTHENTICATOR_TYPES =
            new UserAuthenticationData.Type[] {
        UserAuthenticationData.USERNAME, UserAuthenticationData.PASSWORD
    };

    static final Collection<Capability> capabilities =
            Collections.unmodifiableCollection(Arrays.asList(new Capability[] {
        Capability.GET_TYPE,
        Capability.GET_LAST_MODIFIED,
        Capability.LIST_CHILDREN,
        Capability.READ_CONTENT,
        Capability.URI,
        //Capability.RANDOM_ACCESS_READ,
    }));

    public CmisFileProvider() {
        super();
    }

    @Override
    protected FileSystem doCreateFileSystem(String scheme, FileObject file,
            FileSystemOptions fileSystemOptions) throws FileSystemException {
        Session session = createSession(fileSystemOptions);
        OperationContext oc = session.createOperationContext();
        //TODO make operationContext configurable as well.
        oc.setIncludeAcls(true);
        oc.setIncludeAllowableActions(true);
        oc.setIncludePathSegments(true);
        oc.setIncludePolicies(true);
        oc.setLoadSecondaryTypeProperties(true);

        final FileName rootName = new LayeredFileName(
                scheme, file.getName(),
                FileName.ROOT_PATH, FileType.FOLDER);

        return new CmisFileSystem(rootName, file, session, oc, fileSystemOptions);
    }

    @Override
    public Collection<Capability> getCapabilities() {
        return capabilities;
    }

    private Session createSession(FileSystemOptions vfsOpts) {
        LOG.info("Creating new CMIS connection.");

        UserAuthenticator auth = DefaultFileSystemConfigBuilder
                .getInstance().getUserAuthenticator(vfsOpts);

        Map<String, String> cmisParams = new HashMap<>();
        if (auth != null) {
            UserAuthenticationData data =
                    auth.requestAuthentication(AUTHENTICATOR_TYPES);
            if (data != null) {
                cmisParams.put(SessionParameter.USER, new String(
                        data.getData(UserAuthenticationData.USERNAME)));
                cmisParams.put(SessionParameter.PASSWORD, new String(
                        data.getData(UserAuthenticationData.PASSWORD)));
            }
        }

        CmisFileSystemConfigBuilder cmis =
                CmisFileSystemConfigBuilder.getInstance();


        cmisParams.put(SessionParameter.ATOMPUB_URL, cmis.getAtomURL(vfsOpts));
//                "http://localhost:8080/alfresco/api/-default-/cmis/versions/1.1/atom");
        cmisParams.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        cmisParams.put(SessionParameter.COMPRESSION, "true");
        cmisParams.put(SessionParameter.CACHE_TTL_OBJECTS, "0"); // Caching is turned off


//        addParam(cmisParams, vfsOpts, SessionParameter.USER);
//        addParam(cmisParams, vfsOpts, SessionParameter.PASSWORD);
//        addParam(cmisParams, vfsOpts, SessionParameter.ATOMPUB_URL);
//        addParam(cmisParams, vfsOpts, SessionParameter.BINDING_TYPE);
//        addParam(cmisParams, vfsOpts, SessionParameter.COMPRESSION);
//        addParam(cmisParams, vfsOpts, SessionParameter.CACHE_TTL_OBJECTS);

        SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
        List<Repository> repos = sessionFactory.getRepositories(cmisParams);
        Repository cmisRepo = null;
        if (!repos.isEmpty()) {
            LOG.info("Found (" + repos.size() + ") CMIS repositories");
            cmisRepo = repos.get(0);
            LOG.info("Info about the first CMIS repo "
                    + "[ID=" + cmisRepo.getId() + "]"
                    + "[name=" + cmisRepo.getName() + "]"
                    + "[CMIS ver supported="
                            + cmisRepo.getCmisVersionSupported() + "]");
        } else {
            throw new CmisConnectionException(
                    "Could not connect to CMIS Server, no repository found!");
        }
        return cmisRepo.createSession();
    }
//    private void addParam(
//            Map<String, String> cmisParams, String cmisKey,
//            FileSystemOptions vfsOpts, String vfsKey) {
//        cmisParams.put(cmisKey,
//                CmisFileSystemConfigBuilder.getInstance().getSessionParam(
//                        vfsOpts, vfsKey));
//    }

}
//parameters.put(SessionParameter.USER, cfg.getSessionParam(opts, SessionParameter.USER));
//parameters.put(SessionParameter.PASSWORD, pwd);
//parameters.put(SessionParameter.ATOMPUB_URL, CMIS_URL);
////          "http://localhost:8080/alfresco/api/-default-/cmis/versions/1.1/atom");
//parameters.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
//parameters.put(SessionParameter.COMPRESSION, "true");
//parameters.put(SessionParameter.CACHE_TTL_OBJECTS, "0"); // Caching is turned off

// If there is only one repository exposed these
// lines will help detect it and its ID
