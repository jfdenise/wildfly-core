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

import java.util.ArrayList;
import java.util.List;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.ModelNodeFormatter;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.aesh.activator.ResolveExpressionActivator;
import org.jboss.as.cli.aesh.completer.AttributesCompleter;
import org.jboss.as.cli.aesh.completer.HeadersCompleter;
import org.jboss.as.cli.aesh.completer.PathOptionCompleter;
import org.jboss.as.cli.aesh.converter.HeadersConverter;
import org.jboss.as.cli.aesh.converter.OperationRequestAddressConverter;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.util.SimpleTable;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.CliCommandContext;
import org.wildfly.core.cli.command.CliCommandInvocation;
import org.wildfly.core.cli.command.DMRCommand;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "attribute", description = "")
public class ReadAttributeCommand implements Command<CliCommandInvocation>, DMRCommand {

    @Deprecated
    @Option(hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    @Option(converter = OperationRequestAddressConverter.class, completer = PathOptionCompleter.class)
    private OperationRequestAddress node;

    @Arguments(completer = AttributesCompleter.class, valueSeparator = ',')
    private List<String> name;

    @Option(name = "include-defaults", hasValue = false)
    private boolean includeDefaults;

    @Option(name = "verbose", hasValue = false, shortName = 'v')
    private boolean verbose;

    @Option(name = "resolve-expressions", hasValue = false, activator = ResolveExpressionActivator.class)
    private boolean resolveExpressions;

    @Option(converter = HeadersConverter.class, completer = HeadersCompleter.class)
    private ModelNode headers;

    // Required by ReadAttributesCompleter
    public OperationRequestAddress getNode() {
        return node;
    }

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("read attribute"));
            return CommandResult.SUCCESS;
        }
        OperationRequestAddress address = node;
        if (address == null) {
            address = new DefaultOperationRequestAddress(commandInvocation.
                    getCommandContext().getLegacyCommandContext().getCurrentNodePath());
        }
        if (resolveExpressions) {
            List<Boolean> resHolder = new ArrayList<>();
            try {
                ResolveExpressionActivator.retrieveDescription(address,
                        commandInvocation.
                        getCommandContext().getLegacyCommandContext(), (val) -> {
                            resHolder.add(val);
                        });
            } catch (CommandFormatException ex) {
                throw new CommandException(ex.getMessage(), ex);
            }

            if (!resHolder.get(0)) {
                throw new CommandException("Resolve Expression argument not available at this location.");
            }
        }
        ModelNode request;
        try {
            request = buildRequest(address, commandInvocation.
                    getCommandContext().getLegacyCommandContext());
        } catch (CommandFormatException ex) {
            throw new CommandException(ex.getMessage(), ex);
        }
        if (headers != null) {
            ModelNode opHeaders = request.get(Util.OPERATION_HEADERS);
            opHeaders.set(headers);
        }
        ModelNode response = CommandUtil.execute(request, commandInvocation.
                getCommandContext().getLegacyCommandContext());
        handleResponse(commandInvocation, response, Util.COMPOSITE.equals(request.get(Util.OPERATION).asString()));
        return CommandResult.SUCCESS;
    }

    @Override
    public ModelNode buildRequest(CliCommandContext context) throws CommandFormatException {
        try {
            return buildRequest(OperationRequestAddressConverter.
                    convert(null, context.getLegacyCommandContext()),
                    context.getLegacyCommandContext());
        } catch (OptionValidatorException ex) {
            throw new CommandFormatException(ex.getMessage(), ex);
        }
    }

    private ModelNode buildRequest(OperationRequestAddress address,
            CommandContext ctx) throws CommandFormatException {

        if (name == null || name.isEmpty()) {
            throw new CommandFormatException("Required argument attribute name is not specified.");
        }

        ModelNode req = Util.buildRequest(ctx, address, Util.READ_ATTRIBUTE);
        req.get(Util.NAME).set(name.get(0));
        if (resolveExpressions) {
            req.get(Util.RESOLVE_EXPRESSIONS).set(true);
        }

        if (includeDefaults) {
            req.get(Util.INCLUDE_DEFAULTS).set(includeDefaults);
        }

        if (verbose) {
            final ModelNode composite = new ModelNode();
            composite.get(Util.OPERATION).set(Util.COMPOSITE);
            composite.get(Util.ADDRESS).setEmptyList();
            final ModelNode steps = composite.get(Util.STEPS);
            steps.add(req);
            steps.add(Util.buildRequest(ctx, address, Util.READ_RESOURCE_DESCRIPTION));
            req = composite;
        }

        return req;
    }

    private void handleResponse(CliCommandInvocation context, ModelNode response, boolean composite) throws CommandException {
        if (!Util.isSuccess(response)) {
            throw new CommandException(Util.getFailureDescription(response));
        }
        if (!response.hasDefined(Util.RESULT)) {
            return;
        }

        final ModelNode result = response.get(Util.RESULT);

        if (composite) {
            final SimpleTable table = new SimpleTable(2);
            final StringBuilder valueBuf = new StringBuilder();
            if (result.hasDefined(Util.STEP_1)) {
                final ModelNode stepOutcome = result.get(Util.STEP_1);
                if (Util.isSuccess(stepOutcome)) {
                    if (stepOutcome.hasDefined(Util.RESULT)) {
                        final ModelNode valueResult = stepOutcome.get(Util.RESULT);
                        final ModelNodeFormatter formatter = ModelNodeFormatter.Factory.forType(valueResult.getType());
                        formatter.format(valueBuf, 0, valueResult);
                    } else {
                        valueBuf.append("n/a");
                    }
                    table.addLine(new String[]{"value", valueBuf.toString()});
                } else {
                    throw new CommandException("Failed to get resource description: " + response);
                }
            }

            if (result.hasDefined(Util.STEP_2)) {
                final ModelNode stepOutcome = result.get(Util.STEP_2);
                if (Util.isSuccess(stepOutcome)) {
                    if (stepOutcome.hasDefined(Util.RESULT)) {
                        final ModelNode descrResult = stepOutcome.get(Util.RESULT);
                        if (descrResult.hasDefined(Util.ATTRIBUTES)) {
                            ModelNode attributes = descrResult.get(Util.ATTRIBUTES);
                            if (name == null || name.isEmpty()) {
                                throw new CommandException("Attribute name is not available in handleResponse.");
                            } else if (attributes.hasDefined(name.get(0))) {
                                final ModelNode descr = attributes.get(name.get(0));
                                for (String prop : descr.keys()) {
                                    table.addLine(new String[]{prop, descr.get(prop).asString()});
                                }
                            } else {
                                throw new CommandException("Attribute description is not available.");
                            }
                        } else {
                            throw new CommandException("The resource doesn't provide attribute descriptions.");
                        }
                    } else {
                        throw new CommandException("Result is not available for read-resource-description request: " + response);
                    }
                } else {
                    throw new CommandException("Failed to get resource description: " + response);
                }
            }
            context.println(table.toString(true));
        } else {
            final ModelNodeFormatter formatter = ModelNodeFormatter.Factory.forType(result.getType());
            final StringBuilder buf = new StringBuilder();
            formatter.format(buf, 0, result);
            context.println(buf.toString());
        }
    }
}
