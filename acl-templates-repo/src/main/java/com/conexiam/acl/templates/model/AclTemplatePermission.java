package com.conexiam.acl.templates.model;

public class AclTemplatePermission {
    private String authorityTemplate;
    private String authority;
    private String permission;

    public String getAuthorityTemplate() {
        return authorityTemplate;
    }

    public void setAuthorityTemplate(String authorityTemplate) {
        this.authorityTemplate = authorityTemplate;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }
}
