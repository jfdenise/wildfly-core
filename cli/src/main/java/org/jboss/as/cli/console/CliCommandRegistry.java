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
package org.jboss.as.cli.console;

import org.jboss.as.cli.command.legacy.CliLegacyCommandBridge;
import org.jboss.as.cli.command.legacy.CliLegacyBatchCompliantCommandBridge;
import org.jboss.as.cli.command.legacy.CliLegacyDMRCommandBridge;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.aesh.cl.CommandLine;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.ProcessedCommandBuilder;
import org.jboss.aesh.cl.parser.CommandLineCompletionParser;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.parser.OptionParserException;
import org.jboss.aesh.cl.populator.CommandPopulator;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.container.AeshCommandContainerBuilder;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.command.registry.MutableCommandRegistry;
import org.jboss.aesh.parser.AeshLine;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandHandler;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.OperationCommand;
import org.jboss.as.cli.command.compat.CompatActivator;
import org.jboss.as.cli.command.generic.MainCommandParser;
import org.jboss.as.cli.impl.CliCommandContextImpl;
import org.wildfly.core.cli.command.InteractiveCommand;
import org.jboss.logging.Logger;

/**
 *
 * @author jdenise@redhat.com
 */
public class CliCommandRegistry implements CommandRegistry {

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
        public CommandLineParser<? extends Command> getChildParser(String name) {
            return parser.getChildParser(name);
        }

        @Override
        public void addChildParser(CommandLineParser<? extends Command> childParser) {
            parser.addChildParser(childParser);
        }

