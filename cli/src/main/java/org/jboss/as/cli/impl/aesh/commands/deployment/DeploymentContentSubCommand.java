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

import java.io.IOException;
import java.util.List;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.option.Option;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.accesscontrol.AccessRequirement;
import org.jboss.as.cli.accesscontrol.AccessRequirementBuilder;
import org.jboss.as.cli.impl.aesh.commands.deployment.DeploymentActivators.ForceActivator;
import org.jboss.as.cli.impl.aesh.commands.deployment.DeploymentActivators.RuntimeNameActivator;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;
import org.wildfly.core.cli.command.aesh.activator.HiddenActivator;

/**
 * XXX jfdenise, all fields are public to be accessible from legacy view. To be
 * made private when removed.
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "deployment-deploy-content", description = "")
public abstract class DeploymentContentSubCommand extends DeploymentAbstractSubCommand {

    @Deprecated
    @Option(hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    @Option(hasValue = false, required = false, activator = ForceActivator.class,
            shortName = 'f')
    public boolean force;

    @Option(hasValue = false, required = false)
    public boolean disabled;

    @Option(hasValue = true, name = "runtime-name", required = false, activator
            = RuntimeNameActivator.class)
    public String runtimeName;

    DeploymentContentSubCommand(CommandContext ctx, DeploymentPermissions permissions) {
        super(ctx, permissions);
    }

    protected abstract void checkArgument() throws CommandException;

    protected abstract String getCommandName();

    @Override
    public CommandResult execute(CLICommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("deploy "
                    + getCommandName()));
            return CommandResult.SUCCESS;
        }
        checkArgument();
        CommandContext ctx = commandInvocation.getCommandContext();
        try {
            ModelNode request = buildRequest(commandInvocation.getCommandContext());
            final ModelNode result = execute(ctx, request);
            if (!Util.isSuccess(result)) {
                throw new CommandException(Util.getFailureDescription(result));
            }
        } catch (IOException e) {
            throw new CommandException("Failed to deploy", e);
        } catch (CommandFormatException ex) {
            throw new CommandException(ex);
        }
        return CommandResult.SUCCESS;
    }

    protected abstract ModelNode execute(CommandContext ctx, ModelNode request)
            throws IOException;

    protected ModelNode buildDeploymentRequest(CommandContext ctx, String op)
            throws OperationFormatException {
        return buildDeploymentRequest(ctx, op, getName(), runtimeName);
    }

    private ModelNode buildDeploymentRequest(CommandContext ctx, String op,
            String name, final String runtimeName) throws OperationFormatException {
        // replace
        final ModelNode request = new ModelNode();
        request.get(Util.OPERATION).set(op);
        request.get(Util.NAME).set(name);
        if (op.equals(Util.ADD)) { // replace is on root, add is on deployed artifact.
            request.get(Util.ADDRESS, Util.DEPLOYMENT).set(name);
        }
        if (runtimeName != null) {
            request.get(Util.RUNTIME_NAME).set(runtimeName);
        }
        final ModelNode content = request.get(Util.CONTENT).get(0);
        addContent(ctx, content);
        if (headers != null) {
            ModelNode opHeaders = request.get(Util.OPERATION_HEADERS);
            opHeaders.set(headers);
        }
        return request;
    }

    protected abstract void addContent(CommandContext ctx, ModelNode content)
            throws OperationFormatException;

    protected abstract String getName();

    @Override
    public ModelNode buildRequest(CommandContext context)
            throws CommandFormatException {
        // In case Batch or DMR call, must check that argument is valid.
        try {
            checkArgument();
        } catch (CommandException ex) {
            throw new CommandFormatException(ex);
        }
        CommandContext ctx = context;
        String name = getName();

        if (force) {
            if ((disabled && ctx.isDomainMode()) || serverGroups != null
                    || allServerGroups) {
                throw new CommandFormatException("--force only replaces the content "
                        + "in the deployment repository and can't be used in combination with any of "
                        + "disabled, --server-groups or --all-server-groups.");
            }

            if (Util.isDeploymentInRepository(name, ctx.getModelControllerClient())) {
                ModelNode request = buildDeploymentRequest(ctx,
                        Util.FULL_REPLACE_DEPLOYMENT, name, runtimeName);
                request.get(Util.ENABLED).set(!disabled);
                return request;
            } else if (ctx.isDomainMode()) {
                // add deployment to the repository (disabled in domain (i.e. not associated with any sg))
                ModelNode request = buildDeploymentRequest(ctx, Util.ADD,
                        name, runtimeName);
                return request;
            }
            // standalone mode will add and deploy
        }

        if (disabled) {
            if (serverGroups != null || allServerGroups) {
                throw new CommandFormatException("--server-groups and --all-server-groups "
                        + "can't be used in combination with --disabled.");
            }

            if (!ctx.isBatchMode() && Util.isDeploymentInRepository(name,
                    ctx.getModelControllerClient())) {
                throw new CommandFormatException("'" + name + "' already exists "
                        + "in the deployment repository (use --force"
                        + " to replace the existing content in the repository).");
            }

            // add deployment to the repository disabled
            ModelNode request = buildDeploymentRequest(ctx, Util.ADD, name,
                    runtimeName);
            return request;
        }

        ModelNode deployRequest = new ModelNode();
        if (ctx.isDomainMode()) {
            final List<String> sgList = getServerGroups(ctx);
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

        ModelNode compositeStep = createExtraStep(ctx);
        if (compositeStep != null) {
            final ModelNode composite = new ModelNode();
            composite.get(Util.OPERATION).set(Util.COMPOSITE);
            composite.get(Util.ADDRESS).setEmptyList();
            final ModelNode steps = composite.get(Util.STEPS);
            steps.add(compositeStep);
            steps.add(deployRequest);
            return composite;
        }
        if (headers != null) {
            ModelNode opHeaders = deployRequest.get(Util.OPERATION_HEADERS);
            opHeaders.set(headers);
        }
        return deployRequest;
    }

    protected List<String> getServerGroups(CommandContext ctx)
            throws CommandFormatException {
        return DeploymentCommand.getServerGroups(ctx, ctx.getModelControllerClient(),
                allServerGroups, serverGroups, null);
    }

    protected ModelNode createExtraStep(CommandContext ctx)
            throws CommandFormatException {
        if (!ctx.isBatchMode() && Util.isDeploymentInRepository(getName(),
                ctx.getModelControllerClient())) {
            throw new CommandFormatException("'" + getName() + "' already exists in "
                    + "the deployment repository (use "
                    + "--force to replace the existing content in the repository).");
        }

        ModelNode request = buildDeploymentRequest(ctx, Util.ADD);
        request.get(Util.ADDRESS, Util.DEPLOYMENT).set(getName());
        if (ctx.isDomainMode()) {
            request.get(Util.ENABLED).set(true);
        }

        return request;
    }

    @Override
    protected AccessRequirement buildAccessRequirement(CommandContext ctx) {
        return AccessRequirementBuilder.Factory.create(ctx)
                .any()
                .requirement(getPermissions().getFullReplacePermission())
                .requirement(getPermissions().getAddOrReplacePermission())
                .requirement(getPermissions().getDeployPermission())
                .build();
    }
}
