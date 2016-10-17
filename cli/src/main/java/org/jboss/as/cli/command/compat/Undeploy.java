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
package org.jboss.as.cli.command.compat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.Attachments;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.accesscontrol.AccessRequirement;
import org.jboss.as.cli.accesscontrol.AccessRequirementBuilder;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.aesh.converter.FileConverter;
import org.jboss.as.cli.aesh.converter.HeadersConverter;
import org.jboss.as.cli.command.deployment.DeploymentActivators;
import org.jboss.as.cli.command.deployment.DeploymentArchiveCommand;
import org.jboss.as.cli.command.deployment.DeploymentControlledCommand;
import org.jboss.as.cli.command.deployment.DeploymentListCommand;
import org.jboss.as.cli.command.deployment.DeploymentPermissions;
import org.jboss.as.cli.command.deployment.DeploymentRedeployCommand;
import org.jboss.as.cli.command.deployment.DeploymentUndeployArchiveCommand;
import org.jboss.as.cli.command.deployment.DeploymentUndeployCommand;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.BatchCompliantCommand;
import org.wildfly.core.cli.command.CliCommandContext;
import org.wildfly.core.cli.command.CliCommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
@Deprecated
@CommandDefinition(name = "undeploy", description = "", activator = DeployActivator.class)
public class Undeploy extends DeploymentControlledCommand
        implements Command<CliCommandInvocation>, BatchCompliantCommand {

    @Deprecated
    @Option(hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    @Deprecated
    @Option(hasValue = true, required = false, activator = HiddenActivator.class)
    protected String script;

    @Deprecated
    @Option(hasValue = true, required = false, activator = HiddenActivator.class, converter = FileConverter.class)
    protected File path;

    @Deprecated
    @Option(hasValue = false, shortName = 'l', activator = HiddenActivator.class)
    private boolean l;

    @Deprecated
    @Option(name = "server-groups", activator = HiddenActivator.class, required = false)
    protected String serverGroups;

    @Deprecated
    @Option(name = "all-relevant-server-groups", activator = HiddenActivator.class,
            hasValue = false, required = false)
    protected boolean allRelevantServerGroups;

    @Deprecated
    @Option(converter = HeadersConverter.class, required = false, activator = HiddenActivator.class)
    protected ModelNode headers;

    @Deprecated
    @Option(hasValue = false, name = "keep-content", activator = HiddenActivator.class)
    private boolean keepContent;

    // Argument comes first, aesh behavior.
    @Arguments(valueSeparator = ',', activator = DeploymentActivators.UndeployNameActivator.class,
            completer = DeploymentRedeployCommand.NameCompleter.class)
    protected List<String> name;
    public Undeploy(CommandContext ctx, DeploymentPermissions permissions) {
        super(ctx, permissions);
    }

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("undeploy"));
            return CommandResult.SUCCESS;
        }
        boolean noOptions = script == null
                && headers == null && !allRelevantServerGroups
                && serverGroups == null && path == null
                && !keepContent && (name == null || name.isEmpty());

        if (noOptions || l) {
            DeploymentListCommand.listDeployments(commandInvocation, l);
            return CommandResult.SUCCESS;
        }
        if (path != null) {
            if (DeploymentArchiveCommand.isCliArchive(path)) {
                DeploymentUndeployArchiveCommand command = new DeploymentUndeployArchiveCommand(getCommandContext(), getPermissions());
                command.file = new ArrayList<>();
                command.file.add(path);
                command.script = script;
                return command.execute(commandInvocation);
            }
        }

        if (name == null) {
            DeploymentListCommand.listDeployments(commandInvocation, l);
            return CommandResult.SUCCESS;
        }

        DeploymentUndeployCommand command = new DeploymentUndeployCommand(getCommandContext(), getPermissions());
        command.allRelevantServerGroups = allRelevantServerGroups;
        command.headers = headers;
        command.keepContent = keepContent;
        command.name = name;
        command.serverGroups = serverGroups;
        return command.execute(commandInvocation);
    }

    @Override
    public ModelNode buildRequest(CliCommandContext commandInvocation) throws CommandFormatException {
        if (path != null) {
            if (DeploymentArchiveCommand.isCliArchive(path)) {
                DeploymentUndeployArchiveCommand command = new DeploymentUndeployArchiveCommand(getCommandContext(), getPermissions());
                command.file = new ArrayList<>();
                command.file.add(path);
                command.script = script;
                return command.buildRequest(commandInvocation);
            }
        }

        if (name == null) {
            throw new CommandFormatException("Deployment name is missing.");
        }

        DeploymentUndeployCommand command = new DeploymentUndeployCommand(getCommandContext(), getPermissions());
        command.allRelevantServerGroups = allRelevantServerGroups;
        command.headers = headers;
        command.keepContent = keepContent;
        command.name = name;
        command.serverGroups = serverGroups;
        return command.buildRequest(commandInvocation);
    }

    @Override
    public BatchResponseHandler buildBatchResponseHandler(CliCommandContext commandContext, Attachments attachments) throws CommandException {
        return null;
    }

    @Override
    protected AccessRequirement buildAccessRequirement(CommandContext ctx) {
        return AccessRequirementBuilder.Factory.create(ctx)
                .any()
                .requirement(getPermissions().getListPermission())
                .requirement(getPermissions().getMainRemovePermission())
                .requirement(getPermissions().getUndeployPermission())
                .build();
    }

}
