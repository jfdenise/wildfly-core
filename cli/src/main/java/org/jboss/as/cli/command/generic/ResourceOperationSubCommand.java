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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.cl.converter.Converter;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.cl.internal.ProcessedOptionBuilder;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.parser.OptionParserException;
import org.jboss.aesh.console.command.map.MapProcessedCommandBuilder;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.aesh.activator.ExpectedOptionsActivator;
import org.jboss.as.cli.aesh.completer.BooleanCompleter;
import org.jboss.as.cli.aesh.converter.DefaultValueConverter;
import org.jboss.as.cli.aesh.converter.ListConverter;
import org.jboss.as.cli.aesh.converter.NonObjectConverter;
import org.jboss.as.cli.aesh.converter.PropertiesConverter;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocation;
import org.jboss.as.cli.aesh.provider.CliConverterInvocation;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 *
 * @author jfdenise
 */
class ResourceOperationSubCommand extends AbstractOperationSubCommand {

    private final List<ProcessedOption> commonOptions;
    private final Map<String, OptionCompleter<CliCompleterInvocation>> customCompleters;
    private final Map<String, Converter<ModelNode, CliConverterInvocation>> customConverters;
    ResourceOperationSubCommand(String operationName,
            NodeType nodeType,
            String propertyId,
            List<ProcessedOption> commonOptions,
            Map<String, OptionCompleter<CliCompleterInvocation>> customCompleters,
            Map<String, Converter<ModelNode, CliConverterInvocation>> customConverters) {
        super(operationName, nodeType, propertyId);
        this.commonOptions = commonOptions;
        this.customCompleters = customCompleters;
        this.customConverters = customConverters;
    }

    @Override
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

    @Override
    public ProcessedCommand getProcessedCommand(final CommandContext commandContext)
            throws CommandLineParserException {
        return new MapProcessedCommandBuilder().
                name(getOperationName()).
                addOptions(commonOptions).
                command(this).
                optionProvider(new MapProcessedCommandBuilder.ProcessedOptionProvider() {
                    @Override
                    public List<ProcessedOption> getOptions() {
                        List<ProcessedOption> allOptions = new ArrayList<>();
                        try {
                            final ModelNode descr
                                    = org.jboss.as.cli.command.generic.Util.
                                    getOperationDescription(commandContext,
                                            getOperationName(),
                                            getNodeType());

                            if (descr != null && descr.has(org.jboss.as.cli.Util.REQUEST_PROPERTIES)) {
                                final List<Property> propList = descr.get(org.jboss.as.cli.Util.REQUEST_PROPERTIES).asPropertyList();
                                for (Property prop : propList) {
                                    final ModelNode propDescr = prop.getValue();
                                    OptionCompleter<CliCompleterInvocation> valueCompleter;
                                    Converter<ModelNode, CliConverterInvocation> valueConverter;
                                    valueConverter = customConverters.get(prop.getName());
                                    valueCompleter = customCompleters.get(prop.getName());
                                    if (valueConverter == null) {
                                        valueConverter = DefaultValueConverter.INSTANCE;
                                        if (propDescr.has(org.jboss.as.cli.Util.TYPE)) {
                                            final ModelType type = propDescr.get(org.jboss.as.cli.Util.TYPE).asType();
                                            if (ModelType.BOOLEAN == type) {
                                                if (valueCompleter == null) {
                                                    valueCompleter = BooleanCompleter.INSTANCE;
                                                }
                                            } else if (ModelType.STRING == type) {
                                                valueConverter = NonObjectConverter.INSTANCE;
                                            } else if (prop.getName().endsWith("properties")) { // TODO this is bad but can't rely on proper descriptions
                                                valueConverter = PropertiesConverter.INSTANCE;
                                            } else if (ModelType.LIST == type) {
                                                if (propDescr.hasDefined(org.jboss.as.cli.Util.VALUE_TYPE) && asType(propDescr.get(org.jboss.as.cli.Util.VALUE_TYPE)) == ModelType.PROPERTY) {
                                                    valueConverter = PropertiesConverter.INSTANCE;
                                                } else {
                                                    valueConverter = ListConverter.INSTANCE;
                                                }
                                            }
                                        }
                                    }
                                    if (valueCompleter == null) {
                                        allOptions.add(new ProcessedOptionBuilder().
                                                activator(new ExpectedOptionsActivator(getPropertyId())).
                                                name(prop.getName()).
                                                converter(valueConverter).
                                                type(ModelNode.class).
                                                create());
                                    } else {
                                        allOptions.add(new ProcessedOptionBuilder().
                                                activator(new ExpectedOptionsActivator(getPropertyId())).
                                                completer(valueCompleter).
                                                name(prop.getName()).
                                                converter(valueConverter).
                                                type(ModelNode.class).
                                                create());
                                    }
                                }
                            }
                        } catch (CommandLineException |
                                IllegalArgumentException |
                                OptionParserException ex) {
                            throw new RuntimeException(ex);
                        }
                        return allOptions;
                    }
                }).create();
    }

    private ModelType asType(ModelNode type) {
        if (type == null) {
            return null;
        }
        try {
            return type.asType();
        } catch (IllegalArgumentException e) {
            // the value type is a structure
            return null;
        }
    }
}
