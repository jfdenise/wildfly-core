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

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import java.util.List;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandNotFoundException;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.command.legacy.InternalDMRCommand;
import org.jboss.as.cli.console.CliCommandRegistry;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.CliCommandInvocation;
import org.wildfly.core.cli.command.DMRCommand;

/**
 * A Command to echo variables. This is not activated, we are missing a proper
 * completer and options handling, eg: echo-dmr ls -l is badly handled.
 *
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "echo-dmr", description = "")
public class EchoDMRCommand implements Command<CliCommandInvocation> {

    // XXX JFDENISE, NEED A COMPLETER FOR COMMAND.
    @Arguments()
    private List<String> cmd;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        if (cmd != null && cmd.size() > 0) {
            try {
                echoDMR(commandInvocation);
            } catch (CommandFormatException | CommandNotFoundException |
                    CommandLineParserException | OptionValidatorException ex) {
                throw new CommandException(ex);
            }
        } else {
            commandInvocation.println("Missing the command or operation to translate to DMR.");
        }
        return CommandResult.SUCCESS;
    }

    private void echoDMR(CliCommandInvocation commandInvocation)
            throws CommandException, CommandNotFoundException,
            InterruptedException, CommandFormatException, CommandLineParserException, OptionValidatorException {
        StringBuilder builder = new StringBuilder();
        for (String s : cmd) {
            builder.append(s).append(" ");
        }
        commandInvocation.println(retrieveRequest(cmd.get(0), builder.toString(),
                commandInvocation).toString());
    }

    private ModelNode retrieveRequest(String opName, String originalInput,
            CliCommandInvocation commandInvocation)
            throws CommandNotFoundException, InterruptedException, CommandException, CommandFormatException, CommandLineParserException, OptionValidatorException {

        Command command = null;
        try {
            command = commandInvocation.getPopulatedCommand(originalInput);
        } catch (CommandException ex) {
            // Fall back for Operation and Bridges
            final CommandContainer<Command> container = ((CliCommandRegistry) commandInvocation.getCommandRegistry()).
                    getCommand(opName, originalInput);
            CommandLineParser<Command> cmdParser = container.getParser();
            if (!(cmdParser.getCommand() instanceof InternalDMRCommand)) {
                throw new CommandException("The command does not translate to an operation request.");
            }
            InternalDMRCommand c = (InternalDMRCommand) cmdParser.getCommand();
            return c.buildRequest(originalInput, commandInvocation.getCommandContext());
        }

        if (!(command instanceof DMRCommand)) {
            throw new CommandException("The command does not translate to an operation request.");
        }
        DMRCommand c = (DMRCommand) command;
        return c.buildRequest(commandInvocation.getCommandContext());
    }
}
