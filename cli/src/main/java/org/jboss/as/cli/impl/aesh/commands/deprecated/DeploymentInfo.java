/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.cli.impl.aesh.commands.deprecated;

import java.util.ArrayList;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.option.Option;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.impl.aesh.commands.deployment.AbstractDeployCommand;
import org.jboss.as.cli.impl.aesh.commands.deployment.security.CommandWithPermissions;
import org.jboss.as.cli.impl.aesh.commands.deployment.InfoCommand;
import org.jboss.as.cli.impl.aesh.commands.deployment.security.AccessRequirements;
import org.jboss.as.cli.impl.aesh.commands.deployment.security.Activators;
import org.jboss.as.cli.impl.aesh.commands.deployment.security.Permissions;
import org.jboss.as.cli.impl.aesh.converter.HeadersConverter;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.DMRCommand;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
@Deprecated
@CommandDefinition(name = "deployment-info", description = "", activator = DeploymentInfoActivator.class)
public class DeploymentInfo extends CommandWithPermissions
        implements Command<CLICommandInvocation>, DMRCommand {

    @Deprecated
    @Option(hasValue = false, activator = HideOptionActivator.class)
    protected boolean help;

    @Deprecated
    @Option(name = "name", activator = HideOptionActivator.class)
    private String name;

    @Deprecated
    @Option(converter = HeadersConverter.class,
            required = false)
    protected ModelNode headers;

    @Option(name = "server-group", activator = Activators.ServerGroupsActivator.class,
            completer = AbstractDeployCommand.ServerGroupsCompleter.class,
            required = false)
    protected String serverGroup;
    public DeploymentInfo(CommandContext ctx, Permissions permissions) {
        super(ctx, AccessRequirements.infoAccess(permissions), permissions);
    }

    @Override
    public CommandResult execute(CLICommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        if (help) {
            try {
                Util.printLegacyHelp(commandInvocation.getCommandContext(), "deployment-info");
            } catch (CommandLineException ex) {
                throw new CommandException(ex);
            }
            return CommandResult.SUCCESS;
        }
        InfoCommand ic = new InfoCommand(getCommandContext(), getPermissions());
        ic.headers = headers;
        if (name != null) {
            ic.name = new ArrayList<>();
            ic.name.add(name);
        }
        ic.serverGroup = serverGroup;
        return ic.execute(commandInvocation);
    }

    @Override
    public ModelNode buildRequest(CommandContext context) throws CommandFormatException {
        InfoCommand ic = new InfoCommand(getCommandContext(), getPermissions());
        ic.headers = headers;
        if (name != null) {
            ic.name = new ArrayList<>();
            ic.name.add(name);
        }
        ic.serverGroup = serverGroup;
        return ic.buildRequest(context);
    }
}
