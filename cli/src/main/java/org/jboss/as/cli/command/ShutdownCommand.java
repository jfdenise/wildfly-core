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
package org.jboss.as.cli.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.accesscontrol.AccessRequirement;
import org.jboss.as.cli.accesscontrol.AccessRequirementBuilder;
import org.jboss.as.cli.accesscontrol.PerNodeOperationAccess;
import org.jboss.as.cli.aesh.activator.DomainOptionActivator;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocation;
import org.jboss.as.cli.embedded.EmbeddedProcessLaunch;
import org.jboss.as.cli.impl.CLIModelControllerClient;
import org.jboss.as.cli.impl.CommaSeparatedCompleter;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.CliCommandContext;
import org.wildfly.core.cli.command.CliCommandInvocation;
import org.wildfly.core.cli.command.DMRCommand;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "shutdown", description = "", activator = ShutdownActivator.class)
public class ShutdownCommand implements Command<CliCommandInvocation>, DMRCommand {

    public static class HostCompleter implements OptionCompleter<CliCompleterInvocation> {

        @Override
        public void complete(CliCompleterInvocation cliCompleterInvocation) {
            List<String> candidates = new ArrayList<>();
            int pos = 0;
            if (cliCompleterInvocation.getGivenCompleteValue() != null) {
                pos = cliCompleterInvocation.getGivenCompleteValue().length();
            }
            CommandLineCompleter completer = new CommaSeparatedCompleter() {
                @Override
                protected Collection<String> getAllCandidates(CommandContext ctx) {
                    ShutdownCommand cmd = (ShutdownCommand) cliCompleterInvocation.getCommand();
                    return cmd.hostShutdownPermission.getAllowedOn(ctx);

                }
            };
            int cursor = completer.complete(cliCompleterInvocation.getCommandContext().getLegacyCommandContext(),
                    cliCompleterInvocation.getGivenCompleteValue(),
                    pos, candidates);

            cliCompleterInvocation.addAllCompleterValues(candidates);
            cliCompleterInvocation.setOffset(pos - cursor);
        }
    }

    @Deprecated
    @Option(hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    @Option
    private Boolean restart;

    @Option
    private Integer timeout;

    @Option(activator = DomainOptionActivator.class, completer = HostCompleter.class)
    private String host;

    private final AtomicReference<EmbeddedProcessLaunch> embeddedServerRef;
    private PerNodeOperationAccess hostShutdownPermission;

    private final AccessRequirement access;

    public ShutdownCommand(CommandContext ctx, AtomicReference<EmbeddedProcessLaunch> embeddedServerRef) {
        this.embeddedServerRef = embeddedServerRef;
        this.access = setupAccessRequirement(ctx);
    }

    AccessRequirement getAccessRequirement() {
        return access;
    }

    AtomicReference<EmbeddedProcessLaunch> getServerReference() {
        return embeddedServerRef;
    }

    private AccessRequirement setupAccessRequirement(CommandContext ctx) {
        hostShutdownPermission = new PerNodeOperationAccess(ctx, Util.HOST, null, Util.SHUTDOWN);
        return AccessRequirementBuilder.Factory.create(ctx).any()
                .operation(Util.SHUTDOWN)
                .requirement(hostShutdownPermission)
                .build();
    }

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("shutdown"));
            return CommandResult.SUCCESS;
        }
        ModelControllerClient client = commandInvocation.getCommandContext().getModelControllerClient();
        if (client == null) {
            throw new CommandException("Connection is not available.");
        }

        // I don't see how this can happen, the command is not available for embedded.
        if (embeddedServerRef != null && embeddedServerRef.get() != null) {
            embeddedServerRef.get().stop();
            return CommandResult.SUCCESS;
        }

        if (!(client instanceof CLIModelControllerClient)) {
            throw new CommandException("Unsupported ModelControllerClient implementation " + client.getClass().getName());
        }
        final CLIModelControllerClient cliClient = (CLIModelControllerClient) client;

        CommandContext ctx = commandInvocation.getCommandContext().getLegacyCommandContext();
        final ModelNode op;
        try {
            op = buildRequest(commandInvocation.getCommandContext());
        } catch (CommandFormatException ex) {
            throw new CommandException(ex);
        }

        boolean disconnect = true;
        if ((restart != null && restart) || ctx.isDomainMode()
                && !isLocalHost(ctx.getModelControllerClient(), host)) {
            disconnect = false;
        }

        try {
            final ModelNode response = cliClient.execute(op, true);
            if (!Util.isSuccess(response)) {
                throw new CommandException(Util.getFailureDescription(response));
            }
        } catch (IOException e) {
            // if it's not connected, it's assumed the connection has already been shutdown
            if (cliClient.isConnected()) {
                StreamUtils.safeClose(cliClient);
                throw new CommandException("Failed to execute :shutdown", e);
            }
        }

        if (disconnect) {
            ctx.disconnectController();
        } else {
            // if I try to reconnect immediately, it'll hang for 5 sec
            // which the default connection timeout for model controller client
            // waiting half a sec on my machine works perfectly
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new CommandException("Interrupted while pausing before reconnecting.", e);
            }
            try {
                cliClient.ensureConnected(ctx.getConfig().getConnectionTimeout() + 1000);
            } catch (CommandLineException e) {
                ctx.disconnectController();
                throw new CommandException(e);
            }
        }
        return CommandResult.SUCCESS;
    }

    private boolean isLocalHost(ModelControllerClient client, String host) throws CommandException {
        ModelNode request = new ModelNode();
        request.get(Util.ADDRESS).setEmptyList();
        request.get(Util.OPERATION).set(Util.READ_ATTRIBUTE);
        request.get(Util.NAME).set(Util.LOCAL_HOST_NAME);
        ModelNode response;
        try {
            response = client.execute(request);
        } catch (IOException e) {
            throw new CommandException("Failed to read attribute " + Util.LOCAL_HOST_NAME, e);
        }
        if (!Util.isSuccess(response)) {
            throw new CommandException("Failed to read attribute " + Util.LOCAL_HOST_NAME
                    + ": " + Util.getFailureDescription(response));
        }
        ModelNode result = response.get(Util.RESULT);
        if (!result.isDefined()) {
            throw new CommandException("The result is not defined for attribute " + Util.LOCAL_HOST_NAME + ": " + result);
        }

        return result.asString().equals(host);
    }

    @Override
    public ModelNode buildRequest(CliCommandContext context) throws CommandFormatException {
        final ModelNode op = new ModelNode();
        if (context.getLegacyCommandContext().isDomainMode()) {
            if (host == null) {
                throw new CommandFormatException("Missing required argument --host ");
            }
            op.get(Util.ADDRESS).add(Util.HOST, host);
        } else {
            if (host != null) {
                throw new CommandFormatException("--host is not allowed in the standalone mode.");
            }
            op.get(Util.ADDRESS).setEmptyList();
        }
        op.get(Util.OPERATION).set(Util.SHUTDOWN);
        setBooleanArgument(op, restart, Util.RESTART);
        setIntArgument(op, timeout, Util.TIMEOUT);
        return op;
    }

    private void setBooleanArgument(final ModelNode op, Boolean arg, String paramName)
            throws CommandFormatException {
        if (arg == null) {
            return;
        }
        op.get(paramName).set(arg);
    }

    private void setIntArgument(final ModelNode op, Integer arg, String paramName)
            throws CommandFormatException {
        if (arg == null) {
            return;
        }
        op.get(paramName).set(arg);
    }
}
