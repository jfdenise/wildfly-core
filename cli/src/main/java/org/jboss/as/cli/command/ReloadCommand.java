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
import org.jboss.as.cli.Util;
import org.jboss.as.cli.accesscontrol.AccessRequirement;
import org.jboss.as.cli.accesscontrol.AccessRequirementBuilder;
import org.jboss.as.cli.accesscontrol.PerNodeOperationAccess;
import org.wildfly.core.cli.command.activator.DefaultDomainOptionActivator;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.wildfly.core.cli.command.activator.DefaultStandaloneOptionActivator;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocation;
import org.jboss.as.cli.embedded.EmbeddedProcessLaunch;
import org.jboss.as.cli.impl.CLIModelControllerClient;
import org.jboss.as.cli.impl.CommaSeparatedCompleter;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.CliCommandContext;
import org.wildfly.core.cli.command.CliCommandInvocation;
import org.wildfly.core.cli.command.DMRCommand;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "reload", description = "", activator = ReloadActivator.class)
public class ReloadCommand implements Command<CliCommandInvocation>, DMRCommand {

    public static class HostCompleter implements OptionCompleter<CliCompleterInvocation> {

        @Override
        public void complete(CliCompleterInvocation completerInvocation) {
            ReloadCommand rc = (ReloadCommand) completerInvocation.getCommand();

            CommaSeparatedCompleter comp = new CommaSeparatedCompleter() {
                @Override
                protected Collection<String> getAllCandidates(CommandContext ctx) {
                    return rc.hostReloadPermission.getAllowedOn(ctx);
                }
            };
            List<String> candidates = new ArrayList<>();
            int offset = comp.complete(completerInvocation.getCommandContext().getLegacyCommandContext(),
                    completerInvocation.getGivenCompleteValue(), 0, candidates);
            completerInvocation.addAllCompleterValues(candidates);
            completerInvocation.setOffset(offset);
        }
    }

    private final AtomicReference<EmbeddedProcessLaunch> embeddedServerRef;
    private final PerNodeOperationAccess hostReloadPermission;

    @Deprecated
    @Option(hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    @Option(name = "admin-only", required = false)
    private Boolean adminOnly;

    @Option(name = "restart-servers", required = false,
            activator = DefaultDomainOptionActivator.class)
    private Boolean restartServers;

    @Option(name = "use-current-server-config", required = false,
            activator = DefaultStandaloneOptionActivator.class)
    private Boolean useServerConfig;

    @Option(name = "use-current-domain-config", required = false,
            activator = DefaultDomainOptionActivator.class)
    private Boolean useDomainConfig;

    @Option(name = "use-current-host-config", required = false,
            activator = DefaultDomainOptionActivator.class)
    private Boolean useHostConfig;

    @Option(name = "host-config", required = false,
            activator = DefaultDomainOptionActivator.class)
    private String hostConfig;

    @Option(name = "domain-config", required = false,
            activator = DefaultDomainOptionActivator.class)
    private String domainConfig;

    @Option(name = "server-config", required = false,
            activator = DefaultStandaloneOptionActivator.class)
    private String serverConfig;

    @Option(name = "host", required = false,
            activator = DefaultDomainOptionActivator.class, completer = HostCompleter.class)
    private String host;

    private final AccessRequirement accessRequirement;

    public ReloadCommand(CommandContext ctx, AtomicReference<EmbeddedProcessLaunch> embeddedServerRef) {
        this.embeddedServerRef = embeddedServerRef;
        hostReloadPermission = new PerNodeOperationAccess(ctx, Util.HOST, null, Util.RELOAD);
        accessRequirement = AccessRequirementBuilder.Factory.create(ctx).any()
                .operation(Util.RELOAD)
                .requirement(hostReloadPermission)
                .build();
    }

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws CommandException, InterruptedException {

        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("reload"));
            return CommandResult.SUCCESS;
        }

        CommandContext ctx = commandInvocation.getCommandContext().getLegacyCommandContext();
        final ModelControllerClient client = ctx.getModelControllerClient();
        if (client == null) {
            throw new CommandException("Connection is not available.");
        }

        if (embeddedServerRef != null && embeddedServerRef.get() != null) {
            doHandleEmbedded(ctx, client);
            return CommandResult.SUCCESS;
        }

        if (!(client instanceof CLIModelControllerClient)) {
            throw new CommandException("Unsupported ModelControllerClient implementation " + client.getClass().getName());
        }
        final CLIModelControllerClient cliClient = (CLIModelControllerClient) client;

        final ModelNode op = buildRequest(ctx);
        try {
            final ModelNode response = cliClient.execute(op, true);
            if (!Util.isSuccess(response)) {
                throw new CommandException(Util.getFailureDescription(response));
            }
        } catch (IOException e) {
            // if it's not connected it's assumed the reload is in process
            if (cliClient.isConnected()) {
                StreamUtils.safeClose(cliClient);
                throw new CommandException("Failed to execute :reload", e);
            }
        }

        ensureServerRebootComplete(ctx, client);

