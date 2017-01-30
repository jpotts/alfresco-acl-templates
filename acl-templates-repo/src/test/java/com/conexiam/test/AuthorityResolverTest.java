package com.conexiam.test;

import com.conexiam.acl.templates.authority.resolvers.AuthorityResolver;
import com.tradeshift.test.remote.Remote;
import com.tradeshift.test.remote.RemoteTestRunner;
import org.alfresco.model.ContentModel;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RemoteTestRunner.class)
@Remote(runnerClass=SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:alfresco/application-context.xml")
public class AuthorityResolverTest {

    private static final String ADMIN_USER_NAME = "admin";

    static Logger logger = Logger.getLogger(AuthorityResolverTest.class);

    @Autowired
    @Qualifier("NodeService")
    protected NodeService nodeService;

    @Autowired
    @Qualifier("SiteService")
    protected SiteService siteService;

    @Autowired
    @Qualifier("SearchService")
    protected SearchService searchService;

    @Autowired
    @Qualifier("authority-template.site-manager-group")
    protected AuthorityResolver siteManagerAuthorityResolver;

    @Autowired
    @Qualifier("authority-template.site-collaborator-group")
    protected AuthorityResolver siteCollaboratorAuthorityResolver;

    @Autowired
    @Qualifier("authority-template.site-contributor-group")
    protected AuthorityResolver siteContributorAuthorityResolver;

    @Autowired
    @Qualifier("authority-template.site-consumer-group")
    protected AuthorityResolver siteConsumerAuthorityResolver;

    private String siteShortName = "test-site-" + System.currentTimeMillis();

    private NodeRef documentLibrary = null;

    @Before
    public void setup() {
        AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);

        SiteInfo testSite = siteService.createSite("site-dashboard", siteShortName, "test site", "test site description", SiteVisibility.PUBLIC);

        documentLibrary = siteService.getContainer(testSite.getShortName(), SiteService.DOCUMENT_LIBRARY);
        if (documentLibrary == null) {
            documentLibrary = siteService.createContainer(siteShortName, "documentLibrary", null, null);
        }
    }

    @Test
    public void testSiteManagerGroupResolver() {
        AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);

        String testFolderName = "testFolder-" + (new Date()).getTime();
        Map<QName, Serializable> contentProps = new HashMap<QName, Serializable>();
        contentProps.put(ContentModel.PROP_NAME, testFolderName);
        NodeRef testFolder = nodeService.createNode(documentLibrary,
                ContentModel.ASSOC_CONTAINS,
                QName.createQName(NamespaceService.CONTENT_MODEL_PREFIX, testFolderName),
                ContentModel.TYPE_FOLDER,
                contentProps).getChildRef();

        String authority = siteManagerAuthorityResolver.resolve(testFolder);
        assertEquals("GROUP_site_" + siteShortName + "_SiteManager", authority);

        authority = siteCollaboratorAuthorityResolver.resolve(testFolder);
        assertEquals("GROUP_site_" + siteShortName + "_SiteCollaborator", authority);

        authority = siteContributorAuthorityResolver.resolve(testFolder);
        assertEquals("GROUP_site_" + siteShortName + "_SiteContributor", authority);

        authority = siteConsumerAuthorityResolver.resolve(testFolder);
        assertEquals("GROUP_site_" + siteShortName + "_SiteConsumer", authority);
    }

    @Test
    public void testNonSiteNodeRef() {
        AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);
        //see if there is a folder in the Space Templates folder of the same name
        String query = "PATH:\"/app:company_home\"";
        ResultSet rs = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_FTS_ALFRESCO, query);
        String authority = siteManagerAuthorityResolver.resolve(rs.getNodeRef(0));
        assertNull(authority);
    }

    @After
    public void teardown() {
        siteService.deleteSite(siteShortName);
    }
}
