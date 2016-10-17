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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.Attachments;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.accesscontrol.AccessRequirement;
import org.jboss.as.cli.accesscontrol.AccessRequirementBuilder;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.aesh.completer.HeadersCompleter;
import org.jboss.as.cli.aesh.converter.HeadersConverter;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocation;
import org.jboss.as.cli.command.ControlledCommandActivator;
import org.jboss.as.cli.impl.CommaSeparatedCompleter;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.BatchCompliantCommand;
import org.wildfly.core.cli.command.CliCommandContext;
import org.wildfly.core.cli.command.CliCommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "undeploy", description = "", activator = ControlledCommandActivator.class)
public class DeploymentUndeployCommand extends DeploymentControlledCommand
        implements Command<CliCommandInvocation>, BatchCompliantCommand {

    public static class ServerGroupsCompleter implements
            OptionCompleter<CliCompleterInvocation> {

        @Override
        public void complete(CliCompleterInvocation completerInvocation) {
            DeploymentUndeployCommand rc = (DeploymentUndeployCommand) completerInvocation.getCommand();

            CommaSeparatedCompleter comp = new CommaSeparatedCompleter() {
                @Override
                protected Collection<String> getAllCandidates(CommandContext ctx) {
                    try {
                        return Util.getServerGroupsReferencingDeployment(rc.name.get(0), ctx.getModelControllerClient());
                    } catch (CommandLineException ex) {
                        return Collections.emptyList();
                    }
                }
            };
            List<String> candidates = new ArrayList<>();
            int offset = comp.complete(completerInvocation.getCommandContext().
                    getLegacyCommandContext(),
                    completerInvocation.getGivenCompleteValue(), 0, candidates);
            completerInvocation.addAllCompleterValues(candidates);
            completerInvocation.setOffset(offset);
        }
    }

    @Deprecated
    @Option(hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    @Option(name = "server-groups", activator = DeploymentActivators.UndeployServerGroupsActivator.class,
            completer = ServerGroupsCompleter.class, required = false)
    public String serverGroups;

    @Option(name = "all-relevant-server-groups", activator = DeploymentActivators.AllRelevantServerGroupsActivator.class,
            hasValue = false, required = false)
    public boolean allRelevantServerGroups;

    @Option(converter = HeadersConverter.class, completer = HeadersCompleter.class,
            required = false)
    public ModelNode headers;

    @Option(hasValue = false, name = "keep-content")
    public boolean keepContent;

    // Argument comes first, aesh behavior.
    @Arguments(valueSeparator = ',', activator = DeploymentActivators.UndeployNameActivator.class,
            completer = DeploymentRedeployCommand.NameCompleter.class)
    public List<String> name;

    // XXX jfdenise, is public for compat reason. Make it private when removing compat code.
    public DeploymentUndeployCommand(CommandContext ctx, DeploymentPermissions permissions) {
        super(ctx, permissions);
    }

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("deployment undeploy"));
            return CommandResult.SUCCESS;
        }
        if (name == null || name.isEmpty()) {
            throw new CommandException("No deployment name");
        }
        undeployName(commandInvocation, name.get(0), allRelevantServerGroups,
                serverGroups, keepContent, headers);
        return CommandResult.SUCCESS;
    }

    @Override
    public BatchResponseHandler buildBatchResponseHandler(CliCommandContext commandContext,
            Attachments attachments) throws CommandException {
        return null;
    }

    static void undeployName(CliCommandInvocation commandInvocation, String name,
            boolean allServerGroups, String serverGroups, boolean keepContent, ModelNode headers)
            throws CommandException {
        try {
            ModelNode request = buildRequest(commandInvocation.getCommandContext(),
                    name, allServerGroups, serverGroups, keepContent, headers);
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
        return buildRequest(context, name.get(0), allRelevantServerGroups,
                serverGroups, keepContent, headers);
    }

    private static ModelNode buildRequest(CliCommandContext context, String name,
            boolean allRelevantServerGroups, String serverGroupsStr,
            boolean keepContent, ModelNode headers) throws CommandFormatException {
        CommandContext ctx = context.getLegacyCommandContext();
        final ModelNode composite = new ModelNode();
        composite.get(Util.OPERATION).set(Util.COMPOSITE);
        composite.get(Util.ADDRESS).setEmptyList();
        final ModelNode steps = composite.get(Util.STEPS);

        final ModelControllerClient client = ctx.getModelControllerClient();
        DefaultOperationRequestBuilder builder;

        final List<String> deploymentNames;
        if (name.indexOf('*') < 0) {
            deploymentNames = Collections.singletonList(name);
        } else {
            deploymentNames = Util.getMatchingDeployments(client, name, null);
            if (deploymentNames.isEmpty()) {
                throw new CommandFormatException("No deployment matched wildcard expression " + name);
            }
        }

        for (String deploymentName : deploymentNames) {

            final List<String> serverGroups;
            if (ctx.isDomainMode()) {
                if (allRelevantServerGroups) {
                    if (keepContent) {
                        serverGroups = Util.getAllEnabledServerGroups(deploymentName, client);
                    } else {
                        try {
                            serverGroups = Util.getServerGroupsReferencingDeployment(deploymentName, client);
                        } catch (CommandLineException e) {
                            throw new CommandFormatException("Failed to retrieve all referencing server groups", e);
                        }
                    }
                } else if (serverGroupsStr == null) {
                    //throw new OperationFormatException("Either --all-relevant-server-groups or --server-groups must be specified.");
                    serverGroups = Collections.emptyList();
                } else {
                    serverGroups = Arrays.asList(serverGroupsStr.split(","));
                }

                if (serverGroups.isEmpty()) {
                    if (keepContent) {
                        throw new OperationFormatException("None of the server groups is specified or references specified deployment.");
                    }
                } else {
                    for (String group : serverGroups) {
                        ModelNode groupStep = Util.configureDeploymentOperation(Util.UNDEPLOY, deploymentName, group);
                        steps.add(groupStep);
                        if (!keepContent) {
                            groupStep = Util.configureDeploymentOperation(Util.REMOVE, deploymentName, group);
                            steps.add(groupStep);
                        }
                    }
                }
            } else if (Util.isDeployedAndEnabledInStandalone(deploymentName, client)) {
                builder = new DefaultOperationRequestBuilder();
                builder.setOperationName(Util.UNDEPLOY);
                builder.addNode(Util.DEPLOYMENT, deploymentName);
                steps.add(builder.buildRequest());
            }
        }

        if (!keepContent) {
            for (String deploymentName : deploymentNames) {
                builder = new DefaultOperationRequestBuilder();
                builder.setOperationName(Util.REMOVE);
                builder.addNode(Util.DEPLOYMENT, deploymentName);
                steps.add(builder.buildRequest());
            }
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
                .requirement(getPermissions().getMainRemovePermission())
                .requirement(getPermissions().getUndeployPermission())
                .build();
    }
}
