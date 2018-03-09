package com.conexiam.acl.templates.service;

import com.conexiam.acl.templates.exceptions.AclTemplateServiceException;
import org.alfresco.service.cmr.repository.NodeRef;

import java.util.Set;

public interface AclTemplateService {

    /**
     * Applies the specified ACL template to the provided nodeRef. The caller must have the rights necessary to
     * change permissions on the specified node.
     *
     * @param templateId The cm:name of the JSON file in the ACL template folder that is to be applied.
     * @param nodeRef The node reference of the node to apply the template to.
     * @throws AclTemplateServiceException when something bad happens, like if the template isn't found or cannot be parsed.
     */
    void apply(String templateId, NodeRef nodeRef) throws AclTemplateServiceException;

    /**
     * Return a list of the ACL templates that the template service knows about.
     * @return Set of Strings where each String is the name of an ACL template stored in the ACL template folder.
     */
    Set<String> getAclTemplates();

    /**
     * Returns a list of the authority resolvers that the template service knows about.
     * @return Set of Strings where each String is the ID of an AuthorityResolver bean.
     */
    Set<String> getAuthorityResolvers();

    /**
     * Returns the folder path in the repository where ACL templates are stored.
     * @return String representing the folder path.
     */
    String getAclTemplateFolderPath();
}
