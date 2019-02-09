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
import org.apache.commons.lang3.StringUtils;
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

    static final Collection<Capability> CAPABILITIES =
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
        Session session = createSession(fileSystemOptions, file);
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
        return CAPABILITIES;
    }

    private Session createSession(FileSystemOptions opts, FileObject file) {
        LOG.info("Creating new CMIS connection.");
        Map<String, String> params = new HashMap<>();

        resolveAuth(params, opts);
        resolveRepo(params, opts, file);

        params.put(SessionParameter.COMPRESSION, "true");
        // Caching is turned off
        params.put(SessionParameter.CACHE_TTL_OBJECTS, "0");

        resolveSessionParameters(params, opts);

        updateSessionParameters(params);

        SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
        List<Repository> repos = sessionFactory.getRepositories(params);
        Repository cmisRepo = null;
        if (!repos.isEmpty()) {
            LOG.info("Found (" + repos.size() + ") CMIS repositories");
            cmisRepo = repos.get(0);
            LOG.info("Using (first) CMIS repo "
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

    private void resolveSessionParameters(
            Map<String, String> params, FileSystemOptions opts) {
        CmisFileSystemConfigBuilder cmis =
                CmisFileSystemConfigBuilder.getInstance();
        params.putAll(cmis.getSessionParams(opts));
    }

    protected void updateSessionParameters(Map<String, String> params) {
        //NOOP
    }

    private void resolveAuth(
            Map<String, String> params, FileSystemOptions opts) {
        UserAuthenticator auth = DefaultFileSystemConfigBuilder
                .getInstance().getUserAuthenticator(opts);
        if (auth != null) {
            UserAuthenticationData data =
                    auth.requestAuthentication(AUTHENTICATOR_TYPES);
            if (data != null) {
                params.put(SessionParameter.USER, new String(
                        data.getData(UserAuthenticationData.USERNAME)));
                params.put(SessionParameter.PASSWORD, new String(
                        data.getData(UserAuthenticationData.PASSWORD)));
            }
        }
    }

    private void resolveRepo(Map<String, String> params,
            FileSystemOptions opts, FileObject file) {
        CmisFileSystemConfigBuilder cmis =
                CmisFileSystemConfigBuilder.getInstance();
        String atomURL = cmis.getAtomURL(opts);
        if (StringUtils.isNotBlank(atomURL)) {
            resolveAtomParams(params, atomURL);
            LOG.info("Connecting to CMIS Atom endpoint.");
        } else if (StringUtils.isNotBlank(cmis.getWebServicesURL(opts))) {
            resolveWebServicesParams(params, opts);
            LOG.info("Connecting to CMIS Web Service endpoint.");
        } else {
            LOG.info("None of Atom or Web Services URL provided. "
                   + "Defaulting to Atom, using start URL: "
                   + file.getPublicURIString());
            atomURL = StringUtils.substringBefore(file.toString(), "!");
            resolveAtomParams(params, atomURL);
        }

        String repoId = cmis.getRepositoryId(opts);
        if (StringUtils.isNotBlank(repoId)) {
            params.put(SessionParameter.REPOSITORY_ID, repoId);
        }
    }
    private void resolveAtomParams(Map<String, String> params, String atomURL) {
        params.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        params.put(SessionParameter.ATOMPUB_URL, atomURL);
    }
    private void resolveWebServicesParams(
            Map<String, String> params, FileSystemOptions opts) {
        CmisFileSystemConfigBuilder cmis =
                CmisFileSystemConfigBuilder.getInstance();
        params.put(SessionParameter.BINDING_TYPE,
                BindingType.WEBSERVICES.value());

        String baseURL =
                StringUtils.removeEnd(cmis.getWebServicesURL(opts), "/");

        params.put(SessionParameter.WEBSERVICES_ACL_SERVICE,
                baseURL + "/ACLService?wsdl");
        params.put(SessionParameter.WEBSERVICES_DISCOVERY_SERVICE,
                baseURL + "/DiscoveryService?wsdl");
        params.put(SessionParameter.WEBSERVICES_MULTIFILING_SERVICE,
                baseURL + "/MultiFilingService?wsdl");
        params.put(SessionParameter.WEBSERVICES_NAVIGATION_SERVICE,
                baseURL + "/NavigationService?wsdl");
        params.put(SessionParameter.WEBSERVICES_OBJECT_SERVICE,
                baseURL + "/ObjectService?wsdl");
        params.put(SessionParameter.WEBSERVICES_POLICY_SERVICE,
                baseURL + "/PolicyService?wsdl");
        params.put(SessionParameter.WEBSERVICES_RELATIONSHIP_SERVICE,
                baseURL + "/RelationshipService?wsdl");
        params.put(SessionParameter.WEBSERVICES_REPOSITORY_SERVICE,
                baseURL + "/RepositoryService?wsdl");
        params.put(SessionParameter.WEBSERVICES_VERSIONING_SERVICE,
                baseURL + "/VersioningService?wsdl");

    }


    //TODO save/load custom arguments
    //TODO allow method overwrite to set things up


//    private void addParam(
//            Map<String, String> cmisParams, String cmisKey,
//            FileSystemOptions vfsOpts, String vfsKey) {
//        cmisParams.put(cmisKey,
//                CmisFileSystemConfigBuilder.getInstance().getSessionParam(
//                        vfsOpts, vfsKey));
//    }

}
