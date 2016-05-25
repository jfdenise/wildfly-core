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
import java.util.ArrayList;
import java.util.List;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocation;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.util.CLIExpressionResolver;

/**
 * A Command to echo variables
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "echo", description = "")
public class EchoCommand implements Command<CliCommandInvocation> {

    @Arguments(completer = VariableCompleter.class)
    private List<String> arguments;

    private final DefaultCallbackHandler line = new DefaultCallbackHandler(true);
    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws IOException, InterruptedException {
        if (arguments != null && arguments.size() > 0) {
            try {
                echo(commandInvocation);
            } catch (CommandFormatException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            printVariableList(commandInvocation);
        }
        return null;
    }

    private void echo(CliCommandInvocation commandInvocation)
            throws CommandFormatException {
        line.reset();
        StringBuilder builder = new StringBuilder();
        for (String s : arguments) {
            builder.append(s).append(" ");
        }
        CommandContext ctx = commandInvocation.getCommandContext();
        line.parse(ctx.getCurrentNodePath(),
                builder.toString(), true, ctx);
        String result = line.getSubstitutedLine().trim();
        if (ctx.isResolveParameterValues()) {
            result = CLIExpressionResolver.resolve(result);
        }
        // apply escape rules
        int i = result.indexOf('\\');
        if (i >= 0) {
            final StringBuilder buf = new StringBuilder(result.length() - 1);
            buf.append(result.substring(0, i));
            boolean escaped = true;
            while (++i < result.length()) {
                if (escaped) {
                    buf.append(result.charAt(i));
                    escaped = false;
                } else {
                    final char c = result.charAt(i);
                    if (c == '\\') {
                        escaped = true;
                    } else {
                        buf.append(c);
                    }
                }
            }
            result = buf.toString();
        }
        commandInvocation.getShell().out().println(result);
    }

    private void printVariableList(CliCommandInvocation commandInvocation) {
        for (String var : commandInvocation.getCommandContext().getVariables()) {
            commandInvocation.getShell().out().
                    println(var + "="
                            + commandInvocation.getCommandContext().
                            getVariable(var));
        }
    }

    private static class VariableCompleter implements OptionCompleter<CliCompleterInvocation> {

        @Override
        public void complete(CliCompleterInvocation completerInvocation) {
            List<String> candidates = new ArrayList<>();
            int pos = 0;
            if (completerInvocation.getGivenCompleteValue() != null) {
                pos = completerInvocation.getGivenCompleteValue().length();
            }


            CommandContext ctx = completerInvocation.getCommandContext();
            String val = completerInvocation.getGivenCompleteValue();
            if (val.startsWith("$")) {
                val = val.substring(1);
            }
            for (String var : ctx.getVariables()) {
                if (var.startsWith(val)) {
                    candidates.add("$" + var);
                }
            }
            completerInvocation.addAllCompleterValues(candidates);
        }
    }
}
