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
import java.net.URL;
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
import org.jboss.as.cli.impl.aesh.commands.deployment.EnableAllCommand;
import org.jboss.as.cli.impl.aesh.commands.deployment.DeployArchiveCommand;
import org.jboss.as.cli.impl.aesh.commands.deployment.DeploymentCommand;
import org.jboss.as.cli.impl.aesh.commands.deployment.DeployFileCommand;
import org.jboss.as.cli.impl.aesh.commands.deployment.ListCommand;
import org.jboss.as.cli.impl.aesh.commands.deployment.EnableCommand;
import org.jboss.as.cli.impl.aesh.commands.deployment.DeployUrlCommand;
import org.wildfly.core.cli.command.aesh.FileConverter;
import org.jboss.as.cli.impl.aesh.converter.HeadersConverter;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.BatchCompliantCommand;
import org.wildfly.core.cli.command.DMRCommand;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;
import org.wildfly.core.cli.command.aesh.activator.HideOptionActivator;

/**
 *
 * @author jdenise@redhat.com
 */
@Deprecated
@CommandDefinition(name = "deploy", description = "", activator = DeployActivator.class)
public class Deploy extends DeploymentCommand implements BatchCompliantCommand {

    @Deprecated
    @Option(hasValue = false, activator = HideOptionActivator.class)
    private boolean help;

    @Deprecated
    @Option(hasValue = false, activator = HideOptionActivator.class)
    private boolean force;

    @Deprecated
    @Option(hasValue = false, shortName = 'l', activator = HideOptionActivator.class)
    private boolean l;

    @Deprecated
    @Arguments(valueSeparator = ',',
            activator = HideOptionActivator.class, converter = FileConverter.class)
    private List<File> path;

    @Deprecated
    @Option(activator = HideOptionActivator.class, converter
            = DeployUrlCommand.UrlConverter.class)
    private URL url;

    @Deprecated
    @Option(activator = HideOptionActivator.class)
    private String name;

    @Deprecated
    @Option(hasValue = true, name = "runtime-name", required = false,
            activator = HideOptionActivator.class)
    private String rtName;

    @Deprecated
    @Option(name = "server-groups", activator = HideOptionActivator.class,
            required = false)
    protected String serverGroups;

    @Deprecated
    @Option(name = "all-server-groups", activator = HideOptionActivator.class,
            hasValue = false, required = false)
    protected boolean allServerGroups;

    @Deprecated
    @Option(converter = HeadersConverter.class,
            required = false, activator = HideOptionActivator.class)
    protected ModelNode headers;

    @Deprecated
    @Option(hasValue = false, required = false, activator = HideOptionActivator.class)
    protected boolean disabled;

    @Deprecated
    @Option(hasValue = false, activator = HideOptionActivator.class, required = false)
    private boolean unmanaged;

    @Deprecated
    @Option(hasValue = true, required = false, activator = HideOptionActivator.class)
    private String script;

    @Deprecated
    private static final String ALL = "*";

    public Deploy(CommandContext ctx) {
        super(ctx);
    }

