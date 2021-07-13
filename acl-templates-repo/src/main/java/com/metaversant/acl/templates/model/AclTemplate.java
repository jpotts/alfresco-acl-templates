package com.metaversant.acl.templates.model;

import java.util.List;

public class AclTemplate {

    private boolean inherit;
    private List<AclTemplatePermission> permissions;

    public boolean isInherit() {
        return inherit;
    }

    public void setInherit(boolean inherit) {
        this.inherit = inherit;
    }

    public List<AclTemplatePermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<AclTemplatePermission> permissions) {
        this.permissions = permissions;
    }
}
