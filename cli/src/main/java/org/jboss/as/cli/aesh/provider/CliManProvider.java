package org.jboss.as.cli.aesh.provider;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.helper.ManProvider;
import org.wildfly.security.manager.WildFlySecurityManager;

import java.io.InputStream;

public class CliManProvider implements ManProvider {

    @Override
    public InputStream getManualDocument(String commandName) {
        if(commandName != null && commandName.length() > 0) {
            String filename = Config.getPathSeparator()+"man"+Config.getPathSeparator()+
                    commandName.toLowerCase()+".adoc";
            System.out.println("trying to get file: "+filename);
            return WildFlySecurityManager.getClassLoaderPrivileged(CliManProvider.class).getResourceAsStream(filename);
        }
        else
            return null;
    }
}
