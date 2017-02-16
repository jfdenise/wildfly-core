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
package org.jboss.as.cli.impl.aesh.commands.deployment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommand;
import org.aesh.command.GroupCommandDefinition;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.accesscontrol.AccessRequirement;
import org.jboss.as.cli.accesscontrol.AccessRequirementBuilder;
import org.jboss.as.cli.impl.aesh.commands.activator.ControlledCommandActivator;
import org.jboss.as.controller.client.ModelControllerClient;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
@GroupCommandDefinition(name = "deployment", description = "", activator = ControlledCommandActivator.class)
public class DeploymentCommand extends DeploymentControlledCommand
        implements GroupCommand<CLICommandInvocation, Command> {

    public DeploymentCommand(CommandContext ctx) {
        super(ctx, new DeploymentPermissions(ctx));
    }

    @Deprecated
    @Override
    public CommandResult execute(CLICommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        throw new CommandException("Command action is missing.");
    }

    @Override
    public List<Command> getCommands() {
        List<Command> commands = new ArrayList<>();
        commands.add(new DeploymentRedeployCommand(getCommandContext(), getPermissions()));
        commands.add(new DeploymentAllCommand(getCommandContext(), getPermissions()));
        commands.add(new DeploymentArchiveCommand(getCommandContext(), getPermissions()));
        commands.add(new DeploymentUrlCommand(getCommandContext(), getPermissions()));
        commands.add(new DeploymentFileCommand(getCommandContext(), getPermissions()));
        commands.add(new DeploymentListCommand(getCommandContext(), getPermissions()));
        commands.add(new DeploymentInfoCommand(getCommandContext(), getPermissions()));
        commands.add(new DeploymentUndeployCommand(getCommandContext(), getPermissions()));
        commands.add(new DeploymentUndeployArchiveCommand(getCommandContext(), getPermissions()));

        return commands;
    }

    @Override
    protected AccessRequirement buildAccessRequirement(CommandContext ctx) {
        return AccessRequirementBuilder.Factory.create(ctx)
                .any()
                .requirement(getPermissions().getListPermission())
                .requirement(getPermissions().getFullReplacePermission())
                .requirement(getPermissions().getMainAddPermission())
                .requirement(getPermissions().getDeployPermission())
                .build();
    }

    static List<String> getServerGroups(CommandContext ctx,
            ModelControllerClient client,
            boolean allServerGroups, String serverGroups, File f)
            throws CommandFormatException {
        List<String> sgList = null;
        if (ctx.isDomainMode()) {
            if (allServerGroups) {
                if (serverGroups != null) {
                    throw new CommandFormatException("--all-server-groups and "
                            + "--server-groups can't appear in the same command");
                }
                sgList = Util.getServerGroups(client);
                if (sgList.isEmpty()) {
                    throw new CommandFormatException("No server group is available.");
                }
            } else if (serverGroups == null) {
                final StringBuilder buf = new StringBuilder();
                buf.append("One of ");
                if (f != null) {
                    buf.append("--disabled,");
                }
                buf.append(" --all-server-groups or --server-groups is missing.");
                throw new CommandFormatException(buf.toString());
            } else {
                sgList = Arrays.asList(serverGroups.split(","));
                if (sgList.isEmpty()) {
                    throw new CommandFormatException("Couldn't locate server "
                            + "group name in '--server-groups=" + serverGroups
                            + "'.");
                }
            }
        } else if (serverGroups != null || allServerGroups) {
            throw new CommandFormatException("--server-groups and --all-server-groups "
                    + "can't appear in standalone mode.");
        }
        return sgList;
    }
}
