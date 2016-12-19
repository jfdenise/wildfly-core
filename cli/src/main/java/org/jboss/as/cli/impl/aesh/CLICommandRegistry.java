/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.cli.impl.aesh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.aesh.readline.completion.CompleteOperation;
import org.aesh.cl.CommandLine;
import org.aesh.cl.internal.ProcessedCommand;
import org.aesh.cl.internal.ProcessedCommandBuilder;
import org.aesh.cl.parser.CommandLineCompletionParser;
import org.aesh.cl.parser.CommandLineParser;
import org.aesh.cl.parser.CommandLineParserException;
import org.aesh.cl.parser.OptionParserException;
import org.aesh.cl.populator.CommandPopulator;
import org.aesh.console.command.Command;
import org.aesh.console.command.CommandNotFoundException;
import org.aesh.console.command.container.AeshCommandContainerBuilder;
import org.aesh.console.command.container.CommandContainer;
import org.aesh.console.command.registry.CommandRegistry;
import org.aesh.console.command.registry.MutableCommandRegistry;
import org.aesh.util.ParsedLine;
import org.jboss.as.cli.CommandLineException;
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
        public CommandLine<? extends Command> parse(ParsedLine line, boolean ignoreRequirements) {
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

    private static final Logger log = Logger.getLogger(CLICommandRegistry.class);
    private final MutableCommandRegistry reg = new MutableCommandRegistry();
    private final AeshCommandContainerBuilder containerBuilder = new AeshCommandContainerBuilder();
    private final List<String> exposedCommands = new ArrayList<>();

    private CommandContainer addCommandContainer(CommandContainer container) throws CommandLineException {
        if (container.getParser().getProcessedCommand().getActivator() != null) {
            if (!(container.getParser().getProcessedCommand().getActivator() instanceof CompatActivator)) {
                exposedCommands.add(container.getParser().getProcessedCommand().getName());
            }
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
        return new CLICommandContainer(commandContainer);
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

    @Override
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
}
