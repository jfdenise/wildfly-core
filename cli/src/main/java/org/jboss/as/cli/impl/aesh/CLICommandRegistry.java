/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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
 *
 */
package org.jboss.as.cli.impl.aesh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.aesh.readline.completion.CompleteOperation;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.parser.CommandLineCompletionParser;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.populator.CommandPopulator;
import org.aesh.command.Command;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.impl.registry.MutableCommandRegistryImpl;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.parser.OptionParserException;
import org.aesh.command.registry.MutableCommandRegistry;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.console.AeshContext;
import org.aesh.parser.ParsedLineIterator;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.impl.aesh.commands.deprecated.HasLegacyCounterPart;
import org.jboss.as.cli.impl.OperationCommandContainer;
import org.jboss.logging.Logger;

/**
 *
 * @author jdenise@redhat.com
 */
public class CLICommandRegistry implements CommandRegistry {

    /**
     * Wraps an extension command registered as a sub command.
     */
    private class ExtSubCommandParser implements CommandLineParser<Command> {

        private final CommandLineParser<Command> parser;
        private final ProcessedCommand cmd;

        private ExtSubCommandParser(CommandLineParser<Command> parser,
                ProcessedCommand cmd) {
            this.parser = parser;
            this.cmd = cmd;
        }

        @Override
        public ProcessedCommand<Command> getProcessedCommand() {
            return cmd;
        }

        @Override
        public Command getCommand() {
            return parser.getCommand();
        }

        @Override
        public CommandLineCompletionParser getCompletionParser() {
            return parser.getCompletionParser();
        }

        @Override
        public List<String> getAllNames() {
            return parser.getAllNames();
        }

        @Override
        public CommandLineParser<Command> getChildParser(String name) {
            return parser.getChildParser(name);
        }

        @Override
        public void addChildParser(CommandLineParser<Command> childParser) {
            parser.addChildParser(childParser);
        }

        @Override
        public List<CommandLineParser<Command>> getAllChildParsers() {
            return parser.getAllChildParsers();
        }

        @Override
        public CommandPopulator getCommandPopulator() {
            return parser.getCommandPopulator();
        }

        @Override
        public String printHelp() {
            return parser.printHelp();
        }

        @Override
        public void parse(String line) {
            parser.parse(line);
        }

        @Override
        public void clear() {
            parser.clear();
        }

        @Override
        public boolean isGroupCommand() {
            return parser.isGroupCommand();
        }

        @Override
        public void setChild(boolean b) {
            parser.setChild(b);
        }

        @Override
        public void populateObject(String line, InvocationProviders invocationProviders, AeshContext aeshContext, Mode mode) throws CommandLineParserException, OptionValidatorException {
            parser.populateObject(line, invocationProviders, aeshContext, mode);
        }

        @Override
        public ProcessedOption lastParsedOption() {
            return parser.lastParsedOption();
        }

        @Override
        public void parse(String line, Mode mode) {
            parser.parse(line, mode);
        }

        @Override
        public void parse(ParsedLineIterator iterator, Mode mode) {
            parser.parse(iterator, mode);
        }

        @Override
        public CommandLineParser<Command> parsedCommand() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

    private static final Logger log = Logger.getLogger(CLICommandRegistry.class);
    private final MutableCommandRegistry reg = new MutableCommandRegistryImpl();
    private final AeshCommandContainerBuilder containerBuilder = new AeshCommandContainerBuilder();
    private final List<String> exposedCommands = new ArrayList<>();
    private final CommandContext ctx;
    private final OperationCommandContainer op;

    public CLICommandRegistry(CommandContext ctx, OperationCommandContainer op) {
        this.ctx = ctx;
        this.op = op;
    }

    private CommandContainer addCommandContainer(CommandContainer container) throws CommandLineException {
        Command c = container.getParser().getProcessedCommand().getCommand();
        if (c instanceof HasLegacyCounterPart) {
            // In legacy mode, only expose legacy.
            if (!ctx.isLegacyMode()) {
                exposedCommands.add(container.getParser().getProcessedCommand().name());
            }
        } else {
            exposedCommands.add(container.getParser().getProcessedCommand().name());
        }
        CLICommandContainer cliContainer;
        try {

            if (container instanceof CLICommandContainer) {
                cliContainer = (CLICommandContainer) container;
            } else {
                cliContainer = wrapContainer(container);
            }
        } catch (OptionParserException ex) {
            throw new CommandLineException(ex);
        }
        reg.addCommand(cliContainer);

        return cliContainer;
    }

