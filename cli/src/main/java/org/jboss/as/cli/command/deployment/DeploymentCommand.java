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
package org.jboss.as.cli.command.deployment;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.GroupCommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.GroupCommand;
import org.jboss.as.cli.Attachments;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.accesscontrol.AccessRequirement;
import org.jboss.as.cli.accesscontrol.AccessRequirementBuilder;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.aesh.converter.HeadersConverter;
import org.jboss.as.cli.command.ControlledCommandActivator;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.BatchCompliantCommand;
import org.wildfly.core.cli.command.CliCommandContext;
import org.wildfly.core.cli.command.CliCommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
@GroupCommandDefinition(name = "deployment", description = "", activator = ControlledCommandActivator.class)
public class DeploymentCommand extends DeploymentControlledCommand
        implements GroupCommand<CliCommandInvocation>, BatchCompliantCommand {

    @Deprecated
    @Option(hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    @Deprecated
    @Option(hasValue = false, activator = HiddenActivator.class)
    private boolean force;

    @Deprecated
    @Option(hasValue = false, shortName = 'l', activator = HiddenActivator.class)
    private boolean l;

    @Deprecated
    @Arguments(valueSeparator = ',',
            activator = HiddenActivator.class)
    private List<String> path;

    @Deprecated
    @Option(activator = HiddenActivator.class, converter
            = DeploymentUrlCommand.UrlConverter.class)
    private URL url;

    @Deprecated
    @Option(activator = HiddenActivator.class)
    private String name;

    @Deprecated
    @Option(hasValue = true, name = "runtime-name", required = false,
            activator = HiddenActivator.class)
    private String rtName;

    @Deprecated
    @Option(name = "server-groups", activator = HiddenActivator.class,
            required = false)
    protected String serverGroups;

    @Deprecated
    @Option(name = "all-server-groups", activator = HiddenActivator.class,
            hasValue = false, required = false)
    protected boolean allServerGroups;

    @Deprecated
    @Option(converter = HeadersConverter.class,
            required = false, activator = HiddenActivator.class)
    protected ModelNode headers;

    @Deprecated
    @Option(hasValue = false, required = false, activator = HiddenActivator.class)
    protected boolean disabled;

    @Deprecated
    @Option(hasValue = false, activator = HiddenActivator.class, required = false)
    private boolean unmanaged;

    @Deprecated
    @Option(hasValue = true, required = false, activator = HiddenActivator.class)
    private String script;

    @Deprecated
    private static final String ALL = "<all>";

    public DeploymentCommand(CommandContext ctx) {
        super(ctx, new DeploymentPermissions(ctx));
        DefaultOperationRequestAddress requiredAddress
                = new DefaultOperationRequestAddress();
        requiredAddress.toNodeType(Util.DEPLOYMENT);
        addRequiredPath(requiredAddress);
    }

    @Deprecated
    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("deploy"));
            return CommandResult.SUCCESS;
        }

        boolean noOptions = script == null && !unmanaged
                && !disabled && headers == null && !allServerGroups
                && serverGroups == null && rtName == null && name == null
                && url == null && (path == null || path.isEmpty()) && !force;

        if (l || noOptions) {
            DeploymentListCommand.listDeployments(commandInvocation, l);
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
                DeploymentAllCommand command = new DeploymentAllCommand(getCommandContext(),
                        getPermissions());
                command.allServerGroups = allServerGroups;
                command.headers = headers;
                command.serverGroups = serverGroups;
                return command.execute(commandInvocation);

            } else {
                DeploymentRedeployCommand command = new DeploymentRedeployCommand(getCommandContext(),
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
            DeploymentFileCommand command = new DeploymentFileCommand(getCommandContext(), getPermissions());
            command.allServerGroups = allServerGroups;
            command.disabled = disabled;
            command.file = path;
            command.force = force;
            command.headers = headers;
            command.name = name;
            command.runtimeName = rtName;
            command.script = script;
            command.serverGroups = serverGroups;
            command.unmanaged = unmanaged;
            return command.execute(commandInvocation);
        }

        if (url != null) {
            if (path != null && !path.isEmpty()) {
                throw new CommandException("Filesystem path and --url can't be "
                        + "used together.");
            }
            DeploymentUrlCommand command = new DeploymentUrlCommand(getCommandContext(),
                    getPermissions());
            command.allServerGroups = allServerGroups;
            command.disabled = disabled;
            command.url = new ArrayList<>();
            command.url.add(url);
            command.force = force;
            command.headers = headers;
            command.runtimeName = rtName;
            command.serverGroups = serverGroups;
            return command.execute(commandInvocation);
        }
        throw new CommandException("Command action is missing.");
    }

    @Override
    public List<Command> getCommands() {
        List<Command> commands = new ArrayList<>();
        commands.add(new DeploymentRedeployCommand(getCommandContext(), getPermissions()));
        commands.add(new DeploymentAllCommand(getCommandContext(), getPermissions()));
        commands.add(new DeploymentUrlCommand(getCommandContext(), getPermissions()));
        commands.add(new DeploymentFileCommand(getCommandContext(), getPermissions()));
        commands.add(new DeploymentListCommand(getCommandContext(), getPermissions()));
        commands.add(new DeploymentInfoCommand(getCommandContext(), getPermissions()));

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

    @Deprecated
    @Override
    public BatchResponseHandler buildBatchResponseHandler(CliCommandContext commandContext,
            Attachments attachments) throws CommandException {
        return null;
    }

    @Deprecated
    @Override
    public ModelNode buildRequest(CliCommandContext context) throws CommandFormatException {
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
                DeploymentAllCommand command = new DeploymentAllCommand(getCommandContext(),
                        getPermissions());
                command.allServerGroups = allServerGroups;
                command.headers = headers;
                command.serverGroups = serverGroups;
                return command.buildRequest(context);

            } else {
                DeploymentRedeployCommand command = new DeploymentRedeployCommand(getCommandContext(),
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
            DeploymentFileCommand command = new DeploymentFileCommand(getCommandContext(),
                    getPermissions());
            command.allServerGroups = allServerGroups;
            command.disabled = disabled;
            command.file = path;
            command.force = force;
            command.headers = headers;
            command.name = name;
            command.runtimeName = rtName;
            command.script = script;
            command.serverGroups = serverGroups;
            command.unmanaged = unmanaged;
            return command.buildRequest(context);
        }

        if (url != null) {
            if (path != null && !path.isEmpty()) {
                throw new CommandFormatException("Filesystem path and --url can't "
                        + "be used together.");
            }
            DeploymentUrlCommand command = new DeploymentUrlCommand(getCommandContext(),
                    getPermissions());
            command.allServerGroups = allServerGroups;
            command.disabled = disabled;
            command.url = new ArrayList<>();
            command.url.add(url);
            command.force = force;
            command.headers = headers;
            command.runtimeName = rtName;
            command.serverGroups = serverGroups;
            return command.buildRequest(context);
        }

        throw new CommandFormatException("Command action is missing.");
    }
}
