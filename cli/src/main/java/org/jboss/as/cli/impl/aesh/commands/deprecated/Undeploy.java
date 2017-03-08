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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.jboss.as.cli.Attachments;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.impl.aesh.commands.deployment.DeployArchiveCommand;
import org.jboss.as.cli.impl.aesh.commands.deployment.security.CommandWithPermissions;
import org.jboss.as.cli.impl.aesh.commands.deployment.DisableCommand;
import org.jboss.as.cli.impl.aesh.commands.deployment.ListCommand;
import org.jboss.as.cli.impl.aesh.commands.deployment.security.Permissions;
import org.jboss.as.cli.impl.aesh.commands.deployment.EnableCommand;
import org.jboss.as.cli.impl.aesh.commands.deployment.UndeployArchiveCommand;
import org.jboss.as.cli.impl.aesh.commands.deployment.UndeployCommand;
import org.jboss.as.cli.impl.aesh.commands.deployment.security.AccessRequirements;
import org.wildfly.core.cli.command.aesh.FileConverter;
import org.jboss.as.cli.impl.aesh.converter.HeadersConverter;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.BatchCompliantCommand;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;
import org.jboss.as.cli.impl.aesh.commands.deployment.security.Activators;

/**
 *
 * @author jdenise@redhat.com
 */
@Deprecated
@CommandDefinition(name = "undeploy", description = "", activator = DeployActivator.class)
public class Undeploy extends CommandWithPermissions
        implements Command<CLICommandInvocation>, BatchCompliantCommand {

    @Deprecated
    @Option(hasValue = false, activator = HideOptionActivator.class)
    private boolean help;

    @Deprecated
    @Option(hasValue = true, required = false, activator = HideOptionActivator.class)
    protected String script;

    @Deprecated
    @Option(hasValue = true, required = false, activator = HideOptionActivator.class, converter = FileConverter.class)
    protected File path;

    @Deprecated
    @Option(hasValue = false, shortName = 'l', activator = HideOptionActivator.class)
    private boolean l;

    @Deprecated
    @Option(name = "server-groups", activator = HideOptionActivator.class, required = false)
    protected String serverGroups;

    @Deprecated
    @Option(name = "all-relevant-server-groups", activator = HideOptionActivator.class,
            hasValue = false, required = false)
    protected boolean allRelevantServerGroups;

    @Deprecated
    @Option(converter = HeadersConverter.class, required = false, activator = HideOptionActivator.class)
    protected ModelNode headers;

    @Deprecated
    @Option(hasValue = false, name = "keep-content", activator = HideOptionActivator.class)
    private boolean keepContent;

    // Argument comes first, aesh behavior.
    @Arguments(valueSeparator = ',', activator = Activators.UndeployNameActivator.class,
            completer = EnableCommand.NameCompleter.class)
    protected List<String> name;
    public Undeploy(CommandContext ctx, Permissions permissions) {
        super(ctx, AccessRequirements.undeployLegacyAccess(permissions), permissions);
    }

    @Override
    public CommandResult execute(CLICommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (help) {
            try {
                Util.printLegacyHelp(commandInvocation.getCommandContext(), "undeploy");
            } catch (CommandLineException ex) {
                throw new CommandException(ex);
            }
            return CommandResult.SUCCESS;
        }
        boolean noOptions = script == null
                && headers == null && !allRelevantServerGroups
                && serverGroups == null && path == null
                && !keepContent && (name == null || name.isEmpty());

        if (noOptions || l) {
            ListCommand.listDeployments(commandInvocation, l);
            return CommandResult.SUCCESS;
        }
        if (path != null) {
            if (DeployArchiveCommand.isCliArchive(path)) {
                UndeployArchiveCommand command = new UndeployArchiveCommand(getCommandContext(), getPermissions());
                command.file = new ArrayList<>();
                command.file.add(path);
                command.script = script;
                return command.execute(commandInvocation);
            }
        }

        if (name == null) {
            ListCommand.listDeployments(commandInvocation, l);
            return CommandResult.SUCCESS;
        }

        UndeployCommand command = null;
        if (keepContent) {
            command = new DisableCommand(getCommandContext(), getPermissions());
        } else {
            command = new UndeployCommand(getCommandContext(), getPermissions());
        }
        command.allRelevantServerGroups = allRelevantServerGroups;
        command.headers = headers;
        command.name = name;
        command.serverGroups = serverGroups;

        return command.execute(commandInvocation);
    }

    @Override
    public ModelNode buildRequest(CommandContext commandInvocation) throws CommandFormatException {
        if (path != null) {
            if (DeployArchiveCommand.isCliArchive(path)) {
                UndeployArchiveCommand command = new UndeployArchiveCommand(getCommandContext(), getPermissions());
                command.file = new ArrayList<>();
                command.file.add(path);
                command.script = script;
                return command.buildRequest(commandInvocation);
            }
        }

        if (name == null) {
            throw new CommandFormatException("Deployment name is missing.");
        }

        UndeployCommand command = null;
        if (keepContent) {
            command = new DisableCommand(getCommandContext(), getPermissions());
        } else {
            command = new UndeployCommand(getCommandContext(), getPermissions());
        }
        command.allRelevantServerGroups = allRelevantServerGroups;
        command.headers = headers;
        command.name = name;
        command.serverGroups = serverGroups;
        return command.buildRequest(commandInvocation);
    }

    @Override
    public BatchResponseHandler buildBatchResponseHandler(CommandContext commandContext, Attachments attachments) {
        return null;
    }
}
