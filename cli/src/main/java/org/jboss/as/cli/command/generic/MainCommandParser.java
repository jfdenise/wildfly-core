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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.aesh.cl.activation.OptionActivator;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.cl.converter.Converter;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.cl.internal.ProcessedOptionBuilder;
import org.jboss.aesh.cl.parser.AeshCommandLineParser;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.parser.OptionParserException;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.map.MapCommand;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.aesh.activator.ExpectedOptionsActivator;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.command.generic.MainCommand.MainCommandProcessedCommand;
import org.jboss.as.cli.aesh.completer.ChildrenNameCompleter;
import org.jboss.as.cli.aesh.completer.HeadersCompleter;
import org.jboss.as.cli.aesh.completer.InstanceCompleter;
import org.jboss.as.cli.aesh.completer.OperationCompleter;
import org.jboss.as.cli.aesh.converter.HeadersConverter;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocation;
import org.jboss.as.cli.aesh.provider.CliConverterInvocation;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 *
 * A Parser that creates commands for write-attributes and resource operations.
 * This parser is the entry point for generic commands.
 */
public class MainCommandParser extends AeshCommandLineParser<MapCommand> {

    private final class HelpGeneratorParser extends AeshCommandLineParser<MapCommand> {

        private final AbstractOperationSubCommand cmd;

        public HelpGeneratorParser(ProcessedCommand<MapCommand> processedCommand, AbstractOperationSubCommand cmd) {
            super(processedCommand);
            this.cmd = cmd;
        }

        @Override
        public String printHelp() {
            try {
                return cmd.getHelpContent(commandContext);
            } catch (CommandLineException ex) {
                return "Exception retrieving help : " + ex.toString();
            }
        }

    }
    static final String[] EXCLUDED_OPERATIONS = {"read-attribute",
        "read-children-names",
        "read-children-resources",
        "read-children-types",
        "read-operation-description",
        "read-operation-names",
        "read-resource-description",
        "validate-address",
        "write-attribute",
        "undefine-attribute",
        "whoami"};
    private final NodeType nodeType;
    private final String propertyId;
    private final CommandContext commandContext;
    private final boolean removable;
    private final Map<String, OptionCompleter<CliCompleterInvocation>> customCompleters = new HashMap<>();
    private final Map<String, AbstractOperationSubCommand> customSubCommands = new HashMap<>();
    private final Map<String, Converter<ModelNode, CliConverterInvocation>> customConverters = new HashMap<>();

    public MainCommandParser(String name, NodeType nodeType, String propertyId,
            CommandContext commandContext, boolean removable) throws CommandLineParserException {
        super(new MainCommand(name, nodeType, propertyId == null ? "name" : propertyId, EXCLUDED_OPERATIONS).
                getProcessedCommand(commandContext));
        this.nodeType = nodeType;
        this.propertyId = propertyId == null ? "name" : propertyId;
        this.commandContext = commandContext;
        this.removable = removable;
        // START BACKWARD COMPATIBILITY
        // This is done to allow for backward compatibility, this code should be
        // removed at some point
        MainCommandProcessedCommand c = (MainCommandProcessedCommand) getProcessedCommand();
        c.getMainCommand().customCompleters = customCompleters;
        c.getMainCommand().customConverters = customConverters;
        // END BACKWARD COMPATIBILITY
    }

    public boolean isRemovable() {
        return removable;
    }

    public void addCustomCompleter(String name, OptionCompleter<CliCompleterInvocation> completer) {
        customCompleters.put(name, completer);
    }

    public void addCustomConverter(String name, Converter<ModelNode, CliConverterInvocation> converter) {
        customConverters.put(name, converter);
    }

    public void addCustomSubCommand(AbstractOperationSubCommand subCommand) {
        customSubCommands.put(subCommand.getOperationName(), subCommand);
    }

    @Override
    public List<CommandLineParser<? extends Command>> getChildParsers() {
        List<CommandLineParser<? extends Command>> lst = new ArrayList<>();
        try {
            OperationCompleter completer
                    = new OperationCompleter(nodeType, null, EXCLUDED_OPERATIONS);
            for (final String op : completer.getCandidates(commandContext, null)) {
                // Add an operation as a subcommand

                // Retrieve Description
                final ModelNode descr = Util.getOperationDescription(commandContext,
                        op,
                        nodeType);
                String description = "";
                if (descr.has(org.jboss.as.cli.Util.DESCRIPTION)) {
                    description = descr.get(org.jboss.as.cli.Util.DESCRIPTION).asString();
                }
                AbstractOperationSubCommand subCommand = customSubCommands.get(op);
                if (subCommand == null) {
                    subCommand = new ResourceOperationSubCommand(this, op, description,
                            nodeType, propertyId,
                            customCompleters, customConverters);
                }
                AeshCommandLineParser<?> parser
                        = new HelpGeneratorParser(subCommand.
                                getProcessedCommand(commandContext), subCommand);
                parser.setChild(true);
                lst.add(parser);
            }
            AbstractOperationSubCommand writeCommand = new WriteAttributesSubCommand(this, nodeType,
                    propertyId,
                    null, customCompleters, customConverters, false);
            // Then add write attribute...
            AeshCommandLineParser<?> parser
                    = new HelpGeneratorParser(writeCommand.
                            getProcessedCommand(commandContext), writeCommand);
            parser.setChild(true);
            lst.add(parser);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return lst;
    }

    List<ProcessedOption> getCommonOptions() throws OptionParserException, CommandFormatException {
        final List<ProcessedOption> options = new ArrayList<>();

        // instance identifier option
        OptionActivator instanceActivator = new OptionActivator() {
            @Override
            public boolean isActivated(ProcessedCommand processedCommand) {
                if ((nodeType.dependsOnProfile()
                        && commandContext.isDomainMode())
                        && processedCommand.findLongOption("profile") == null) {
                    return false;
                }
                return true;
            }

        };

        // We need to retrieve the description of the propertyId (for help).
        String desc = null;
        if (commandContext.getModelControllerClient() != null) {
            List<Property> props = org.jboss.as.cli.command.generic.Util.
                    getNodeProperties(commandContext, nodeType);

            for (Property p : props) {
                if (p.getName().equals(propertyId)) {
                    desc = p.getValue().get("type").asString() + ", "
                            + p.getValue().get("description").asString();
                    break;
                }
            }
        }
        options.add(new ProcessedOptionBuilder().name(propertyId).
                completer(new InstanceCompleter(nodeType)).
                type(String.class).
                description(desc).
                required(true).
                activator(instanceActivator).
                create());

        options.add(new ProcessedOptionBuilder().name("headers").
                completer(new HeadersCompleter()).
                description("OBJECT, The operation headers.").
                type(Object.class).
                converter(HeadersConverter.INSTANCE).
                activator(new ExpectedOptionsActivator(propertyId)).
                create());

        options.add(new ProcessedOptionBuilder().name("help").
                activator(HiddenActivator.class).
                type(String.class).
                hasValue(false).
                create());

        // profile option
        if (nodeType.dependsOnProfile() && commandContext.isDomainMode()) {
            ChildrenNameCompleter profileCompleter = new ChildrenNameCompleter(commandContext, null, nodeType);
            OptionActivator profileActivator = new OptionActivator() {
                @Override
                public boolean isActivated(ProcessedCommand processedCommand) {
                    return nodeType.dependsOnProfile() && commandContext.isDomainMode();
                }

            };
            options.add(new ProcessedOptionBuilder().completer(profileCompleter).
                    description("The name of the profile the target resource belongs to.").
                    activator(profileActivator).name("profile").type(String.class).create());
        }
        return options;
    }
}
