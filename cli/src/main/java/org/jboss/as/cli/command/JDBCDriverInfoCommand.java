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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.accesscontrol.AccessRequirement;
import org.jboss.as.cli.accesscontrol.AccessRequirementBuilder;
import org.jboss.as.cli.accesscontrol.HostServerOperationAccess;
import org.wildfly.core.cli.command.activator.ExpectedOptionsActivator;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocation;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.parsing.ParserUtil;
import org.jboss.as.cli.util.SimpleTable;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.OperationResponse;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.CliCommandContext;
import org.wildfly.core.cli.command.CliCommandInvocation;
import org.wildfly.core.cli.command.activator.CliOptionActivator;
import org.wildfly.core.cli.command.activator.DefaultDomainOptionActivator;
import org.wildfly.core.cli.command.activator.DomainOptionActivator;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "jdbc-driver-info", description = "",
        activator = JDBCDriverInfoActivator.class)
public class JDBCDriverInfoCommand implements Command<CliCommandInvocation> {

    public static class HostCompleter implements OptionCompleter<CliCompleterInvocation> {

        @Override
        public void complete(CliCompleterInvocation completerInvocation) {
            JDBCDriverInfoCommand hc = (JDBCDriverInfoCommand) completerInvocation.getCommand();

            DefaultCompleter comp = new DefaultCompleter(new DefaultCompleter.CandidatesProvider() {
                @Override
                public Collection<String> getAllCandidates(CommandContext ctx) {
                    final ModelControllerClient client = ctx.getModelControllerClient();
                    if (client == null) {
                        return Collections.emptyList();
                    }
                    return hc.hostServerPermission.getAllowedHosts(ctx);
                }
            });
            List<String> candidates = new ArrayList<>();
            int offset = comp.complete(completerInvocation.getCommandContext().getLegacyCommandContext(),
                    completerInvocation.getGivenCompleteValue(), 0, candidates);
            completerInvocation.addAllCompleterValues(candidates);
            completerInvocation.setOffset(offset);
        }
    }

    public static class ServerCompleter implements OptionCompleter<CliCompleterInvocation> {

        @Override
        public void complete(CliCompleterInvocation completerInvocation) {
            JDBCDriverInfoCommand hc = (JDBCDriverInfoCommand) completerInvocation.getCommand();

            DefaultCompleter comp = new DefaultCompleter(new DefaultCompleter.CandidatesProvider() {
                @Override
                public Collection<String> getAllCandidates(CommandContext ctx) {
                    final ModelControllerClient client = ctx.getModelControllerClient();
                    if (client == null) {
                        return Collections.emptyList();
                    }
                    return hc.hostServerPermission.getAllowedServers(ctx, hc.host);
                }
            });
            List<String> candidates = new ArrayList<>();
            int offset = comp.complete(completerInvocation.getCommandContext().getLegacyCommandContext(),
                    completerInvocation.getGivenCompleteValue(), 0, candidates);
            completerInvocation.addAllCompleterValues(candidates);
            completerInvocation.setOffset(offset);
        }
    }

    public static class ServerActivator extends ExpectedOptionsActivator
            implements CliOptionActivator, DomainOptionActivator {

        private final DefaultDomainOptionActivator da = new DefaultDomainOptionActivator();
        public ServerActivator() {
            super("host");
        }

        @Override
        public void setCommandContext(CliCommandContext commandContext) {
            da.setCommandContext(commandContext);
        }

        @Override
        public CliCommandContext getCommandContext() {
            return da.getCommandContext();
        }

        @Override
        public boolean isActivated(ProcessedCommand processedCommand) {
            if (!da.isActivated(processedCommand)) {
                return false;
            }
            return super.isActivated(processedCommand);
        }

    }

    public static class DriverCompleter implements OptionCompleter<CliCompleterInvocation> {