    @Deprecated
    @Override
    public CommandResult execute(CLICommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        if (help) {
            try {
                Util.printLegacyHelp(commandInvocation.getCommandContext(), "deploy");
            } catch (CommandLineException ex) {
                throw new CommandException(ex);
            }
            return CommandResult.SUCCESS;
        }

        boolean noOptions = script == null && !unmanaged
                && !disabled && headers == null && !allServerGroups
                && serverGroups == null && rtName == null && name == null
                && url == null && (path == null || path.isEmpty()) && !force;

        if (l || noOptions) {
            ListCommand.listDeployments(commandInvocation, l);
            return CommandResult.SUCCESS;
        }

        if ((path == null || path.isEmpty()) && url == null) {
            if (name == null) {
                throw new CommandException("Filesystem path, --url or --name is "
                        + " required.");
            }
            if (name.equals(ALL)) {
                if (force || disabled) {
                    throw new CommandException("force and disabled can't be used "
                            + "when deploying all disabled deployments");
                }
                EnableAllCommand command = new EnableAllCommand(getCommandContext(),
                        getPermissions());
                command.allServerGroups = allServerGroups;
                command.headers = headers;
                command.serverGroups = serverGroups;
                return command.execute(commandInvocation);

            } else {
                EnableCommand command = new EnableCommand(getCommandContext(),
                        getPermissions());
                command.allServerGroups = allServerGroups;
                command.headers = headers;
                command.serverGroups = serverGroups;
                command.name = new ArrayList<>();
                command.name.add(name);
                return command.execute(commandInvocation);
            }
        }

        if (path != null && !path.isEmpty()) {
            if (url != null) {
                throw new CommandException("Filesystem path and --url can't be used together.");
            }
            Command c;
            if (DeployArchiveCommand.isCliArchive(path.get(0))) {
                DeployArchiveCommand command = new DeployArchiveCommand(getCommandContext(), getPermissions());
                command.file = path;
                command.script = script;
                c = command;
            } else {
                DeployFileCommand command = new DeployFileCommand(getCommandContext(), getPermissions());
                command.allServerGroups = allServerGroups;
                command.disabled = disabled;
                command.file = path;
                command.replace = force;
                command.headers = headers;
                command.name = name;
                command.runtimeName = rtName;
                command.serverGroups = serverGroups;
                command.unmanaged = unmanaged;
                c = command;
            }
            return c.execute(commandInvocation);
        }

        if (url != null) {
            if (path != null && !path.isEmpty()) {
                throw new CommandException("Filesystem path and --url can't be "
                        + "used together.");
            }
            DeployUrlCommand command = new DeployUrlCommand(getCommandContext(),
                    getPermissions());
            command.allServerGroups = allServerGroups;
            command.disabled = disabled;
            command.url = new ArrayList<>();
            command.url.add(url);
            command.replace = force;
            command.headers = headers;
            command.runtimeName = rtName;
            command.serverGroups = serverGroups;
            return command.execute(commandInvocation);
        }
        throw new CommandException("Command action is missing.");
    }

    @Deprecated
    @Override
    public ModelNode buildRequest(CommandContext context) throws CommandFormatException {
        boolean noOptions = script == null && !unmanaged
                && !disabled && headers == null && !allServerGroups
                && serverGroups == null && rtName == null && name == null
                && url == null && (path == null || path.isEmpty()) && !force;

        if (l || noOptions) {
            throw new CommandFormatException("No option");
        }

        if ((path == null || path.isEmpty()) && url == null) {
            if (name == null) {
                throw new CommandFormatException("Command is missing arguments for "
                        + "non-interactive mode: 'deploy'.");
            }
            if (name.equals(ALL)) {
                if (force || disabled) {
                    throw new CommandFormatException("force and disabled can't be "
                            + "used when deploying all disabled deployments");
                }
                EnableAllCommand command = new EnableAllCommand(getCommandContext(),
                        getPermissions());
                command.allServerGroups = allServerGroups;
                command.headers = headers;
                command.serverGroups = serverGroups;
                return command.buildRequest(context);

            } else {
                EnableCommand command = new EnableCommand(getCommandContext(),
                        getPermissions());
                command.allServerGroups = allServerGroups;
                command.headers = headers;
                command.serverGroups = serverGroups;
                command.name = new ArrayList<>();
                command.name.add(name);
                return command.buildRequest(context);
            }
        }

        if (path != null && !path.isEmpty()) {
            if (url != null) {
                throw new CommandFormatException("Filesystem path and --url can't "
                        + "be used together.");
            }
            DMRCommand c;
            if (DeployArchiveCommand.isCliArchive(path.get(0))) {
                DeployArchiveCommand command = new DeployArchiveCommand(getCommandContext(),
                        getPermissions());
                command.file = path;
                command.script = script;
                c = command;
            } else {
                DeployFileCommand command = new DeployFileCommand(getCommandContext(),
                        getPermissions());
                command.allServerGroups = allServerGroups;
                command.disabled = disabled;
                command.file = path;
                command.replace = force;
                command.headers = headers;
                command.name = name;
                command.runtimeName = rtName;
                command.serverGroups = serverGroups;
                command.unmanaged = unmanaged;
                c = command;
            }
            return c.buildRequest(context);
        }

        if (url != null) {
            if (path != null && !path.isEmpty()) {
                throw new CommandFormatException("Filesystem path and --url can't "
                        + "be used together.");
            }
            DeployUrlCommand command = new DeployUrlCommand(getCommandContext(),
                    getPermissions());
            command.allServerGroups = allServerGroups;
            command.disabled = disabled;
            command.url = new ArrayList<>();
            command.url.add(url);
            command.replace = force;
            command.headers = headers;
            command.runtimeName = rtName;
            command.serverGroups = serverGroups;
            return command.buildRequest(context);
        }

        throw new CommandFormatException("Command action is missing.");
    }

    @Deprecated
    @Override
    public BatchResponseHandler buildBatchResponseHandler(CommandContext commandContext,
            Attachments attachments) {
        return null;
    }
}
