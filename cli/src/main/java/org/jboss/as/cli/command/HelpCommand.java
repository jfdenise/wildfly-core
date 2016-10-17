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
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.activation.CommandActivator;
import org.jboss.aesh.cl.activation.OptionActivator;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocation;
import org.jboss.as.cli.console.CliCommandContainer.CliCommandParser;
import org.jboss.as.cli.console.CliCommandRegistry;
import org.jboss.as.cli.console.CliSpecialCommand.CliSpecialParser;
import org.jboss.as.cli.operation.OperationRequestCompleter;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.wildfly.core.cli.command.CliCommandInvocation;
import org.wildfly.core.cli.command.activator.DefaultNotExpectedOptionsActivator;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "help", description = "", aliases = {"h"})
public class HelpCommand implements Command<CliCommandInvocation> {

    public static class CommandsActivator implements OptionActivator {

        @Override
        public boolean isActivated(ProcessedCommand pc) {
            return ((HelpCommand) pc.getCommand()).command == null
                    || ((HelpCommand) pc.getCommand()).command.isEmpty();
        }

    }

    public static class ArgActivator extends DefaultNotExpectedOptionsActivator {

        public ArgActivator() {
            super("commands");
        }
    }

    @Arguments(completer = HelpCompleter.class, activator = ArgActivator.class)//, valueSeparator = ',')
    private List<String> command;

    private final CliCommandRegistry registry;

    @Option(hasValue = false, activator = CommandsActivator.class)
    private boolean commands;

    public HelpCommand(CliCommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (command == null || command.isEmpty()) {
            if (commands) {
                List<String> lst = new ArrayList<>();
                for (String c : registry.getAllCommandNames()) {
                    CommandLineParser cmdParser;
                    try {
                        cmdParser = registry.findCommand(c, null);
                    } catch (CommandNotFoundException ex) {
                        continue;
                    }
                    CommandActivator activator = cmdParser.getProcessedCommand().getActivator();
                    if (activator.isActivated(cmdParser.getProcessedCommand())) {
                        lst.add(c);
                    }
                }
                lst.sort(null);
                commandInvocation.println("Commands available in the current context:");
                commandInvocation.printColumns(lst);
                commandInvocation.println("To read a description of a specific command execute 'help <command name>'.");
            } else {
                commandInvocation.println(commandInvocation.getHelpInfo("help"));
            }
            return CommandResult.SUCCESS;
        }
        if (commands) {
            throw new CommandException("commands action not usable with an argument");
        }
        if (command.size() > 2) {
            throw new CommandException("Command has more than one action");
        }
        String mainCommand = command.get(0);
        StringBuilder builder = new StringBuilder();
        for (String s : command) {
            builder.append(s).append(" ");
        }
        try {
            CommandLineParser parser = registry.findCommand(mainCommand, builder.toString());
            if (parser instanceof CliCommandParser) {
                CliCommandParser cparser = (CliCommandParser) parser;
                CommandLineParser<Command> wrapped = cparser.getWrappedParser();
                if (wrapped instanceof CliSpecialParser) {
                    CliSpecialParser sp = (CliSpecialParser) wrapped;
                    commandInvocation.println(sp.printHelp(builder.toString()));
                    return CommandResult.SUCCESS;
                }
            }
            commandInvocation.println(parser.printHelp());
        } catch (CommandNotFoundException ex) {
            throw new CommandException("Command not found " + builder.toString());
        }
        return CommandResult.SUCCESS;
    }

    public static class HelpCompleter implements OptionCompleter<CliCompleterInvocation> {

        private final DefaultCallbackHandler parsedCmd = new DefaultCallbackHandler(false);

        @Override
        public void complete(CliCompleterInvocation completerInvocation) {
            HelpCommand cmd = (HelpCommand) completerInvocation.getCommand();
            String mainCommand = null;
            if (cmd.command != null) {
                if (cmd.command.size() > 1) {
                    // Nothing to add.
                    return;
                }
                mainCommand = cmd.command.get(0);
            }

            // Special case for operations.
            if (completerInvocation.getGivenCompleteValue().startsWith("/")
                    || completerInvocation.getGivenCompleteValue().startsWith(":")
                    || completerInvocation.getGivenCompleteValue().startsWith(".")) {
                List<String> candidates = new ArrayList<>();
                String buff = completerInvocation.getGivenCompleteValue();
                parsedCmd.reset();
                try {
                    parsedCmd.parse(null,
                            buff, false,
                            completerInvocation.getCommandContext().getLegacyCommandContext());
                } catch (CommandFormatException ex) {
                    // XXX OK.
                    return;
                }
                int offset = OperationRequestCompleter.INSTANCE.
                        complete(completerInvocation.getCommandContext().getLegacyCommandContext(),
                                parsedCmd,
                                buff,
                                0, candidates);
                boolean contains = false;
                for (String c : candidates) {
                    int i = c.indexOf("(");
                    if (i > 0) {
                        contains = true;
                    }
                }
                if (contains) {
                    return;
                } else {
                    Collections.sort(candidates);
                    completerInvocation.setOffset(buff.length() - offset);
                    completerInvocation.addAllCompleterValues(candidates);
                    completerInvocation.setAppendSpace(false);
                }
                return;
            }

            List<String> allExposed = new ArrayList<>(cmd.registry.getExposedCommands());
            //allExposed.remove(":");
            //allExposed.add(":<operation>");
            List<String> candidates = new ArrayList<>();
            String buff = completerInvocation.getGivenCompleteValue();
            if (mainCommand == null) {
                // need to add all commands
                allExposed.add("/");
                if (buff == null || buff.isEmpty()) {
                    candidates.addAll(allExposed);
                } else {
                    for (String c : allExposed) {
                        if (c.startsWith(buff)) {
                            candidates.add(c);
                        }
                    }
                }
            } else {
                try {
                    CommandLineParser<? extends Command> p = cmd.registry.findCommand(mainCommand, null);
                    for (CommandLineParser child : p.getAllChildParsers()) {
                        if (child.getProcessedCommand().getName().startsWith(buff)) {
                            candidates.add(child.getProcessedCommand().getName());
                        }
                    }
                } catch (CommandNotFoundException ex) {
                    // XXX OK, no command.
                }
            }
            Collections.sort(candidates);
            completerInvocation.addAllCompleterValues(candidates);
        }

    }
}
