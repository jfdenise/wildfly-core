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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.container.AeshCommandContainerBuilder;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.command.registry.MutableCommandRegistry;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandHandler;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.OperationCommand;

/**
 *
 * @author jdenise@redhat.com
 */
public class CliCommandRegistry implements CommandRegistry {

    private final MutableCommandRegistry reg = new MutableCommandRegistry();
    private final List<CliSpecialCommand> specials = new ArrayList<>();
    private final Map<String, CliSpecialCommand> legacyHandlers = new HashMap<>();
    private final CommandContext context;
    private final AeshCommandContainerBuilder containerBuilder = new AeshCommandContainerBuilder();

    public CliCommandRegistry(CommandContext context)
            throws CommandLineException {
        this.context = context;
    }

    public void addSpecialCommand(CliSpecialCommand special)
            throws CommandLineException {
        specials.add(special);
        addCommand(special.getCommand());
    }

    public void addCommand(CommandContainer container) {
        CliCommandContainer cliContainer
                = new CliCommandContainer(context, container);
        reg.addCommand(cliContainer);
    }

    public void addCommand(Command command) {
        addCommand(containerBuilder.create(command));
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
        reg.removeCommand(name);
    }

    public void registerLegacyHandler(CommandHandler handler, String[] names) throws CommandLineParserException {
        for (String n : names) {
            CliLegacyCommandBridge bridge;
            if (handler instanceof OperationCommand) {
                if (handler.isBatchMode(context)) {
                    bridge = new CliLegacyBatchCompliantCommandBridge(n,
                            context, (OperationCommand) handler);
                } else {
                    bridge = new CliLegacyDMRCommandBridge(n,
                            context, (OperationCommand) handler);
                }
            } else {
                bridge = new CliLegacyCommandBridge(n,
                        context);
            }
            CliSpecialCommand cmd = new CliSpecialCommandBuilder().name(n).context(context).
                    executor(bridge).create();
            addCommand(cmd.getCommand());
            legacyHandlers.put(n, cmd);
        }
    }

    public void removeLegacyHandler(String cmdName) {
        reg.removeCommand(cmdName);
        legacyHandlers.remove(cmdName);
    }

    public Command findCommand(String name, String line) throws CommandNotFoundException {
        // XXX JFDENISE, Aesh should offer this logic.
        CommandContainer c = getCommand(name, line);
        CommandLineParser p = c.getParser();
        String[] split = line.split(" ");
        if (split.length > 1) {
            String sub = split[1];
            CommandLineParser child = c.getParser().getChildParser(sub);
            if (child != null) {
                p = child;
            }
        }
        return p.getCommand();
    }
}
