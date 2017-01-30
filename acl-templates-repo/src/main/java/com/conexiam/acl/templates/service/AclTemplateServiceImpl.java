package com.conexiam.acl.templates.service;

import com.conexiam.acl.templates.authority.resolvers.AuthorityResolver;
import com.conexiam.acl.templates.com.conexiam.acl.templates.service.AclTemplateService;
import com.conexiam.acl.templates.exceptions.AclTemplateServiceException;
import com.conexiam.acl.templates.model.AclTemplate;
import com.conexiam.acl.templates.model.AclTemplatePermission;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AclTemplateServiceImpl implements AclTemplateService {
    // Dependencies
    private NodeService nodeService;
    private PermissionService permissionService;
    private ContentService contentService;
    private SearchService searchService;
    private String aclTemplateFolderPath;
    private Map<String, AuthorityResolver> authorityResolvers = new HashMap<String, AuthorityResolver>();

    private ObjectMapper mapper = new ObjectMapper();

    static Logger logger = Logger.getLogger(AclTemplateServiceImpl.class);

    public void apply(String templateId, NodeRef nodeRef) throws AclTemplateServiceException {
        // Fetch the ACL template that corresponds to the templateId
        AclTemplate template = getAclTemplate(templateId);

        // Apply the template to the node
        applyTemplate(template, nodeRef);
    }

    public Set<String> getAclTemplates() {
        String query = "+PATH:\"" + aclTemplateFolderPath + "/*\"";
        ResultSet results = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_LUCENE, query);
        Set<String> aclTemplates = new HashSet<>();
        for (int i = 0; i < results.length(); i++) {
            NodeRef nodeRef = results.getNodeRef(i);
            aclTemplates.add((String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME));
        }
        return aclTemplates;
    }

    public Set<String> getAuthorityResolvers() {
        return authorityResolvers.keySet();
    }

    private AclTemplate getAclTemplate(String templateId) throws AclTemplateServiceException {

        // Find the JSON file with a name that matches the templateId by querying for the name
        // and the folder where ACL Templates live
        String query = "+PATH:\"" + aclTemplateFolderPath + "/*\" +@cm\\:name:\"" + templateId + "\"";
        ResultSet results = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_LUCENE, query);
        if (results.length() <= 0) {
            throw new AclTemplateServiceException("ACL template not found: " + templateId);
        }
        NodeRef aclTemplateNodeRef = results.getNodeRef(0);
        logger.debug("Retrieved acl template nodeRef: " + aclTemplateNodeRef.getId());

        // Get the ACL template's content input stream
        ContentReader reader = contentService.getReader(aclTemplateNodeRef, ContentModel.PROP_CONTENT);
        InputStream inputStream = reader.getContentInputStream();

        // Read the JSON from the input stream into the POJO
        AclTemplate template = null;
        try {
            template = mapper.readValue(inputStream, AclTemplate.class);
        } catch (IOException ioe) {
            throw new AclTemplateServiceException("IO exception reading ACL template JSON: " + templateId + "(" + aclTemplateNodeRef.getId() + ")");
        }
        logger.debug("Parsed the acl template JSON");

        return template;
    }

    private void applyTemplate(AclTemplate template, NodeRef nodeRef) throws AclTemplateServiceException {
        if (nodeRef == null) {
            throw new AclTemplateServiceException("Node ref must be non-null");
        }

        if (!nodeService.exists(nodeRef)) {
            throw new AclTemplateServiceException("Node ref does not exist: " + nodeRef.getId());
        }

        // first, resolve authority templates
        template = resolveAuthorityTemplates(template, nodeRef);
        permissionService.deletePermissions(nodeRef);
        permissionService.setInheritParentPermissions(nodeRef, template.isInherit());
        for (AclTemplatePermission entry : template.getPermissions()) {
            permissionService.setPermission(nodeRef, entry.getAuthority(), entry.getPermission(), true);
        }
    }

    private AclTemplate resolveAuthorityTemplates(AclTemplate template, NodeRef nodeRef) throws AclTemplateServiceException {
        for (AclTemplatePermission entry : template.getPermissions()) {
            String authorityTemplate = entry.getAuthorityTemplate();
            if (authorityTemplate != null) {
                AuthorityResolver resolver = authorityResolvers.get(authorityTemplate);
                if (resolver != null) {
                    String authority = resolver.resolve(nodeRef);
                    if (authority != null) {
                        entry.setAuthority(authority);
                    }
                } else {
                    throw new AclTemplateServiceException("Could not resolve authority template: " + authorityTemplate);
                }
            }
        }
        return template;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setAuthorityResolvers(Map<String, AuthorityResolver> authorityResolvers) {
        this.authorityResolvers = authorityResolvers;
    }

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setAclTemplateFolderPath(String aclTemplateFolderPath) {
        this.aclTemplateFolderPath = aclTemplateFolderPath;
    }

    public String getAclTemplateFolderPath() {
        return this.aclTemplateFolderPath;
    }
}
