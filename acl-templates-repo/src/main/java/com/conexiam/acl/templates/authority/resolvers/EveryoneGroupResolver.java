package com.conexiam.acl.templates.authority.resolvers;

import org.alfresco.service.cmr.repository.NodeRef;

import com.conexiam.acl.templates.authority.resolvers.AuthorityResolver;

public class EveryoneGroupResolver implements AuthorityResolver {
    public String resolve(NodeRef nodeRef) {
        return "GROUP_EVERYONE" ;
    }
}