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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.jboss.aesh.cl.internal.OptionType;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.cl.internal.ProcessedOptionBuilder;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.parser.OptionParserException;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.map.MapCommand;
import org.jboss.aesh.console.command.map.MapProcessedCommandBuilder;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.aesh.activator.DefaultActivator;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.aesh.activator.ResolveExpressionActivator;
import org.jboss.as.cli.aesh.completer.PathOptionCompleter;
import org.jboss.as.cli.aesh.converter.OperationRequestAddressConverter;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.cli.util.SimpleTable;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.wildfly.core.cli.command.CliCommandContext;
import org.wildfly.core.cli.command.CliCommandInvocation;
import org.wildfly.core.cli.command.DMRCommand;

/**
 * This command can't be a static command. It exposes some options that are
 * computed on the fly.
 *
 * @author jdenise@redhat.com
 */
public class LsMapCommand extends MapCommand<CliCommandInvocation> implements DMRCommand {

    // XXX JFDENISE, we need to store values because each time
    // the options are re-built we need to transfer options.
    // Should be done at the Aesh level.
    private List<ProcessedOption> previousOptions;

    public static class LsActivator extends DefaultActivator {

        @Override
        public boolean isActivated(ProcessedCommand command) {
            return getCommandContext().getModelControllerClient() != null;
        }
    }

    private class DynamicOptionsProvider implements MapProcessedCommandBuilder.ProcessedOptionProvider {

        private final CommandContext ctx;

