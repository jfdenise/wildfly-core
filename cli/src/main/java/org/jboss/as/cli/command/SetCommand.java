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
import java.io.IOException;
import java.util.List;
import org.jboss.aesh.cl.Arguments;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;

/**
 * A Command to set a variable
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "set", description = "")
public class SetCommand implements Command<CliCommandInvocation> {

    @Arguments()
    private List<String> variables;

    private DefaultCallbackHandler line = new DefaultCallbackHandler(true);

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws IOException, InterruptedException {
        if (variables != null && variables.size() > 0) {
            try {
                set(commandInvocation);
            } catch (CommandLineException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            printVariableList(commandInvocation);
        }
        return null;
    }

    private void set(CliCommandInvocation commandInvocation)
            throws CommandLineException {

        CommandContext ctx = commandInvocation.getCommandContext();
        for (String arg : variables) {
            arg = ArgumentWithValue.resolveValue(arg);
            if (arg.charAt(0) == '$') {
                arg = arg.substring(1);
                if (arg.isEmpty()) {
                    throw new CommandFormatException("Variable name is missing after '$'");
                }
            }
            final int equals = arg.indexOf('=');
            if (equals < 1) {
                throw new CommandFormatException("'=' is missing for variable '" + arg + "'");
            }
            final String name = arg.substring(0, equals);
            if (name.isEmpty()) {
                throw new CommandFormatException("The name is missing in '" + arg + "'");
            }
            if (equals == arg.length() - 1) {
                ctx.setVariable(name, null);
            } else {
                String value = arg.substring(equals + 1);
                if (value.length() > 2 && value.charAt(0) == '`' && value.charAt(value.length() - 1) == '`') {
                    value = Util.getResult(ctx, value.substring(1, value.length() - 1));
                }
                ctx.setVariable(name, value);
            }
        }
    }

    private void printVariableList(CliCommandInvocation commandInvocation) {
        for (String var : commandInvocation.getCommandContext().getVariables()) {
            commandInvocation.getShell().out().
                    println(var + "="
                            + commandInvocation.getCommandContext().
                            getVariable(var));
        }
    }
}
