package com.metaversant.test;

import com.metaversant.acl.templates.authority.resolvers.AuthorityResolver;
import com.metaversant.acl.templates.service.AclTemplateService;
import org.alfresco.model.ContentModel;
import org.alfresco.rad.test.AbstractAlfrescoIT;
import org.alfresco.rad.test.AlfrescoTestRunner;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.Serializable;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(AlfrescoTestRunner.class)
public class AuthorityResolverIT extends AbstractAlfrescoIT {

    private static final String ADMIN_USER_NAME = "admin";

    static Logger logger = Logger.getLogger(AuthorityResolverIT.class);

    private String siteShortName = "test-site-" + System.currentTimeMillis();

    private NodeRef documentLibrary = null;

    @Before
    public void setup() {
        logger.debug("Inside setup");

        SiteService siteService = getServiceRegistry().getSiteService();

        AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);

        SiteInfo testSite = siteService.createSite("site-dashboard", siteShortName, "test site", "test site description", SiteVisibility.PUBLIC);

        documentLibrary = siteService.getContainer(testSite.getShortName(), SiteService.DOCUMENT_LIBRARY);
        if (documentLibrary == null) {
            documentLibrary = siteService.createContainer(siteShortName, "documentLibrary", null, null);
        }
        logger.debug("Leaving setup");
    }

    @Test
    public void testSiteManagerGroupResolver() {
        NodeService nodeService = getServiceRegistry().getNodeService();
        AclTemplateService aclTemplateService = (AclTemplateService) getServiceRegistry().getService(AclTemplateService.ACL_TEMPLATE_SERVICE);

        AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);

        String testFolderName = "testFolder-" + (new Date()).getTime();
        Map<QName, Serializable> contentProps = new HashMap<QName, Serializable>();
        contentProps.put(ContentModel.PROP_NAME, testFolderName);
        NodeRef testFolder = nodeService.createNode(documentLibrary,
                ContentModel.ASSOC_CONTAINS,
                QName.createQName(NamespaceService.CONTENT_MODEL_PREFIX, testFolderName),
                ContentModel.TYPE_FOLDER,
                contentProps).getChildRef();

        AuthorityResolver authResolver = (AuthorityResolver) getServiceRegistry().getService(QName.createQName(NamespaceService.ALFRESCO_URI, "authority-template.site-manager-group"));

        String authority = authResolver.resolve(testFolder);
        assertEquals("GROUP_site_" + siteShortName + "_SiteManager", authority);

        authResolver = (AuthorityResolver) getServiceRegistry().getService(QName.createQName(NamespaceService.ALFRESCO_URI, "authority-template.site-collaborator-group"));
        authority = authResolver.resolve(testFolder);
        assertEquals("GROUP_site_" + siteShortName + "_SiteCollaborator", authority);

        authResolver = (AuthorityResolver) getServiceRegistry().getService(QName.createQName(NamespaceService.ALFRESCO_URI,"authority-template.site-contributor-group"));
        authority = authResolver.resolve(testFolder);
        assertEquals("GROUP_site_" + siteShortName + "_SiteContributor", authority);

        authResolver = (AuthorityResolver) getServiceRegistry().getService(QName.createQName(NamespaceService.ALFRESCO_URI,"authority-template.site-consumer-group"));
        authority = authResolver.resolve(testFolder);
        assertEquals("GROUP_site_" + siteShortName + "_SiteConsumer", authority);

    }

    @Test
    public void testNonSiteNodeRef() {
        SearchService searchService = getServiceRegistry().getSearchService();

        AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);
        //see if there is a folder in the Space Templates folder of the same name
        String query = "PATH:\"/app:company_home\"";
        ResultSet rs = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_FTS_ALFRESCO, query);
        AuthorityResolver authResolver = (AuthorityResolver) getServiceRegistry().getService(QName.createQName(NamespaceService.ALFRESCO_URI, "authority-template.site-manager-group"));
        String authority = authResolver.resolve(rs.getNodeRef(0));
        assertNull(authority);
    }

    @After
    public void teardown() {
        SiteService siteService = getServiceRegistry().getSiteService();
        siteService.deleteSite(siteShortName);
    }
}