        @Override
        public void complete(CliCompleterInvocation completerInvocation) {
            DefaultCompleter comp = new DefaultCompleter(new DefaultCompleter.CandidatesProvider() {
                @Override
                public Collection<String> getAllCandidates(CommandContext ctx) {
                    try {
                        JDBCDriverInfoCommand rcmd = (JDBCDriverInfoCommand) completerInvocation.getCommand();
                        final ModelNode req = buildRequest(ctx, rcmd.host, rcmd.server);
                        final ModelNode response = ctx.getModelControllerClient().execute(req);
                        if (response.hasDefined(Util.RESULT)) {
                            final List<ModelNode> list = response.get(Util.RESULT).asList();
                            final List<String> names = new ArrayList<>(list.size());
                            for (ModelNode node : list) {
                                if (node.hasDefined(Util.DRIVER_NAME)) {
                                    names.add(node.get(Util.DRIVER_NAME).asString());
                                }
                            }
                            return names;
                        } else {
                            return Collections.emptyList();
                        }
                    } catch (Exception e) {
                        return Collections.emptyList();
                    }
                }
            });
            List<String> candidates = new ArrayList<>();
            int offset = comp.complete(completerInvocation.getCommandContext().getLegacyCommandContext(),
                    completerInvocation.getGivenCompleteValue(), 0, candidates);
            completerInvocation.addAllCompleterValues(candidates);
            completerInvocation.setOffset(offset);
        }
    }

    public static class DriverActivator extends ExpectedOptionsActivator {

        public DriverActivator() {
            super("host", "server");
        }

    }

    private final HostServerOperationAccess hostServerPermission;
    private final AccessRequirement accessRequirement;
    private OperationRequestAddress requiredAddress;
    private boolean dependsOnProfile;
    private String requiredType;

