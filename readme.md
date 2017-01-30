# Alfresco ACL Templates

Allows you to define a set of named ACL templates described in JSON, then apply a template to a node using the ACL template service. The service will first resolve any Authority Templates that might exist in the ACL template, then will apply the resulting ACL to the node.

## Why Does this Exist?

You can often generalize how an ACL should look. For example, you might say, "This is how an ACL should look when something is approved" or "This is how an ACL should look when something is archived". In addition, you might have many places where you are making changes to ACLs, such as workflows, actions, behaviors, etc.

It is nice to be able to describe an ACL as a generalized pattern or template and do so separate from code, so that administrators or others can make adjustments without needing to touch code or know where that code is in the first place.

With this add-on, you can work with your stakeholders to determine the appropriate set of ACL templates and use JSON to describe those in a central place (the Data Dictionary). Then, any time your Java or JavaScript code needs to set permissions on a node, it can use the ACL Template Service to say, "Apply the 'archived' ACL template to this node" and the permissions will be set appropriately.

If someone later decides, for example, that archived nodes should now be readable by the Legal Department, the ACL template is edited to add the Legal Department. Code that leverages the ACL Template Service does not have to be touched.

## Defining ACL Templates

ACL Template definitions reside in the Data Dictionary under a folder called "ACL Templates". That folder is created when the AMP is installed.

ACL Templates are expressed as JSON. Here is a simple example:

    {
      "inherit": false,
      "permissions": [
        {
          "authorityTemplate": "site-manager-group",
          "permission": "Consumer"
        }
      ]
    }

In this example, the ACL Template consists of a single entry. The entry does not specify a hard-coded authority. Instead, it specifies an Authority Template called "site-manager-group". An authority template is like an alias. It gets resolved when the ACL Template gets applied to a node.

So this ACL Template says, "When this template is applied to a node, figure out what the node's Site Manager Group is and set that to Consumer."

## Authority Template

An Authority Template is just a Java Bean that implements the AuthorityResolver interface. An AuthorityResolver is responsible for returning a String which is the value of the authority to use. The AuthorityResolver gets handed the node reference for the node the ACL Template is being applied to.

In the earlier example, the Authority Template called "site-manager-group" ties to a Java class that takes a node reference, determines which Share Site it belongs to and then returns the name of the group used to store people in the SiteManager role. But AuthorityTemplates could do anything, like return a value based on a property value, for example.

## Applying an ACL Template to a Node

The ACL Template Service is used to apply an ACL Template to a node. The service can be called from Java or by using a root object called "aclTemplates".

For example, to apply an ACL Template called "test-template-2.json" to a folder you could do:

    aclTemplateService.apply("test-template-2.json", testFolder);

Or, if you are using JavaScript, it would look like:

    aclTemplates.apply("test-template-2.json", testFolder);

In addition to the apply method, you can also ask the service for the list of ACL Templates it knows about using `getAclTemplates()` as well as the list of Authority Resolvers it knows about using `getAuthorityResolvers()`.

## Tests

If you check the source code out you can use Maven to run tests with `mvn test`.

## Installing

Running `mvn package` will produce an AMP that can be installed in your Alfresco WAR. Copy the AMP to $ALFRESCO_HOME/amps, then use the MMT to install the AMP, which is typically done by running a script such as `bin/apply_amps.sh`.

## Adding Your Own Authority Resolvers

It is unlikely that this addon will be useful without adding your own Authority Resolver beans. To do that, write one or more Java classes that implement the AuthorityResolver interface, then wire them up in a Spring context XML file.

Then, override the acl-template-service bean with your own definition that passes in your own map of authorityResolvers.

Your custom Authority Resolver Java beans and the related Spring context should go in your own repo-tier AMP, which means you'll need to add this AMP as a dependency to your own project. 