        @Override
        public List<CommandLineParser<? extends Command>> getAllChildParsers() {
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
        public CommandLine<? extends Command> parse(String line) {
            return parser.parse(line);
        }

        @Override
        public CommandLine<? extends Command> parse(String line, boolean ignoreRequirements) {
            return parser.parse(line, ignoreRequirements);
        }

        @Override
        public CommandLine<? extends Command> parse(AeshLine line, boolean ignoreRequirements) {
            return parser.parse(line, ignoreRequirements);
        }

        @Override
        public CommandLine<? extends Command> parse(List<String> lines, boolean ignoreRequirements) {
            return parser.parse(lines, ignoreRequirements);
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
    }

    private static final Logger log = Logger.getLogger(CliCommandRegistry.class);
    private final MutableCommandRegistry reg = new MutableCommandRegistry();
    private final List<CliSpecialCommand> specials = new ArrayList<>();
    private final Map<String, CliSpecialCommand> legacyHandlers = new HashMap<>();
    private final Map<String, CommandContainer> interactiveCommands = new HashMap<>();
    private final CommandContext context;
    private final CliCommandContextImpl commandContext;
    private final AeshCommandContainerBuilder containerBuilder = new AeshCommandContainerBuilder();
    private final AeshCliConsole console;
    private final List<String> exposedCommands = new ArrayList<>();

    CliCommandRegistry(AeshCliConsole console, CommandContext context,
            CliCommandContextImpl commandContext)
            throws CommandLineException {
        this.context = context;
        this.commandContext = commandContext;
        this.console = console;
    }

    public void addSpecialCommand(CliSpecialCommand special)
            throws CommandLineException {
        specials.add(special);
        addCommandContainer(special.getCommandContainer());
    }

    private CommandContainer addCommandContainer(CommandContainer container) throws CommandLineException {
        if (container.getParser().getProcessedCommand().getActivator() != null) {
            if (!(container.getParser().getProcessedCommand().getActivator() instanceof CompatActivator)) {
                exposedCommands.add(container.getParser().getProcessedCommand().getName());
            }
        }
        CliCommandContainer cliContainer;
        if (container.getParser().getCommand() instanceof InteractiveCommand) {
            interactiveCommands.put(container.getParser().
                    getProcessedCommand().getName(), container);
        }
        try {
            if (container instanceof CliCommandContainer) {
                cliContainer = (CliCommandContainer) container;
            } else {
                cliContainer = wrapContainer(container);
            }
        } catch (OptionParserException ex) {
            throw new CommandLineException(ex);
        }
        reg.addCommand(cliContainer);

        return cliContainer;
    }

    CliCommandContainer wrapContainer(CommandContainer commandContainer) throws OptionParserException {
        return new CliCommandContainer(console,
                context, commandContext,
                commandContainer, console.newResultHandler());
    }

    public boolean isInteractive(String command) {
        return interactiveCommands.containsKey(command);
    }

    public CommandContainer addCommand(Command command) throws CommandLineException {
        CommandContainer container = containerBuilder.create(command);

        // Sub command handling
        String name = container.getParser().getProcessedCommand().getName();
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
                                description(cmd.getDescription()).
                                name(childName). // child name
                                populator(cmd.getCommandPopulator()).
                                resultHandler(cmd.getResultHandler()).
                                validator(cmd.getValidator()).
                                create();
                        existingParent.
                                addChildParser(new ExtSubCommandParser(container.getParser(),
                                        sub));
                    } catch (CommandLineParserException ex) {
                        throw new CommandLineException(ex);
                    }
                    return getCommand(parentName, parentName + " " + childName);
                } catch (CommandNotFoundException ex) {
                    Logger.getLogger(CliCommandRegistry.class).warn("No parent "
                            + parentName + " command found. Registering command as "
                            + name);
                }
            }
        }

        return addCommand(container);
    }

    public CommandContainer addCommand(CommandContainer container) throws CommandLineException {
        // If a legacy handler exists, just remove it.
        String name = container.getParser().getProcessedCommand().getName();
        if (legacyHandlers.containsKey(name)) {
            removeLegacyHandler(name);
            log.info("Legacy handler " + name
                    + "remved, new one registered.");
        }
        return addCommandContainer(container);
    }

    @Override
    public CommandContainer<Command> getCommand(String name, String line)
            throws CommandNotFoundException {
        for (CliSpecialCommand special : specials) {
            CommandContainer<Command> command = special.commandFor(name);
            if (command != null) {
                return command;
            }
        }
        return reg.getCommand(name, line);
    }

    @Override
    public void completeCommandName(CompleteOperation completeOperation) {
        for (CliSpecialCommand special : specials) {
            if (special.complete(completeOperation)) {
                return;
            }
        }

        for (CliSpecialCommand bridge : legacyHandlers.values()) {
            if (bridge.complete(completeOperation)) {
                return;
            }
        }

        reg.completeCommandName(completeOperation);
    }

    @Override
    public Set<String> getAllCommandNames() {
        return reg.getAllCommandNames();
    }

    @Override
    public void removeCommand(String name) {
        if (exposedCommands.contains(name)) {
            exposedCommands.remove(name);
        }
        reg.removeCommand(name);
    }

    public void registerLegacyHandler(CommandHandler handler, String[] names)
            throws CommandLineParserException, CommandLineException {
        for (String name : names) {
            try {
                CommandLineParser c = findCommand(name, null);
                if (c != null) {
                    // Do not register legacy command when new one already present.
                    log.info("Not registering legacy handler " + name
                            + ". New one is already registered.");
                    return;
                }
            } catch (CommandNotFoundException ex) {
                // That is fine.
            }
        }
        for (String n : names) {
            CliLegacyCommandBridge bridge;
            if (handler instanceof OperationCommand) {
                if (handler.isBatchMode(context)) {
                    bridge = new CliLegacyBatchCompliantCommandBridge(n,
                            context, commandContext, (OperationCommand) handler, console);
                } else {
                    bridge = new CliLegacyDMRCommandBridge(n,
                            context, commandContext, (OperationCommand) handler, console);
                }
            } else {
                bridge = new CliLegacyCommandBridge(n,
                        commandContext, console);
            }
            CliSpecialCommand cmd = new CliSpecialCommandBuilder().name(n).context(context).
                    activator((c) -> handler.isAvailable(context)).
                    registry(this).
                    executor(bridge).resultHandler(console.newResultHandler()).create();
            addCommandContainer(cmd.getCommandContainer());
            legacyHandlers.put(n, cmd);
        }
    }

    public void removeLegacyHandler(String cmdName) {
        reg.removeCommand(cmdName);
        legacyHandlers.remove(cmdName);
    }

    public List<String> getExposedCommands() {
        return Collections.unmodifiableList(exposedCommands);
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
                if (c instanceof CliCommandContainer) {
                    CliCommandContainer cli = (CliCommandContainer) c;
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

    public Collection<String> getRemovableGenericCommandNames() {
        List<String> lst = new ArrayList<>();
        for (String name : getAllCommandNames()) {
            CommandContainer container;
            try {
                container = getCommand(name, null);
            } catch (CommandNotFoundException ex) {
                continue;
            }
            if (container != null && container instanceof CliCommandContainer) {
                CommandContainer cc = ((CliCommandContainer) container).getWrappedContainer();
                if (cc.getParser() instanceof MainCommandParser) {
                    if (((MainCommandParser) cc.getParser()).isRemovable()) {
                        lst.add(name);
                    }
                }
            }
        }
        return lst;
    }

    public void removeGenericCommand(String name) throws Exception {
        CommandContainer container = getCommand(name, null);
        if (container != null && container instanceof CliCommandContainer) {
            CommandLineParser p = ((CliCommandContainer) container).getWrappedContainer().getParser();
            if (p instanceof MainCommandParser) {
                if (((MainCommandParser) p).isRemovable()) {
                    reg.removeCommand(name);
                } else {
                    throw new Exception(name + " can't be removed");
                }
            } else {
                throw new Exception("Invalid command " + name);
            }
        }
    }

    @Override
    public List<CommandLineParser<?>> getChildCommandParsers(String parent) throws CommandNotFoundException {
        return reg.getChildCommandParsers(parent);
    }
}
