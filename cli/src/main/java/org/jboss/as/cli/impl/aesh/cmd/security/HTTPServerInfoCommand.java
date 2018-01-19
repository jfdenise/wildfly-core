/*
Copyright 2017 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package org.jboss.as.cli.impl.aesh.cmd.security;

import java.util.List;
import org.jboss.as.cli.impl.aesh.cmd.security.model.AuthFactorySpec;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.option.Option;
import org.jboss.as.cli.CommandContext;
import static org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.OPT_SERVER_NAME;
import org.jboss.as.cli.impl.aesh.cmd.security.SecurityCommand.OptionCompleters;
import org.jboss.as.cli.impl.aesh.cmd.security.model.DefaultResourceNames;
import org.jboss.as.cli.impl.aesh.cmd.security.model.HTTPServer;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "display-http-server-info", description = "", activator = HttpServerCommandActivator.class)
public class HTTPServerInfoCommand implements Command<CLICommandInvocation> {

    @Option(name = OPT_SERVER_NAME, completer = OptionCompleters.ServerNameCompleter.class)
    String serverName;

    @Override
    public CommandResult execute(CLICommandInvocation commandInvocation) throws CommandException, InterruptedException {
        try {
            CommandContext context = commandInvocation.getCommandContext();
            context.printLine("");
            if (serverName == null) {
                serverName = DefaultResourceNames.getDefaultServerName(context);
            }
            context.printLine("HTTP Server: " + serverName);
            context.printLine("");
            SecurityCommand.displaySSLSecurityInfo(context, HTTPServer.getSSLContextName(serverName, context));
            context.printLine("");
            List<String> securityDomains = HTTPServer.getSecurityDomains(context);
            if (securityDomains.isEmpty()) {
                context.printLine("No application security domains.");
            } else {
                for (String securityDomain : securityDomains) {
                    context.printLine(securityDomain);
                    SecurityCommand.displayAuthSecurityInfo(context, HTTPServer.getSecurityDomainFactoryName(securityDomain, context),
                            "application security domain: " + securityDomain, AuthFactorySpec.HTTP);
                    context.printLine("");
                }
            }
        } catch (Exception ex) {
            throw new CommandException(ex.getLocalizedMessage());
        }
        return CommandResult.SUCCESS;
    }
}
