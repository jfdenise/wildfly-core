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
import java.util.Collections;
import java.util.List;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.accesscontrol.AccessRequirement;
import org.jboss.as.cli.accesscontrol.AccessRequirementBuilder;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.CliCommandContext;
import org.wildfly.core.cli.command.CliCommandInvocation;
import org.wildfly.core.cli.command.DMRCommand;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "deploy-all", description = "")
public class DeploymentAllCommand extends DeploymentAbstractSubCommand implements DMRCommand {

    @Deprecated
    @Option(hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    DeploymentAllCommand(CommandContext ctx, DeploymentPermissions permissions) {
        super(ctx, permissions);
    }

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("deployment deploy-all"));
            return CommandResult.SUCCESS;
        }
        deployAll(commandInvocation, allServerGroups, serverGroups, headers);
        return CommandResult.SUCCESS;
    }

    static void deployAll(CliCommandInvocation commandInvocation,
            boolean allServerGroups,
            String serverGroups, ModelNode headers) throws CommandException {
        try {
            ModelNode request = buildRequest(commandInvocation.getCommandContext(),
                    allServerGroups, serverGroups, headers);
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
        return buildRequest(context, allServerGroups, serverGroups, headers);
    }

    private static ModelNode buildRequest(CliCommandContext context,
            boolean allServerGroups, String serverGroups, ModelNode headers)
            throws CommandFormatException {
        CommandContext ctx = context.getLegacyCommandContext();
        List<String> sgList = DeploymentCommand.getServerGroups(ctx,
                ctx.getModelControllerClient(),
                allServerGroups, serverGroups, null);
        if (sgList == null) {
            // No serverGroups means a null serverGroup.
            sgList = Collections.singletonList(null);
        }
        ModelControllerClient client = context.getModelControllerClient();
        final ModelNode composite = new ModelNode();
        composite.get(Util.OPERATION).set(Util.COMPOSITE);
        composite.get(Util.ADDRESS).setEmptyList();
        final ModelNode steps = composite.get(Util.STEPS);
        boolean empty = true;
        for (String serverGroup : sgList) {
            List<String> deployments = Util.getDeployments(client, serverGroup);
            for (String deploymentName : deployments) {
                try {
                    if (!Util.isEnabledDeployment(deploymentName,
                            client, serverGroup)) {
                        DefaultOperationRequestBuilder builder
                                = new DefaultOperationRequestBuilder();
                        if (serverGroup != null) {
                            builder.addNode(Util.SERVER_GROUP, serverGroup);
                        }
                        builder.addNode(Util.DEPLOYMENT, deploymentName);
                        builder.setOperationName(Util.DEPLOY);
                        steps.add(builder.buildRequest());
                        empty = false;
                    }
                } catch (IOException ex) {
                    throw new CommandFormatException(ex);
                }
            }
        }
        if (empty) {
            throw new CommandFormatException("No disabled deployment to deploy.");
        }
        if (headers != null) {
            ModelNode opHeaders = composite.get(Util.OPERATION_HEADERS);
            opHeaders.set(headers);
        }
        return composite;
    }

    @Override
    protected AccessRequirement buildAccessRequirement(CommandContext ctx) {
        return AccessRequirementBuilder.Factory.create(ctx)
                .any()
                .requirement(getPermissions().getDeployPermission())
                .build();
    }
}