        return CommandResult.SUCCESS;
    }

    private void doHandleEmbedded(CommandContext ctx, ModelControllerClient client) throws CommandException {

        assert (embeddedServerRef != null);
        assert (embeddedServerRef.get() != null);

        final ModelNode op = buildRequest(ctx);
        if (embeddedServerRef.get().isHostController()) {
            // WFCORE-938
            // for embedded-hc, we require --admin-only=true to be passed until the EHC supports --admin-only=false
            if (!adminOnly) {
                throw new CommandException("Reload into running mode is not supported, --admin-only=true must be specified.");
            }
        }

        try {
            final ModelNode response = client.execute(op);
            if (!Util.isSuccess(response)) {
                throw new CommandException(Util.getFailureDescription(response));
            }
        } catch (IOException e) {
            // This shouldn't be possible, as this is a local client
            StreamUtils.safeClose(client);
            throw new CommandException("Failed to execute :reload", e);
        }

        ensureServerRebootComplete(ctx, client);
    }

    private void ensureServerRebootComplete(CommandContext ctx, ModelControllerClient client) throws CommandException {
        final long start = System.currentTimeMillis();
        final long timeoutMillis = ctx.getConfig().getConnectionTimeout() + 1000;
        final ModelNode getStateOp = new ModelNode();
        if (ctx.isDomainMode()) {
            final ParsedCommandLine args = ctx.getParsedCommandLine();
            getStateOp.get(Util.ADDRESS).add(Util.HOST, host);
        }

        getStateOp.get(ClientConstants.OP).set(ClientConstants.READ_ATTRIBUTE_OPERATION);

        // this is left for compatibility with older hosts, it could use runtime-configuration-state on newer hosts.
        if (ctx.isDomainMode()) {
            getStateOp.get(ClientConstants.NAME).set("host-state");
        } else {
            getStateOp.get(ClientConstants.NAME).set("server-state");
        }

        while (true) {
            String serverState = null;
            try {
                final ModelNode response = client.execute(getStateOp);
                if (Util.isSuccess(response)) {
                    serverState = response.get(ClientConstants.RESULT).asString();
                    if ("running".equals(serverState) || "restart-required".equals(serverState)) {
                        // we're reloaded and the server is started
                        break;
                    }
                }
            } catch (IOException | IllegalStateException e) {
                // ignore and try again
                // IllegalStateException is because the embedded server ModelControllerClient will
                // throw that when the server-state / host-state is "stopping"
            }

            if (System.currentTimeMillis() - start > timeoutMillis) {
                if (!"starting".equals(serverState)) {
                    ctx.disconnectController();
                    throw new CommandException("Failed to establish connection in " + (System.currentTimeMillis() - start)
                            + "ms");
                }
                // else we don't wait any longer for start to finish
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                ctx.disconnectController();
                throw new CommandException("Interrupted while pausing before reconnecting.", e);
            }
        }
    }

    @Override
    public ModelNode buildRequest(CliCommandContext context) throws CommandFormatException {
        try {
            return buildRequest(context.getLegacyCommandContext());
        } catch (CommandException ex) {
            throw new CommandFormatException(ex);
        }
    }

    private ModelNode buildRequest(CommandContext ctx) throws CommandException {
        final ParsedCommandLine args = ctx.getParsedCommandLine();

        final ModelNode op = new ModelNode();
        if (ctx.isDomainMode()) {
            if (useServerConfig != null && useServerConfig) {
                throw new CommandException("--use-current-server-config is not allowed in the domain mode.");
            }
            if (serverConfig != null) {
                throw new CommandException("--server-config is not allowed in the domain mode.");
            }

            if (host == null) {
                throw new CommandException("Missing required argument --host");
            }
            op.get(Util.ADDRESS).add(Util.HOST, host);

            setBooleanArgument(op, restartServers, "restart-servers");
            setBooleanArgument(op, useDomainConfig, "use-current-domain-config");
            setBooleanArgument(op, useHostConfig, "use-current-host-config");
            setStringValue(op, hostConfig, "host-config");
            setStringValue(op, domainConfig, "domain-config");
        } else {
            if (host != null) {
                throw new CommandException("--host is not allowed in the standalone mode.");
            }
            if (useDomainConfig != null && useDomainConfig) {
                throw new CommandException("--use-current-domain-config is not allowed in the standalone mode.");
            }
            if (useHostConfig != null && useHostConfig) {
                throw new CommandException("--use-current-host-config is not allowed in the standalone mode.");
            }
            if (restartServers != null && restartServers) {
                throw new CommandException("--restart-servers is not allowed in the standalone mode.");
            }
            if (hostConfig != null) {
                throw new CommandException("--host-config is not allowed in the standalone mode.");
            }
            if (domainConfig != null) {
                throw new CommandException("--domain-config is not allowed in the standalone mode.");
            }

            op.get(Util.ADDRESS).setEmptyList();
            setBooleanArgument(op, useServerConfig, "use-current-server-config");
            setStringValue(op, serverConfig, "server-config");

        }
        op.get(Util.OPERATION).set(Util.RELOAD);

        setBooleanArgument(op, adminOnly, "admin-only");
        return op;
    }

    protected void setBooleanArgument(final ModelNode op, Boolean value, String paramName) {
        if (value == null) {
            return;
        }
        op.get(paramName).set(value);
    }

    private void setStringValue(final ModelNode op, String value, String paramName) {
        if (value == null) {
            return;
        }
        op.get(paramName).set(value);
    }

    AccessRequirement getAccessRequirement() {
        return accessRequirement;
    }
}
