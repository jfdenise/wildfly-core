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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.cl.converter.Converter;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.cl.internal.ProcessedOptionBuilder;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.parser.OptionParserException;
import org.jboss.aesh.console.command.map.MapCommand;
import org.jboss.aesh.console.command.map.MapProcessedCommandBuilder;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.aesh.activator.ExpectedOptionsActivator;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.aesh.completer.BooleanCompleter;
import org.jboss.as.cli.aesh.converter.DefaultValueConverter;
import org.jboss.as.cli.aesh.converter.ListConverter;
import org.jboss.as.cli.aesh.converter.NonObjectConverter;
import org.jboss.as.cli.aesh.converter.PropertiesConverter;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocation;
import org.jboss.as.cli.aesh.provider.CliConverterInvocation;
import org.jboss.as.cli.command.generic.Util.AttributeDescription;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author jfdenise
 */
class WriteAttributesSubCommand extends AbstractOperationSubCommand {

    private final List<ProcessedOption> commonOptions;
    private final Map<String, OptionCompleter<CliCompleterInvocation>> customCompleters;
    private final Map<String, Converter<ModelNode, CliConverterInvocation>> customConverters;
    private final boolean hidden;
    WriteAttributesSubCommand(NodeType nodeType, String propertyId,
            List<ProcessedOption> commonOptions,
            Map<String, OptionCompleter<CliCompleterInvocation>> customCompleters,
            Map<String, Converter<ModelNode, CliConverterInvocation>> customConverters, boolean hidden) {
        super("write-attributes", nodeType, propertyId);
        this.commonOptions = commonOptions;
        this.customCompleters = customCompleters;
        this.customConverters = customConverters;
        this.hidden = hidden;
    }

    @Override
    public ModelNode buildRequest(CommandContext ctx) throws CommandFormatException {

        final ModelNode composite = new ModelNode();
        composite.get(org.jboss.as.cli.Util.OPERATION).set(org.jboss.as.cli.Util.COMPOSITE);
        composite.get(org.jboss.as.cli.Util.ADDRESS).setEmptyList();
        final ModelNode steps = composite.get(org.jboss.as.cli.Util.STEPS);

        final String profile;
        if (getNodeType().dependsOnProfile() && ctx.isDomainMode()) {
            profile = (String) getValue("profile");
            if (profile == null) {
                throw new OperationFormatException("--profile argument value is missing.");
            }
        } else {
            profile = null;
        }

        for (String argName : getValues().keySet()) {
            if (getNodeType().dependsOnProfile() && argName.equals("--profile")
                    || getPropertyId().equals(argName)) {
                continue;
            }

//            final ArgumentWithValue arg = (ArgumentWithValue) nodeProps.get(argName);
//            if (arg == null) {
//                throw new CommandFormatException("Unrecognized argument name '" + argName + "'");
//            }
            DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
            if (profile != null) {
                builder.addNode(org.jboss.as.cli.Util.PROFILE, profile);
            }

            for (OperationRequestAddress.Node node : getNodeType().getAddress()) {
                builder.addNode(node.getType(), node.getName());
            }
            builder.addNode(getNodeType().getType(), (String) getValue(getPropertyId()));
            builder.setOperationName(org.jboss.as.cli.Util.WRITE_ATTRIBUTE);
//            if (argName.charAt(1) == '-') {
//                propName = argName.substring(2);
//            } else {
//                propName = argName.substring(1);
//            }
            builder.addProperty(org.jboss.as.cli.Util.NAME, argName);

//                final String valueString = args.getPropertyValue(argName);
//                ModelNode nodeValue = arg.getValueConverter().fromString(ctx, valueString);
            final ModelNode nodeValue = (ModelNode) getValue(argName);
            builder.getModelNode().get(org.jboss.as.cli.Util.VALUE).set(nodeValue);

            steps.add(builder.buildRequest());
        }

        return composite;
    }

    @Override
    public ProcessedCommand<MapCommand> getProcessedCommand(final CommandContext commandContext) throws CommandLineParserException {
        return new MapProcessedCommandBuilder().
                name(getOperationName()).
                addOptions(commonOptions).
                command(this).
                optionProvider(new MapProcessedCommandBuilder.ProcessedOptionProvider() {
                    @Override
                    public List<ProcessedOption> getOptions() {
                        final List<ProcessedOption> allOptions = new ArrayList<>();
                        if (commandContext.getModelControllerClient() == null) {
                            return allOptions;
                        }
                        try {
                            Iterator<AttributeDescription> props
                                    = org.jboss.as.cli.command.generic.Util.getAttributeDescriptions(commandContext, getNodeType());
                            while (props.hasNext()) {
                                final AttributeDescription prop = props.next();
                                if (prop.getName().equals(getPropertyId())) {
                                    continue;
                                }
                                if (prop.isWriteAllowed()) {
                                    OptionCompleter<CliCompleterInvocation> valueCompleter = null;
                                    Converter<ModelNode, CliConverterInvocation> valueConverter = null;
                                    valueConverter = customConverters.get(prop.getName());
                                    valueCompleter = customCompleters.get(prop.getName());
                                    if (valueConverter == null) {
                                        valueConverter = DefaultValueConverter.INSTANCE;
                                        final ModelType propType = prop.getType();
                                        if (propType != null) {
                                            if (ModelType.BOOLEAN == propType) {
                                                if (valueCompleter == null) {
                                                    valueCompleter = BooleanCompleter.INSTANCE;
                                                }
                                            } else if (ModelType.STRING == propType) {
                                                valueConverter = NonObjectConverter.INSTANCE;
                                            } else if (prop.getName().endsWith("properties")) { // TODO this is bad but can't rely on proper descriptions
                                                valueConverter = PropertiesConverter.INSTANCE;
                                            } else if (ModelType.LIST == propType) {
                                                if (org.jboss.as.cli.command.generic.Util.asType(prop.getProperty(org.jboss.as.cli.Util.VALUE_TYPE)) == ModelType.PROPERTY) {
                                                    valueConverter = PropertiesConverter.INSTANCE;
                                                } else {
                                                    valueConverter = ListConverter.INSTANCE;
                                                }
                                            }
                                        }
                                    }
                                    if (valueCompleter == null) {
                                        allOptions.add(new ProcessedOptionBuilder().
                                                activator(new HiddenActivator(hidden, new ExpectedOptionsActivator(getPropertyId()))).
                                                name(prop.getName()).
                                                converter(valueConverter).
                                                type(ModelNode.class).
                                                create());
                                    } else {
                                        allOptions.add(new ProcessedOptionBuilder().
                                                activator(new HiddenActivator(hidden, new ExpectedOptionsActivator(getPropertyId()))).
                                                completer(valueCompleter).
                                                name(prop.getName()).
                                                converter(valueConverter).
                                                type(ModelNode.class).
                                                create());
                                    }
                                }
                            }
                        } catch (CommandLineException | OptionParserException ex) {
                            // XXX JFDENISE, REPLACE WITH LOG...
                            throw new RuntimeException(ex);
                        }
                        return allOptions;
                    }
                }).
                create();
    }
}
