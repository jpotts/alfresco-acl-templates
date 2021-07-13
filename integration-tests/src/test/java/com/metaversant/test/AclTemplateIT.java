package com.metaversant.test;

import com.metaversant.acl.templates.exceptions.AclTemplateServiceException;
import com.metaversant.acl.templates.service.AclTemplateService;
import org.alfresco.model.ContentModel;
import org.alfresco.rad.test.AbstractAlfrescoIT;
import org.alfresco.rad.test.AlfrescoTestRunner;
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

import java.io.Serializable;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by jpotts, Metaversant on 4/3/18.
 */
@RunWith(value = AlfrescoTestRunner.class)
public class AclTemplateIT extends AbstractAlfrescoIT {
  static Logger logger = Logger.getLogger(AclTemplateIT.class);

  private static final String ADMIN_USER_NAME = "admin";

  private String siteShortName = "test-site-" + System.currentTimeMillis();

  private NodeRef documentLibrary = null;

  @Before
  public void setup() {
    logger.debug("Inside setup");
    AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);

    SiteService siteService = getServiceRegistry().getSiteService();
    SearchService searchService = getServiceRegistry().getSearchService();
    AclTemplateService aclTemplateService = (AclTemplateService) getServiceRegistry().getService(AclTemplateService.ACL_TEMPLATE_SERVICE);
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
    assertNotNull(results);
    NodeRef aclTemplateFolderNodeRef = results.getNodeRef(0);
    assertNotNull(aclTemplateFolderNodeRef);

    System.out.println("Leaving setup");
  }

  @Test
  public void helloWorldTest() {
    logger.debug("Starting test");
    assertEquals(1, 1);
    logger.debug("Finishing test");
  }

  @Test
  public void secondTest() {
    NodeService nodeService = getServiceRegistry().getNodeService();
    assertNotNull(nodeService);
  }

  @Test
  public void testApplyTemplate() {
    NodeService nodeService = getServiceRegistry().getNodeService();
    PermissionService permissionService = getServiceRegistry().getPermissionService();
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
    NodeService nodeService = getServiceRegistry().getNodeService();
    PermissionService permissionService = getServiceRegistry().getPermissionService();
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
    AclTemplateService aclTemplateService = (AclTemplateService) getServiceRegistry().getService(AclTemplateService.ACL_TEMPLATE_SERVICE);

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
    AclTemplateService aclTemplateService = (AclTemplateService) getServiceRegistry().getService(AclTemplateService.ACL_TEMPLATE_SERVICE);

    Set<String> aclTemplates = aclTemplateService.getAclTemplates();
    assertEquals(4, aclTemplates.size());
  }

  @Test
  public void testGetAuthorityResolvers() {
    AclTemplateService aclTemplateService = (AclTemplateService) getServiceRegistry().getService(AclTemplateService.ACL_TEMPLATE_SERVICE);
    Set<String> authorityResolvers = aclTemplateService.getAuthorityResolvers();
    assertEquals(4, authorityResolvers.size());
  }

  @After
  public void teardown() {
    SiteService siteService = getServiceRegistry().getSiteService();
    siteService.deleteSite(siteShortName);
    System.out.println("Leaving teardown");
  }
}
