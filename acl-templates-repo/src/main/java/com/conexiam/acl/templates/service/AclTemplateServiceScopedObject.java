package com.conexiam.acl.templates.service;

import com.conexiam.acl.templates.exceptions.AclTemplateServiceException;
import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.alfresco.repo.jscript.ScriptNode;
import org.apache.log4j.Logger;

import java.util.Set;

public class AclTemplateServiceScopedObject extends BaseScopableProcessorExtension {

    static Logger logger = Logger.getLogger(AclTemplateServiceScopedObject.class);

    // Dependencies
    private AclTemplateServiceImpl aclTemplateService;

    public void apply(String templateId, ScriptNode scriptNode) throws AclTemplateServiceException {
        aclTemplateService.apply(templateId, scriptNode.getNodeRef());
    }

    public Set<String> getAclTemplates() {
        return aclTemplateService.getAclTemplates();
    }

    public Set<String> getAuthorityResolvers() {
        return aclTemplateService.getAuthorityResolvers();
    }

    public void setAclTemplateService(AclTemplateServiceImpl aclTemplateService) {
        this.aclTemplateService = aclTemplateService;
    }
}
