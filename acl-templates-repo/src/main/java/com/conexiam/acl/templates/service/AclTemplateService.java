package com.conexiam.acl.templates.service;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;

public class AclTemplateService {
    // Dependencies
    private NodeService nodeService;
    private PermissionService permissionService;
    private ServiceRegistry serviceRegistry;

    public void apply(String templateId, NodeRef nodeRef) {
        //TODO: To be implemented
    }
}
