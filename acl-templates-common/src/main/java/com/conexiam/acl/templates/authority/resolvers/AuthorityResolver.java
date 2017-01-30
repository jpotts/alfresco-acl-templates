package com.conexiam.acl.templates.authority.resolvers;

import org.alfresco.service.cmr.repository.NodeRef;

public interface AuthorityResolver {
    String resolve(NodeRef nodeRef);
}
