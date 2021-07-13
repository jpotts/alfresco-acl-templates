package com.metaversant.acl.templates.authority.resolvers;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;

public class SiteRoleGroupResolver implements AuthorityResolver {
    // Dependencies
    private SiteService siteService;
    private String role;

    public String resolve(NodeRef nodeRef) {
        SiteInfo siteInfo = siteService.getSite(nodeRef);
        if (siteInfo == null) {
            return null;
        }
        String siteId = siteInfo.getShortName();
        String siteRoleGroup = siteService.getSiteRoleGroup(siteId, role);
        return siteRoleGroup;
    }

    public void setSiteService(SiteService siteService) {
        this.siteService = siteService;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
