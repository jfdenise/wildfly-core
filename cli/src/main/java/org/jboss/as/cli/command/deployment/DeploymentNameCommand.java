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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.accesscontrol.AccessRequirement;
import org.jboss.as.cli.accesscontrol.AccessRequirementBuilder;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocation;
import org.jboss.as.cli.command.deployment.DeploymentActivators.NameActivator;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.CliCommandContext;
import org.wildfly.core.cli.command.CliCommandInvocation;
import org.wildfly.core.cli.command.DMRCommand;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "deploy-name", description = "")
public class DeploymentNameCommand extends DeploymentAbstractSubCommand implements DMRCommand {

    public static class NameCompleter
            implements OptionCompleter<CliCompleterInvocation> {

        @Override
        public void complete(CliCompleterInvocation completerInvocation) {
            if (completerInvocation.getCommandContext().getModelControllerClient() != null) {
                List<String> deployments
                        = Util.getDeployments(completerInvocation.getCommandContext().
                                getModelControllerClient());
                if (!deployments.isEmpty()) {
                    List<String> candidates = new ArrayList<>();
                    String opBuffer = completerInvocation.getGivenCompleteValue();
                    if (opBuffer.isEmpty()) {
                        candidates.addAll(deployments);
                    } else {
                        for (String name : deployments) {
                            if (name.startsWith(opBuffer)) {
                                candidates.add(name);
                            }
                        }
                        Collections.sort(candidates);
                    }
                    completerInvocation.addAllCompleterValues(candidates);
                }
            }
        }
    }

    @Deprecated
    @Option(hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    // Argument comes first, aesh behavior.
    @Arguments(valueSeparator = ',', activator = NameActivator.class,
            completer = NameCompleter.class)
    protected List<String> name;

    DeploymentNameCommand(CommandContext ctx, DeploymentPermissions permissions) {
        super(ctx, permissions);
    }

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("deployment deploy-name"));
            return CommandResult.SUCCESS;
        }
        if (name == null || name.isEmpty()) {
            throw new CommandException("No deployment name");
        }
        deployName(commandInvocation, name.get(0), allServerGroups, serverGroups, headers);
        return CommandResult.SUCCESS;
    }

    static void deployName(CliCommandInvocation commandInvocation, String name,
            boolean allServerGroups, String serverGroups, ModelNode headers)
            throws CommandException {
        try {
            ModelNode request = buildRequest(commandInvocation.getCommandContext(),
                    name, allServerGroups, serverGroups);
            if (headers != null) {
                ModelNode opHeaders = request.get(Util.OPERATION_HEADERS);
                opHeaders.set(headers);
            }
            final ModelNode result = commandInvocation.getCommandContext().
                    getModelControllerClient().execute(request);
            if (!Util.isSuccess(result)) {
                throw new CommandException(Util.getFailureDescription(result));
            }
        } catch (IOException e) {
            throw new CommandException("Failed to deploy", e);
        } catch (CommandFormatException ex) {
            throw new CommandException(ex);
        }
    }

    @Override
    public ModelNode buildRequest(CliCommandContext context)
            throws CommandFormatException {
        return buildRequest(context, name.get(0), allServerGroups, serverGroups);
    }

    private static ModelNode buildRequest(CliCommandContext context, String name,
            boolean allServerGroups, String serverGroups) throws CommandFormatException {
        CommandContext ctx = context.getLegacyCommandContext();
        ModelNode deployRequest = new ModelNode();
        if (ctx.isDomainMode()) {
            final List<String> sgList = DeploymentCommand.getServerGroups(ctx,
                    ctx.getModelControllerClient(),
                    allServerGroups, serverGroups, null);
            deployRequest.get(Util.OPERATION).set(Util.COMPOSITE);
            deployRequest.get(Util.ADDRESS).setEmptyList();
            ModelNode steps = deployRequest.get(Util.STEPS);
            for (String serverGroup : sgList) {
                steps.add(Util.configureDeploymentOperation(Util.ADD, name,
                        serverGroup));
            }
            for (String serverGroup : sgList) {
                steps.add(Util.configureDeploymentOperation(Util.DEPLOY, name,
                        serverGroup));
            }
        } else {
            if (serverGroups != null || allServerGroups) {
                throw new CommandFormatException("--all-server-groups and --server-groups "
                        + "can't appear in standalone mode.");
            }
            deployRequest.get(Util.OPERATION).set(Util.DEPLOY);
            deployRequest.get(Util.ADDRESS, Util.DEPLOYMENT).set(name);
        }

        if (!ctx.isBatchMode() && !Util.isDeploymentInRepository(name,
                ctx.getModelControllerClient())) {
            throw new CommandFormatException("'" + name + "' is not found among "
                    + "the registered deployments.");
        }

        return deployRequest;
    }

    @Override
    protected AccessRequirement buildAccessRequirement(CommandContext ctx) {
        return AccessRequirementBuilder.Factory.create(ctx)
                .any()
                .requirement(getPermissions().getDeployPermission())
                .build();
    }
}
