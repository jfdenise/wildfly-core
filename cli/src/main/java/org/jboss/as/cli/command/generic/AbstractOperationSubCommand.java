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
package org.jboss.as.cli.command.generic;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.map.MapCommand;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.ModelNodeFormatter;
import org.jboss.as.cli.command.generic.Util.AttributeDescription;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.util.SimpleTable;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.CliCommandInvocation;

/**
 *
 * @author jfdenise
 */
public abstract class AbstractOperationSubCommand extends MapCommand<CliCommandInvocation> {

    private final String operationName;
    private final NodeType nodeType;
    private final String propertyId;

    public AbstractOperationSubCommand(String operationName, NodeType nodeType, String propertyId) {
        this.operationName = operationName;
        this.nodeType = nodeType;
        this.propertyId = propertyId;
    }

    public abstract ProcessedCommand getProcessedCommand(final CommandContext commandContext)
            throws CommandLineParserException;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (contains("help")) {
            try {
                printHelp(commandInvocation.getCommandContext().
                        getLegacyCommandContext(), getValues());
            } catch (CommandLineException ex) {
                throw new RuntimeException(ex);
            }
            return null;
        }
        ModelNode request = null;
        try {
            request = buildRequest(commandInvocation.getCommandContext().getLegacyCommandContext());
        } catch (CommandFormatException ex) {
            throw new RuntimeException(ex);
        }
        ModelNode headersNode = (ModelNode) getValue("headers");
        if (headersNode != null) {
            ModelNode opHeaders = request.get(org.jboss.as.cli.Util.OPERATION_HEADERS);
            opHeaders.set(headersNode);
        }
        final ModelControllerClient client = commandInvocation.getCommandContext().getModelControllerClient();
        final ModelNode response;
        try {
            response = client.execute(request);
        } catch (Exception e) {

            throw new RuntimeException("Failed to perform operation", e);
        }
        if (!org.jboss.as.cli.Util.isSuccess(response)) {
            throw new RuntimeException(org.jboss.as.cli.Util.getFailureDescription(response));
        }

        if (!org.jboss.as.cli.Util.isSuccess(response)) {
            throw new RuntimeException(org.jboss.as.cli.Util.getFailureDescription(response));
        }
        StringBuilder buf = null;
        try {
            buf = formatResponse(commandInvocation.getCommandContext().getLegacyCommandContext(), response, false, null);
        } catch (CommandFormatException ex) {
            throw new RuntimeException(ex);
        }
        if (buf != null) {
            commandInvocation.println(buf.toString());
        }
        return null;
    }

    StringBuilder formatResponse(CommandContext ctx, ModelNode opResponse, boolean composite, StringBuilder buf) throws CommandFormatException {
        if (opResponse.hasDefined(org.jboss.as.cli.Util.RESULT)) {
            final ModelNode result = opResponse.get(org.jboss.as.cli.Util.RESULT);
            if (composite) {
                final Set<String> keys;
                try {
                    keys = result.keys();
                } catch (Exception e) {
                    throw new CommandFormatException("Failed to get step results from a composite operation response " + opResponse);
                }
                for (String key : keys) {
                    final ModelNode stepResponse = result.get(key);
                    buf = formatResponse(ctx, stepResponse, false, buf); // TODO nested composite ops aren't expected for now
                }
            } else {
                final ModelNodeFormatter formatter = ModelNodeFormatter.Factory.forType(result.getType());
                if (buf == null) {
                    buf = new StringBuilder();
                }
                formatter.format(buf, 0, result);
            }
        }
        if (opResponse.hasDefined(org.jboss.as.cli.Util.RESPONSE_HEADERS)) {
            final ModelNode headers = opResponse.get(org.jboss.as.cli.Util.RESPONSE_HEADERS);
            final Set<String> keys = headers.keys();
            final SimpleTable table = new SimpleTable(2);
            for (String key : keys) {
                table.addLine(new String[]{key + ':', headers.get(key).asString()});
            }
            if (buf == null) {
                buf = new StringBuilder();
            } else {
                buf.append(org.jboss.as.cli.Util.LINE_SEPARATOR);
            }
            table.append(buf, false);
        }
        return buf;
    }

    private void printHelp(CommandContext ctx,
            Map<String, Object> values) throws CommandLineException {
        final ModelNode result = org.jboss.as.cli.command.generic.Util.
                getOperationDescription(ctx, getOperationName(), getNodeType());
        if (!result.hasDefined(org.jboss.as.cli.Util.DESCRIPTION)) {
            throw new CommandLineException("Operation description is not available.");
        }

        ctx.printLine("\nDESCRIPTION:\n");
        org.jboss.as.cli.command.generic.Util.formatText(ctx, result.get(org.jboss.as.cli.Util.DESCRIPTION).asString(), 2);

        if (result.hasDefined(org.jboss.as.cli.Util.REQUEST_PROPERTIES)) {
            org.jboss.as.cli.command.generic.Util.printProperties(ctx, getPropertyId(),
                    org.jboss.as.cli.command.generic.Util.
                    getAttributeIterator(result.get(org.jboss.as.cli.Util.REQUEST_PROPERTIES).asPropertyList(), null));
        } else {
            org.jboss.as.cli.command.generic.Util.printProperties(ctx, getPropertyId(),
                    Collections.<AttributeDescription>emptyIterator());
        }
    }

    ModelNode buildRequest(CommandContext ctx) throws CommandFormatException {
        final ModelNode request = new ModelNode();
        final ModelNode address = request.get(org.jboss.as.cli.Util.ADDRESS);
        if (getNodeType().dependsOnProfile() && ctx.isDomainMode()) {
            final String profile = (String) getValue("profile");
            if (profile == null) {
                throw new OperationFormatException("Required argument --profile is missing.");
            }
            address.add(org.jboss.as.cli.Util.PROFILE, profile);
        }

        for (OperationRequestAddress.Node node : getNodeType().getAddress()) {
            address.add(node.getType(), node.getName());
        }
        address.add(getNodeType().getType(), (String) getValue(getPropertyId()));
        request.get(org.jboss.as.cli.Util.OPERATION).set(getOperationName());

        for (String argName : getValues().keySet()) {
            if (getNodeType().dependsOnProfile() && argName.equals("profile")) {
                continue;
            }
            if (argName.equals(getPropertyId())) {
                continue;
            }

            // XXX JFDENISE, SHOUDL BE DONE BY THE RUNTIME
//            final ArgumentWithValue arg = (ArgumentWithValue) this.args.get(argName);
//            if (arg == null) {
//                if (argName.equals(GenericTypeOperationHandler.this.name.getFullName())) {
//                    continue;
//                }
//                throw new CommandFormatException("Unrecognized argument " + argName + " for command '" + opName + "'.");
//            }
// XXX JFDENISE< WHAT IS THIS?
//            final String propName;
//            if (argName.charAt(1) == '-') {
//                propName = argName.substring(2);
//            } else {
//                propName = argName.substring(1);
//            }
//                final String valueString = parsedArgs.getPropertyValue(argName);
//                ModelNode nodeValue = arg.getValueConverter().fromString(ctx, valueString);
            final ModelNode nodeValue = (ModelNode) getValue(argName);
            request.get(argName).set(nodeValue);
        }
        return request;
    }

    /**
     * @return the operationName
     */
    public String getOperationName() {
        return operationName;
    }

    /**
     * @return the nodeType
     */
    public NodeType getNodeType() {
        return nodeType;
    }

    /**
     * @return the propertyId
     */
    public String getPropertyId() {
        return propertyId;
    }
}