        DynamicOptionsProvider(CommandContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public List<ProcessedOption> getOptions() {
            if (ctx.getModelControllerClient() == null) {
                return Collections.emptyList();
            }
            OperationRequestAddress addr = (OperationRequestAddress) getValue(PATH_ARGUMENT_NAME);
            final OperationRequestAddress address = addr == null ? new DefaultOperationRequestAddress(ctx.getCurrentNodePath()) : addr;
            if (address.endsOnType()) {
                return Collections.emptyList();
            }
            final ModelNode req = new ModelNode();
            Map<String, ProcessedOption> options = Collections.emptyMap();
            if (address.isEmpty()) {
                req.get(Util.ADDRESS).setEmptyList();
            } else {
                final ModelNode addrNode = req.get(Util.ADDRESS);
                for (OperationRequestAddress.Node node : address) {
                    addrNode.add(node.getType(), node.getName());
                }
            }
            req.get(Util.OPERATION).set(Util.READ_RESOURCE_DESCRIPTION);
            try {
                final ModelNode response = ctx.getModelControllerClient().execute(req);
                if (Util.isSuccess(response)) {
                    if (response.hasDefined(Util.RESULT)) {
                        final ModelNode result = response.get(Util.RESULT);
                        if (result.hasDefined(Util.ATTRIBUTES)) {
                            options = new TreeMap<>();
                            ModelNode attributes = result.get(Util.ATTRIBUTES);
                            for (String key : attributes.keys()) {
                                ModelNode attribute = attributes.get(key);
                                for (String k : attribute.keys()) {
                                    options.put(k, new ProcessedOptionBuilder().name(k).
                                            activator((ProcessedCommand processedCommand)
                                                    -> processedCommand.findOption("l") != null
                                                    && processedCommand.findOption("l").getValue() != null).
                                            type(Boolean.class).hasValue(false).create());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            List<ProcessedOption> opts = new ArrayList<>();
            for (ProcessedOption po : options.values()) {
                opts.add(po);
            }
            transferValues(opts);
            return opts;
        }
    }

    private static final String PATH_ARGUMENT_NAME = "lspathargument";

    public static class AttrDescPropertyActivator extends HiddenActivator {

        // to be discovered by help.
        public static final String[] WF_CLI_EXPECTED_OPTIONS = {"l"};
    }

    public ProcessedCommand getProcessedCommand(CommandContext ctx) throws OptionParserException, CommandLineParserException {
        ProcessedCommand p = new MapProcessedCommandBuilder().
                name("ls").
                optionProvider(new DynamicOptionsProvider(ctx)).
                addOption(new ProcessedOptionBuilder().name("l").
                        shortName('l').
                        hasValue(false).
                        type(Boolean.class).
                        create()).
                addOption(new ProcessedOptionBuilder().name("resolve-expressions").
                        hasValue(false).
                        type(Boolean.class).
                        activator(ResolveExpressionActivator.class).
                        create()
                ).
                // For help only
                addOption(new ProcessedOptionBuilder().name("<attribute-description-property>").
                        hasValue(false).
                        type(String.class).
                        activator(AttrDescPropertyActivator.class).
                        create()
                ).
                // Useless option but it has actually one.
                //                addOption(new ProcessedOptionBuilder().name("headers").
                //                        hasValue(true).
                //                        type(String.class).
                //                        completer(HeadersCompleter.class).
                //                        converter(HeadersConverter.class).create()).
                argument(new ProcessedOptionBuilder().completer(PathOptionCompleter.class).
                        name(PATH_ARGUMENT_NAME).
                        hasValue(true).
                        valueSeparator(',').
                        type(List.class).
                        optionType(OptionType.ARGUMENT).
                        converter(OperationRequestAddressConverter.class).create()).
                activator(new LsActivator()).
                command(this).create();
        return p;
    }

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        CommandContext ctx = commandInvocation.getCommandContext().getLegacyCommandContext();
        OperationRequestAddress address = (OperationRequestAddress) getValue(PATH_ARGUMENT_NAME);
        if (address == null) {
            address = new DefaultOperationRequestAddress(ctx.getCurrentNodePath());
        }
        if (contains("resolve")) {
            List<Boolean> resHolder = new ArrayList<>();
            try {
                ResolveExpressionActivator.retrieveDescription(address, ctx, (val) -> {
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
            request = buildRequest(address, ctx);
        } catch (CommandFormatException ex) {
            throw new CommandException(ex.getMessage(), ex);
        }
        ModelNode response = CommandUtil.execute(request, ctx);
        handleResponse(commandInvocation, response, Util.COMPOSITE.equals(request.get(Util.OPERATION).asString()));
        return CommandResult.SUCCESS;
    }

    @Override
    public ModelNode buildRequest(CliCommandContext context) throws CommandFormatException {
        OperationRequestAddress address = (OperationRequestAddress) getValue(PATH_ARGUMENT_NAME);
        if (address == null) {
            address = new DefaultOperationRequestAddress(context.getLegacyCommandContext().getCurrentNodePath());
        }
        return buildRequest(address,
                context.getLegacyCommandContext());
    }

    public ModelNode buildRequest(OperationRequestAddress address,
            CommandContext ctx) throws CommandFormatException {
        ModelNode request;
        if (address.endsOnType()) {
            final String type = address.getNodeType();
            address.toParentNode();
            final DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder(address);
            try {
                builder.setOperationName(Util.READ_CHILDREN_NAMES);
                builder.addProperty(Util.CHILD_TYPE, type);
                request = builder.buildRequest();
            } catch (OperationFormatException e) {
                throw new IllegalStateException("Failed to build operation", e);
            }
        } else {

            final ModelNode composite = new ModelNode();
            composite.get(Util.OPERATION).set(Util.COMPOSITE);
            composite.get(Util.ADDRESS).setEmptyList();
            final ModelNode steps = composite.get(Util.STEPS);

            {
                ModelNode typesRequest = new ModelNode();
                typesRequest.get(Util.OPERATION).set(Util.READ_CHILDREN_TYPES);
                typesRequest = Util.getAddressNode(ctx, address, typesRequest);
                steps.add(typesRequest);
            }

            {
                ModelNode resourceRequest = new ModelNode();
                resourceRequest.get(Util.OPERATION).set(Util.READ_RESOURCE);
                resourceRequest = Util.getAddressNode(ctx, address, resourceRequest);
                resourceRequest.get(Util.INCLUDE_RUNTIME).set(Util.TRUE);
                if (contains("resolve")) {
                    resourceRequest.get(Util.RESOLVE_EXPRESSIONS).set(Util.TRUE);
                }
                steps.add(resourceRequest);
            }

            if (contains("l")) {
                steps.add(Util.buildRequest(ctx, address, Util.READ_RESOURCE_DESCRIPTION));
            }
            request = composite;
        }
        ModelNode headers = (ModelNode) getValue("headers");
        if (headers != null) {
            ModelNode opHeaders = request.get(Util.OPERATION_HEADERS);
            opHeaders.set(headers);
        }
        return request;
    }

    private void handleResponse(CliCommandInvocation invocation, ModelNode outcome, boolean composite) throws CommandException {
        CommandContext ctx = invocation.getCommandContext().getLegacyCommandContext();
        if (!composite) {
            final List<ModelNode> nodeList = outcome.get(Util.RESULT).asList();
            if (!nodeList.isEmpty()) {
                final List<String> lst = new ArrayList<>(nodeList.size());
                for (ModelNode node : nodeList) {
                    lst.add(node.asString());
                }
                printList(invocation, lst);
            }
            return;
        }

        List<String> additionalProps = Collections.emptyList();
        if (contains("l")) {
            // Retrieve the set of attribute names
            additionalProps = new ArrayList<>();
            for (String k : getValues().keySet()) {
                if (k.equals("l") || k.equals(PATH_ARGUMENT_NAME) || k.equals("resolve-expressions")) {
                    continue;
                }
                additionalProps.add(k);
            }
        }
        Collections.sort(additionalProps);

        ModelNode resultNode = outcome.get(Util.RESULT);

        ModelNode attrDescriptions = null;
        ModelNode childDescriptions = null;
        if (resultNode.hasDefined(Util.STEP_3)) {
            final ModelNode stepOutcome = resultNode.get(Util.STEP_3);
            if (Util.isSuccess(stepOutcome)) {
                if (stepOutcome.hasDefined(Util.RESULT)) {
                    final ModelNode descrResult = stepOutcome.get(Util.RESULT);
                    if (descrResult.hasDefined(Util.ATTRIBUTES)) {
                        attrDescriptions = descrResult.get(Util.ATTRIBUTES);
                    }
                    if (descrResult.hasDefined(Util.CHILDREN)) {
                        childDescriptions = descrResult.get(Util.CHILDREN);
                    }
                } else {
                    throw new CommandException("Result is not available for read-resource-description request: " + outcome);
                }
            } else {
                throw new CommandException("Failed to get resource description: " + outcome);
            }
        }

        List<String> names = null;
        List<String> typeNames = null;
        if (resultNode.hasDefined(Util.STEP_1)) {
            ModelNode typesOutcome = resultNode.get(Util.STEP_1);
            if (Util.isSuccess(typesOutcome)) {
                if (typesOutcome.hasDefined(Util.RESULT)) {
                    final ModelNode resourceResult = typesOutcome.get(Util.RESULT);
                    final List<ModelNode> types = resourceResult.asList();
                    if (!types.isEmpty()) {
                        typeNames = new ArrayList<>();
                        for (ModelNode type : types) {
                            typeNames.add(type.asString());
                        }
                        if (childDescriptions == null && attrDescriptions == null) {
                            names = typeNames;
                        }
                    }
                } else {
                    throw new CommandException("Result is not available for read-children-types request: " + outcome);
                }
            } else {
                throw new CommandException("Failed to fetch type names: " + outcome);
            }
        } else {
            throw new CommandException("The result for children type names is not available: " + outcome);
        }

        if (resultNode.hasDefined(Util.STEP_2)) {
            ModelNode resourceOutcome = resultNode.get(Util.STEP_2);
            if (Util.isSuccess(resourceOutcome)) {
                if (resourceOutcome.hasDefined(Util.RESULT)) {
                    final ModelNode resourceResult = resourceOutcome.get(Util.RESULT);
                    final List<Property> props = resourceResult.asPropertyList();
                    if (!props.isEmpty()) {
                        final SimpleTable attrTable;
                        if (attrDescriptions == null) {
                            attrTable = null;
                        } else if (!additionalProps.isEmpty()) {
                            String[] headers = new String[3 + additionalProps.size()];
                            headers[0] = "ATTRIBUTE";
                            headers[1] = "VALUE";
                            headers[2] = "TYPE";
                            int i = 3;
                            for (String additional : additionalProps) {
                                headers[i++] = additional.toUpperCase(Locale.ENGLISH);
                            }
                            attrTable = new SimpleTable(headers);
                        } else {
                            attrTable = new SimpleTable(new String[]{"ATTRIBUTE", "VALUE", "TYPE"});
                        }
                        SimpleTable childrenTable = childDescriptions == null ? null : new SimpleTable(new String[]{"CHILD", "MIN-OCCURS", "MAX-OCCURS"});
                        if (typeNames == null && attrTable == null && childrenTable == null) {
                            typeNames = new ArrayList<>();
                            names = typeNames;
                        }

                        for (Property prop : props) {
                            final StringBuilder buf = new StringBuilder();
                            if (typeNames == null || !typeNames.contains(prop.getName())) {
                                if (attrDescriptions == null) {
                                    buf.append(prop.getName());
                                    buf.append('=');
                                    buf.append(prop.getValue().asString());
                                    // TODO the value should be formatted nicer but the current
                                    // formatter uses new lines for complex value which doesn't work here
                                    // final ModelNode value = prop.getValue();
                                    // ModelNodeFormatter.Factory.forType(value.getType()).format(buf, 0, value);
                                    typeNames.add(buf.toString());
                                    buf.setLength(0);
                                } else {
                                    final String[] line = new String[attrTable.columnsTotal()];
                                    line[0] = prop.getName();
                                    line[1] = prop.getValue().asString();
                                    if (attrDescriptions.hasDefined(prop.getName())) {
                                        final ModelNode attrDescr = attrDescriptions.get(prop.getName());
                                        line[2] = getAsString(attrDescr, Util.TYPE);
                                        if (!additionalProps.isEmpty()) {
                                            int i = 3;
                                            for (String additional : additionalProps) {
                                                line[i++] = getAsString(attrDescr, additional);
                                            }
                                        }
                                    } else {
                                        for (int i = 2; i < line.length; ++i) {
                                            line[i] = "n/a";
                                        }
                                    }
                                    attrTable.addLine(line);
                                }
                            } else if (childDescriptions != null) {
                                if (childDescriptions.hasDefined(prop.getName())) {
                                    final ModelNode childDescr = childDescriptions.get(prop.getName());
                                    final Integer maxOccurs = getAsInteger(childDescr, Util.MAX_OCCURS);
                                    childrenTable.addLine(new String[]{prop.getName(), getAsString(childDescr, Util.MIN_OCCURS), maxOccurs == null ? "n/a"
                                        : (maxOccurs == Integer.MAX_VALUE ? "unbounded" : maxOccurs.toString())});
                                } else {
                                    childrenTable.addLine(new String[]{prop.getName(), "n/a", "n/a"});
                                }
                            }
                        }

                        StringBuilder buf = null;
                        if (attrTable != null && !attrTable.isEmpty()) {
                            buf = new StringBuilder();
                            attrTable.append(buf, true);
                        }
                        if (childrenTable != null
                                && !childrenTable.isEmpty()) {
                            if (buf == null) {
                                buf = new StringBuilder();
                            } else {
                                buf.append("\n\n");
                            }
                            childrenTable.append(buf, true);
                        }
                        if (buf != null) {
                            ctx.printLine(buf.toString());
                        }
                    }
                } else {
                    throw new CommandException("Result is not available for read-resource request: " + outcome);
                }
            } else {
                throw new CommandException("Failed to fetch attributes: " + outcome);
            }
        } else {
            throw new CommandException("The result for attributes is not available: " + outcome);
        }

        if (names != null) {
            printList(invocation, names);
        }
    }

    private void transferValues(List<ProcessedOption> options) {
        if (previousOptions != null) {
            for (ProcessedOption popt : previousOptions) {
                if (popt.getValue() != null) {
                    for (ProcessedOption opt : options) {
                        if (opt.getName().equals(popt.getName())) {
                            opt.addValue(popt.getValue());
                        }
                    }
                }
            }
        }
        previousOptions = options;
    }

    private void printList(CliCommandInvocation ctx, List<String> lst) {
        if (contains("l")) {
            for (String item : lst) {
                ctx.println(item);
            }
        } else {
            ctx.printColumns(lst);
        }
    }

    private static String getAsString(final ModelNode attrDescr, String name) {
        if (attrDescr == null) {
            return "n/a";
        }
        return attrDescr.has(name) ? attrDescr.get(name).asString() : "n/a";
    }

    private static Integer getAsInteger(final ModelNode attrDescr, String name) {
        if (attrDescr == null) {
            return null;
        }
        return attrDescr.has(name) ? attrDescr.get(name).asInt() : null;
    }
}
