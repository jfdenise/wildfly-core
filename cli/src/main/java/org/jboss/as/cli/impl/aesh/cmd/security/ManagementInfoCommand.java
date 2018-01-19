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

import org.jboss.as.cli.impl.aesh.cmd.security.model.AuthFactorySpec;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.aesh.cmd.ConnectedActivator;
import org.jboss.as.cli.impl.aesh.cmd.security.model.ManagementInterfaces;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "display-management-info", description = "", activator = ConnectedActivator.class)
public class ManagementInfoCommand implements Command<CLICommandInvocation> {

    @Override
    public CommandResult execute(CLICommandInvocation commandInvocation) throws CommandException, InterruptedException {
        try {
            CommandContext context = commandInvocation.getCommandContext();
            context.printLine("");
            context.printLine(Util.HTTP_INTERFACE);
            context.printLine("");
            SecurityCommand.displaySSLSecurityInfo(context, ManagementInterfaces.getManagementInterfaceSSLContextName(context, Util.HTTP_INTERFACE));
            context.printLine("");
            SecurityCommand.displayAuthSecurityInfo(context, ManagementInterfaces.getManagementInterfaceHTTPFactoryName(context),
                    Util.HTTP_INTERFACE, AuthFactorySpec.HTTP);
            context.printLine("");
            SecurityCommand.displayAuthSecurityInfo(context, ManagementInterfaces.getManagementInterfaceSaslFactoryName(Util.HTTP_INTERFACE, context),
                    Util.HTTP_INTERFACE, AuthFactorySpec.SASL);
            context.printLine("");
            context.printLine(Util.NATIVE_INTERFACE);
            context.printLine("");
            SecurityCommand.displaySSLSecurityInfo(context, ManagementInterfaces.getManagementInterfaceSSLContextName(context, Util.NATIVE_INTERFACE));
            context.printLine("");
            SecurityCommand.displayAuthSecurityInfo(context, ManagementInterfaces.getManagementInterfaceSaslFactoryName(Util.NATIVE_INTERFACE, context),
                    Util.NATIVE_INTERFACE, AuthFactorySpec.SASL);
            context.printLine("");

        } catch (Exception ex) {
            throw new CommandException(ex.getLocalizedMessage());
        }
        return CommandResult.SUCCESS;
    }
}
