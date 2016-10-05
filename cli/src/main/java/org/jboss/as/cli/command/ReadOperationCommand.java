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
import org.jboss.as.cli.Util;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.aesh.completer.HeadersCompleter;
import org.jboss.as.cli.aesh.completer.OperationsCompleter;
import org.jboss.as.cli.aesh.completer.PathOptionCompleter;
import org.jboss.as.cli.aesh.converter.HeadersConverter;
import org.jboss.as.cli.aesh.converter.OperationRequestAddressConverter;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.util.SimpleTable;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.wildfly.core.cli.command.CliCommandContext;
import org.wildfly.core.cli.command.CliCommandInvocation;
import org.wildfly.core.cli.command.DMRCommand;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "operation", description = "")
public class ReadOperationCommand implements Command<CliCommandInvocation>, DMRCommand {

    @Deprecated
    @Option(hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    @Option(converter = OperationRequestAddressConverter.class, completer = PathOptionCompleter.class)
    private OperationRequestAddress node;

    @Arguments(completer = OperationsCompleter.class, valueSeparator = ',')
    private List<String> name;

    @Option(converter = HeadersConverter.class, completer = HeadersCompleter.class)
    private ModelNode headers;

    // Required by ReadOperationsCompleter
    public OperationRequestAddress getNode() {
        return node;
    }

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("read operation"));
            return CommandResult.SUCCESS;
        }
        OperationRequestAddress address = node;
        if (address == null) {
            address = new DefaultOperationRequestAddress(commandInvocation.
                    getCommandContext().getLegacyCommandContext().getCurrentNodePath());
        }
        ModelNode request;
        try {
            request = buildRequest(address, commandInvocation.
                    getCommandContext().getLegacyCommandContext());
        } catch (CommandFormatException ex) {
            throw new CommandException(ex.getMessage(), ex);
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
        ModelNode request;
        if (name == null || name.isEmpty()) {
            request = Util.buildRequest(ctx, address, Util.READ_OPERATION_NAMES);
            if (ctx.getConfig().isAccessControl()) {
                request.get(Util.ACCESS_CONTROL).set(true);
            }
        } else {
            request = Util.buildRequest(ctx, address, Util.READ_OPERATION_DESCRIPTION);
            request.get(Util.NAME).set(name.get(0));
        }
        if (headers != null) {
            ModelNode opHeaders = request.get(Util.OPERATION_HEADERS);
            opHeaders.set(headers);
        }
        return request;
    }

    private void handleResponse(CliCommandInvocation context, ModelNode response, boolean composite) throws CommandException {
        if (!Util.isSuccess(response)) {
            throw new CommandException(Util.getFailureDescription(response));
        }
        if (!response.hasDefined(Util.RESULT)) {
            return;
        }

        boolean opDescr = name != null && !name.isEmpty();

        if (opDescr) {
            final ModelNode result = response.get(Util.RESULT);

            if (result.has(Util.DESCRIPTION)) {
                context.println("\n\t" + result.get(Util.DESCRIPTION).asString());
            } else {
                context.println("Operation description is not available.");
            }

            final StringBuilder buf = new StringBuilder();
            buf.append("\n\nPARAMETERS\n");
            if (result.has(Util.REQUEST_PROPERTIES)) {
                final List<Property> props = result.get(Util.REQUEST_PROPERTIES).asPropertyList();
                if (props.isEmpty()) {
                    buf.append("\n\tn/a\n");
                } else {
                    for (Property prop : props) {
                        buf.append('\n');
                        buf.append(prop.getName()).append("\n\n");

                        final List<Property> propProps = prop.getValue().asPropertyList();
                        final SimpleTable table = new SimpleTable(2);
                        for (Property propProp : propProps) {
                            if (propProp.getName().equals(Util.DESCRIPTION)) {
                                buf.append('\t').append(propProp.getValue().asString()).append("\n\n");
                            } else if (!propProp.getName().equals(Util.VALUE_TYPE)) {
                                // TODO not detailing the value-type here, it's readability/formatting issue
                                table.addLine(new String[]{'\t' + propProp.getName() + ':', propProp.getValue().asString()});
                            }
                        }
                        table.append(buf, false);
                        buf.append('\n');
                    }
                }
            } else {
                buf.append("\n\tn/a\n");
            }
            context.println(buf.toString());

            buf.setLength(0);
            buf.append("\nRESPONSE\n");

            if (result.has(Util.REPLY_PROPERTIES)) {
                final List<Property> props = result.get(Util.REPLY_PROPERTIES).asPropertyList();
                if (props.isEmpty()) {
                    buf.append("\n\tn/a\n");
                } else {
                    buf.append('\n');

                    final SimpleTable table = new SimpleTable(2);
                    StringBuilder vtBuf = null;
                    for (Property prop : props) {
                        ModelType modelType = prop.getValue().getType();
                        if (prop.getName().equals(Util.DESCRIPTION)) {
                            buf.append('\t').append(prop.getValue().asString()).append("\n\n");
                        } else if (prop.getName().equals(Util.VALUE_TYPE) && (prop.getValue().getType() == ModelType.OBJECT || prop.getValue().getType() == ModelType.LIST)) {
                            final List<Property> vtProps = prop.getValue().asPropertyList();
                            if (!vtProps.isEmpty()) {
                                vtBuf = new StringBuilder();
                                for (Property vtProp : vtProps) {
                                    vtBuf.append('\n').append(vtProp.getName()).append("\n\n");
                                    final List<Property> vtPropProps = vtProp.getValue().asPropertyList();
                                    final SimpleTable vtTable = new SimpleTable(2);
                                    for (Property vtPropProp : vtPropProps) {
                                        if (vtPropProp.getName().equals(Util.DESCRIPTION)) {
                                            vtBuf.append('\t').append(vtPropProp.getValue().asString()).append("\n\n");
                                        } else if (!vtPropProp.getName().equals(Util.VALUE_TYPE)) {
                                            // TODO not detailing the value-type here, it's readability/formatting issue
                                            vtTable.addLine(new String[]{'\t' + vtPropProp.getName() + ':', vtPropProp.getValue().asString()});
                                        }
                                    }
                                    vtTable.append(vtBuf, false);
                                    vtBuf.append('\n');
                                }
                            }
                        } else {
                            table.addLine(new String[]{'\t' + prop.getName() + ':', prop.getValue().asString()});
                        }
                    }
                    table.append(buf, false);
                    buf.append('\n');

                    if (vtBuf != null) {
                        buf.append(vtBuf);
                    }
                }
            } else {
                buf.append("\n\tn/a\n");
            }
            context.println(buf.toString());
        } else {
            context.printColumns(Util.getList(response));
        }
    }
}
