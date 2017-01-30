package com.conexiam.test;

import com.conexiam.acl.templates.exceptions.AclTemplateServiceException;
import com.conexiam.acl.templates.service.AclTemplateService;
import com.tradeshift.test.remote.Remote;
import com.tradeshift.test.remote.RemoteTestRunner;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
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

import java.io.File;
import java.io.Serializable;
import java.util.*;

import static org.junit.Assert.*;

@RunWith(RemoteTestRunner.class)
@Remote(runnerClass=SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:alfresco/application-context.xml")
public class AclTemplateTest {

    private static final String ADMIN_USER_NAME = "admin";

    static Logger logger = Logger.getLogger(AclTemplateTest.class);

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
    @Qualifier("ContentService")
    protected ContentService contentService;

    @Autowired
    @Qualifier("acl-template-service")
    protected AclTemplateService aclTemplateService;

    @Autowired
    @Qualifier("PermissionService")
    protected PermissionService permissionService;

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

        // Upload test ACL templates
        String query = "PATH:\"" + aclTemplateService.getAclTemplateFolderPath() + "\"";
        ResultSet results = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_FTS_ALFRESCO, query);
        if (results.length() <= 0) {
            logger.error("Could not find ACL template folder: " + aclTemplateService.getAclTemplateFolderPath());
        }
        NodeRef aclTemplateFolderNodeRef = results.getNodeRef(0);

        // If you change the number of test templates uploaded, remember to adjust the testGetAclTemplates accordingly
        uploadAclTemplate(aclTemplateFolderNodeRef, "test-template-1.json");
        uploadAclTemplate(aclTemplateFolderNodeRef, "test-template-2.json");
        uploadAclTemplate(aclTemplateFolderNodeRef, "test-bad-authority-template.json");
        uploadAclTemplate(aclTemplateFolderNodeRef, "test-template-reset.json");
    }

    private void uploadAclTemplate(NodeRef aclTemplateFolderNodeRef, String aclTemplateName) {
        // Delete the test JSON file if it already exists
        NodeRef nodeRef = nodeService.getChildByName(aclTemplateFolderNodeRef, ContentModel.ASSOC_CONTAINS, aclTemplateName);
        if (nodeRef != null) {
            nodeService.deleteNode(nodeRef);
        }

        Map<QName, Serializable> props = new HashMap<QName, Serializable>(1);
        props.put(ContentModel.PROP_NAME, aclTemplateName);

        // use the node service to create a new node
        nodeRef = this.nodeService.createNode(
                aclTemplateFolderNodeRef,
                ContentModel.ASSOC_CONTAINS,
                QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, aclTemplateName),
                ContentModel.TYPE_CONTENT,
                props).getChildRef();

        ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
        writer.setLocale(Locale.US);
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(aclTemplateName).getFile());
        writer.setMimetype("application/json");
        writer.putContent(file);
    }

    @Test
    public void testApplyTemplate() {
        AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);

        String testFolderName = "testFolder-" + (new Date()).getTime();
        Map<QName, Serializable> contentProps = new HashMap<QName, Serializable>();
        contentProps.put(ContentModel.PROP_NAME, testFolderName);
        NodeRef testFolder = nodeService.createNode(documentLibrary,
                ContentModel.ASSOC_CONTAINS,
                QName.createQName(NamespaceService.CONTENT_MODEL_PREFIX, testFolderName),
                ContentModel.TYPE_FOLDER,
                contentProps).getChildRef();

        assertTrue(permissionService.getInheritParentPermissions(testFolder));
        assertEquals(6, permissionService.getAllSetPermissions(testFolder).size());

        boolean exceptionThrown = false;
        try {
            aclTemplateService.apply("test-template-1.json", testFolder);
        } catch (AclTemplateServiceException atse) {
            exceptionThrown = true;
        }
        assertFalse(exceptionThrown);

        assertFalse(permissionService.getInheritParentPermissions(testFolder));
        assertEquals(1, permissionService.getAllSetPermissions(testFolder).size());
    }

    @Test
    public void testApplyMultipleTemplates() {
        AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);

        String testFolderName = "testFolder-" + (new Date()).getTime();
        Map<QName, Serializable> contentProps = new HashMap<QName, Serializable>();
        contentProps.put(ContentModel.PROP_NAME, testFolderName);
        NodeRef testFolder = nodeService.createNode(documentLibrary,
                ContentModel.ASSOC_CONTAINS,
                QName.createQName(NamespaceService.CONTENT_MODEL_PREFIX, testFolderName),
                ContentModel.TYPE_FOLDER,
                contentProps).getChildRef();

        // check the state of the permissions before hand, ie, the default perms
        assertTrue(permissionService.getInheritParentPermissions(testFolder));
        assertEquals(6, permissionService.getAllSetPermissions(testFolder).size());

        boolean exceptionThrown = false;
        try {
            aclTemplateService.apply("test-template-2.json", testFolder);
        } catch (AclTemplateServiceException atse) {
            exceptionThrown = true;
        }
        assertFalse(exceptionThrown);

        // now perms should reflect what was in the template
        assertFalse(permissionService.getInheritParentPermissions(testFolder));
        assertEquals(3, permissionService.getAllSetPermissions(testFolder).size());

        exceptionThrown = false;
        try {
            aclTemplateService.apply("test-template-reset.json", testFolder);
        } catch (AclTemplateServiceException atse) {
            exceptionThrown = true;
        }
        assertFalse(exceptionThrown);

         // now perms should be back to how they were initially
        assertTrue(permissionService.getInheritParentPermissions(testFolder));
        assertEquals(6, permissionService.getAllSetPermissions(testFolder).size());
    }

    @Test
    public void testBadTemplateId() {
        AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);

        boolean exceptionThrown = false;
        try {
            aclTemplateService.apply("test-bad-acl-template-name.json", null);
        } catch (AclTemplateServiceException atse) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
    }

    @Test
    public void testBadAuthorityTemplate() {
        AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);

        String testFolderName = "testFolder-" + (new Date()).getTime();
        Map<QName, Serializable> contentProps = new HashMap<QName, Serializable>();
        contentProps.put(ContentModel.PROP_NAME, testFolderName);
        NodeRef testFolder = nodeService.createNode(documentLibrary,
                ContentModel.ASSOC_CONTAINS,
                QName.createQName(NamespaceService.CONTENT_MODEL_PREFIX, testFolderName),
                ContentModel.TYPE_FOLDER,
                contentProps).getChildRef();

        boolean exceptionThrown = false;
        try {
            aclTemplateService.apply("test-bad-authority-template.json", testFolder);
        } catch (AclTemplateServiceException atse) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
    }

    @Test
    public void testGetAclTemplates() {
        Set<String> aclTemplates = aclTemplateService.getAclTemplates();
        assertEquals(4, aclTemplates.size());
    }

    @Test
    public void testGetAuthorityResolvers() {
        Set<String> authorityResolvers = aclTemplateService.getAuthorityResolvers();
        assertEquals(4, authorityResolvers.size());
    }

    @After
    public void teardown() {
        siteService.deleteSite(siteShortName);
    }
}