    CLICommandContainer wrapContainer(CommandContainer commandContainer) throws OptionParserException {
        return new CLICommandContainer(commandContainer, ctx);
    }

    public CommandContainer addCommand(Command command) throws CommandLineException {
        CommandContainer container = containerBuilder.create(command);

        // Sub command handling
        String name = container.getParser().getProcessedCommand().name();
        int index = name.indexOf("@");
        if (index >= 0 && index != name.length() - 1) {
            String parentName = name.substring(index + 1);
            String childName = name.substring(0, index);
            if (!parentName.isEmpty()) {
                try {
                    CommandLineParser existingParent = findCommand(parentName,
                            parentName);
                    // Parent exists.
                    // If child already exists, we can't register it.
                    try {
                        findCommand(parentName, parentName + " " + childName);
                        throw new CommandLineException("Command "
                                + parentName + " " + childName
                                + " already exists. Can't register " + name);
                    } catch (CommandNotFoundException ex) {
                        // XXX OK
                    }

                    try {
                        // Add sub to existing command.
                        ProcessedCommand cmd = container.getParser().
                                getProcessedCommand();
                        ProcessedCommand sub = new ProcessedCommandBuilder().
                                activator(cmd.getActivator()).
                                addOptions(cmd.getOptions()).
                                aliases(cmd.getAliases()).
                                argument(cmd.getArgument()).
                                command(cmd.getCommand()).
                                description(cmd.description()).
                                name(childName). // child name
                                populator(cmd.getCommandPopulator()).
                                resultHandler(cmd.resultHandler()).
                                validator(cmd.validator()).
                                create();
                        existingParent.
                                addChildParser(new ExtSubCommandParser(container.getParser(),
                                        sub));
                    } catch (CommandLineParserException ex) {
                        throw new CommandLineException(ex);
                    }
                    return getCommand(parentName, parentName + " " + childName);
                } catch (CommandNotFoundException ex) {
                    log.warn("No parent "
                            + parentName + " command found. Registering command as "
                            + name);
                }
            }
        }

        return addCommand(container);
    }

    public CommandContainer addCommand(CommandContainer container) throws CommandLineException {
        return addCommandContainer(container);
    }

    @Override
    public CommandContainer<Command> getCommand(String name, String line)
            throws CommandNotFoundException {
        if (name.startsWith(":") || name.startsWith("/") || name.startsWith(".")) {
            return op;
        }
        return reg.getCommand(name, line);
    }

    @Override
    public void completeCommandName(CompleteOperation completeOperation) {
        reg.completeCommandName(completeOperation);
    }

    @Override
    public Set<String> getAllCommandNames() {
        return reg.getAllCommandNames();
    }

    public void removeCommand(String name) {
        if (exposedCommands.contains(name)) {
            exposedCommands.remove(name);
        }
        reg.removeCommand(name);
    }

    public CommandLineParser findCommand(String name, String line) throws CommandNotFoundException {
        CommandContainer c = getCommand(name, line);
        CommandLineParser p = c.getParser();
        String[] split = line == null ? new String[0] : line.split(" ");
        if (split.length > 1) {
            String sub = split[1];
            CommandLineParser child = c.getParser().getChildParser(sub);
            if (child != null) {
                // Must make it a CLI thing to properly print the help.
                if (c instanceof CLICommandContainer) {
                    CLICommandContainer cli = (CLICommandContainer) c;
                    child = cli.wrapParser(child);
                }
                p = child;
            } else {
                throw new CommandNotFoundException("Command not found " + line);
            }
        }
        return p;
    }

    @Override
    public CommandContainer getCommandByAlias(String alias) throws CommandNotFoundException {
        return reg.getCommandByAlias(alias);
    }

    @Override
    public List<CommandLineParser<?>> getChildCommandParsers(String parent) throws CommandNotFoundException {
        return reg.getChildCommandParsers(parent);
    }

    public List<String> getExposedCommands() {
        return Collections.unmodifiableList(exposedCommands);
    }

    @Override
    public void addRegistrationListener(CommandRegistrationListener listener) {
        reg.addRegistrationListener(listener);
    }

    @Override
    public void removeRegistrationListener(CommandRegistrationListener listener) {
        reg.removeRegistrationListener(listener);
    }

}