    @Deprecated
    @Option(hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    @Option(completer = HostCompleter.class, activator = DefaultDomainOptionActivator.class)
    private String host;

    @Option(completer = ServerCompleter.class, activator = ServerActivator.class)
    private String server;

    @Arguments(completer = DriverCompleter.class, activator = DriverActivator.class)
    private List<String> name;

    public JDBCDriverInfoCommand(CommandContext ctx) {
        addRequiredPath("/subsystem=datasources");
        hostServerPermission = new HostServerOperationAccess(ctx, Util.SUBSYSTEM + '=' + Util.DATASOURCES, Util.INSTALLED_DRIVERS_LIST);
        accessRequirement = AccessRequirementBuilder.Factory.create(ctx)
                .any()
                .operation(Util.SUBSYSTEM + '=' + Util.DATASOURCES, Util.INSTALLED_DRIVERS_LIST)
                .requirement(hostServerPermission)
                .build();
    }

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("jdbc-driver-info"));
            return CommandResult.SUCCESS;
        }
        CommandContext ctx = commandInvocation.getCommandContext().getLegacyCommandContext();
        final ModelNode request = buildRequest(ctx, host, server);

        final ModelControllerClient client = ctx.getModelControllerClient();
        final OperationResponse operationResponse;
        try {
            operationResponse = client.executeOperation(Operation.Factory.create(request), OperationMessageHandler.DISCARD);
        } catch (Exception e) {
            throw new CommandException("Failed to perform operation", e);
        }
        try {
            final ModelNode response = operationResponse.getResponseNode();
            if (!Util.isSuccess(response)) {
                throw new CommandException(Util.getFailureDescription(response));
            }
            handleResponse(commandInvocation, operationResponse.getResponseNode(),
                    Util.COMPOSITE.equals(request.get(Util.OPERATION).asString()));
            operationResponse.close();
        } catch (IOException ex) {
            throw new CommandException("Failed to perform operation", ex);
        }

        return CommandResult.SUCCESS;
    }

    protected void handleResponse(CliCommandInvocation commandInvocation,
            ModelNode response, boolean composite) throws CommandException {
        CommandContext ctx = commandInvocation.getCommandContext().getLegacyCommandContext();
        final ModelNode result = response.get(Util.RESULT);
        if (!result.isDefined()) {
            throw new CommandException("The operation result is not defined: " + result);
        }
        final List<ModelNode> list = result.asList();
        if (name == null || name.isEmpty()) {
            final SimpleTable table = new SimpleTable(new String[]{"NAME", "SOURCE"});
            for (ModelNode node : list) {
                final ModelNode driverName = node.get(Util.DRIVER_NAME);
                if (!driverName.isDefined()) {
                    throw new CommandException(Util.DRIVER_NAME + " is not available: " + node);
                }
                final String source;
                if (node.hasDefined(Util.DEPLOYMENT_NAME)) {
                    source = node.get(Util.DEPLOYMENT_NAME).asString();
                } else if (node.hasDefined(Util.DRIVER_MODULE_NAME)) {
                    final StringBuilder buf = new StringBuilder();
                    buf.append(node.get(Util.DRIVER_MODULE_NAME).asString());
                    if (node.hasDefined(Util.MODULE_SLOT)) {
                        buf.append('/').append(node.get(Util.MODULE_SLOT).asString());
                    }
                    source = buf.toString();
                } else {
                    source = "n/a";
                }
                table.addLine(new String[]{driverName.asString(), source});
            }
            ctx.printLine(table.toString(true));
        } else {
            final SimpleTable table = new SimpleTable(2);
            for (ModelNode node : list) {
                final ModelNode driverName = node.get(Util.DRIVER_NAME);
                if (!driverName.isDefined()) {
                    throw new CommandException(Util.DRIVER_NAME + " is not available: " + node);
                }
                if (name.get(0).equals(driverName.asString())) {
                    for (String propName : node.keys()) {
                        final ModelNode value = node.get(propName);
                        table.addLine(new String[]{propName, value.isDefined() ? value.asString() : "n/a"});
                    }
                }
            }
            ctx.printLine(table.toString(false));
        }
    }

    private static ModelNode buildRequest(CommandContext ctx, String host,
            String server) throws CommandException {
        final ModelNode req = new ModelNode();
        final ModelNode address = req.get(Util.ADDRESS);
        if (ctx.isDomainMode()) {
            if (host == null) {
                throw new CommandException("--host not set");
            }
            if (server == null) {
                throw new CommandException("--server not set");
            }
            address.add(Util.HOST, host);
            address.add(Util.SERVER, server);
        }
        address.add(Util.SUBSYSTEM, Util.DATASOURCES);
        req.get(Util.OPERATION).set(Util.INSTALLED_DRIVERS_LIST);
        return req;
    }

    private void addRequiredPath(String requiredPath) {
        if (requiredPath == null) {
            throw new IllegalArgumentException("Required path can't be null.");
        }
        DefaultOperationRequestAddress requiredAddress = new DefaultOperationRequestAddress();
        CommandLineParser.CallbackHandler handler = new DefaultCallbackHandler(requiredAddress);
        try {
            ParserUtil.parseOperationRequest(requiredPath, handler);
        } catch (CommandFormatException e) {
            throw new IllegalArgumentException("Failed to parse nodeType: " + e.getMessage());
        }
        addRequiredPath(requiredAddress);
    }

    /**
     * Adds a node path which is required to exist before the command can be
     * used.
     *
     * @param requiredPath node path which is required to exist before the
     * command can be used.
     */
    private void addRequiredPath(OperationRequestAddress requiredPath) {
        if (requiredPath == null) {
            throw new IllegalArgumentException("Required path can't be null.");
        }
        // there perhaps could be more but for now only one is allowed
        if (requiredAddress != null) {
            throw new IllegalStateException("Only one required address is allowed, atm.");
        }
        requiredAddress = requiredPath;

        final Iterator<OperationRequestAddress.Node> iterator = requiredAddress.iterator();
        if (iterator.hasNext()) {
            final String firstType = iterator.next().getType();
            dependsOnProfile = Util.SUBSYSTEM.equals(firstType) || Util.PROFILE.equals(firstType);
        }
        if (requiredAddress.endsOnType()) {
            requiredType = requiredAddress.toParentNode().getType();
        }
    }

    OperationRequestAddress getRequiredAddress() {
        return requiredAddress;
    }

    boolean isDependsOnProfile() {
        return dependsOnProfile;
    }

    AccessRequirement getAccessRequirement() {
        return accessRequirement;
    }

    String getRequiredType() {
        return requiredType;
    }
}
