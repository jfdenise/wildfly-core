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
package org.jboss.as.cli.command.ifelse;

import java.util.List;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.command.trycatch.TryActivator;
import org.jboss.as.cli.handlers.CommandHandlerWithHelp;
import org.jboss.as.cli.handlers.ifelse.ConditionArgument;
import org.jboss.as.cli.handlers.ifelse.Operation;
import org.wildfly.core.cli.command.CliCommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "if", description = "", activator = TryActivator.class)
public class IfCommand implements Command<CliCommandInvocation> {

    @Option(hasValue = false)
    private boolean help;

    @Arguments(completer = IfCompleter.class, valueSeparator = '%')
    private List<String> args;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("if"));
            return CommandResult.SUCCESS;
        }
        if (args == null || args.isEmpty()) {
            throw new CommandException("The command is missing arguments.");
        }
        final BatchManager batchManager
                = commandInvocation.getCommandContext().
                getLegacyCommandContext().getBatchManager();
        if (batchManager.isBatchActive()) {
            throw new CommandException("if is not allowed while in batch mode.");
        }

        // Horrible hack to retrieve condition
        StringBuilder condition = new StringBuilder();
        int ofIndex = -1;
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if ("of".equals(arg)) {
                ofIndex = i;
                break;
            }
            condition.append(arg).append(" ");
        }
        String cond = condition.toString().trim();
        if (ofIndex < 0) {
            throw new CommandException("Failed to locate 'of' in " + args);
        }

        if (ofIndex == args.size() - 1) {
            throw new CommandException("The line is null or empty.");
        }

        // Horrible hack to retrieve request
        StringBuilder reqBuilder = new StringBuilder();
        for (int i = ofIndex + 1; i < args.size(); i++) {
            reqBuilder.append(args.get(i)).append(" ");
        }
        String request = reqBuilder.toString().trim();

        // Parse the condition, for now reuse the handler API.
        ConditionArgument argument = new ConditionArgument(new CommandHandlerWithHelp("if") {
            @Override
            protected void doHandle(CommandContext ctx) throws CommandLineException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });

        try {
            Operation op = argument.resolveOperation(cond);
            commandInvocation.getCommandContext().registerRedirection(new IfElseRedirection(
                    commandInvocation.getCommandContext(), op, request));
        } catch (CommandLineException ex) {
            ex.printStackTrace();
            throw new CommandException(ex);
        }
        return CommandResult.SUCCESS;
    }
}
