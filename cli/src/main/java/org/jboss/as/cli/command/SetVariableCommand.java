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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocation;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.parsing.CommandSubstitutionException;
import org.wildfly.core.cli.command.CliCommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "set", description = "")
public class SetVariableCommand implements Command<CliCommandInvocation> {

    public static class VariablesCompleter implements OptionCompleter<CliCompleterInvocation> {

        @Override
        public void complete(CliCompleterInvocation cliCompleterInvocation) {
            List<String> candidates = new ArrayList<>();
            int pos = 0;
            if (cliCompleterInvocation.getGivenCompleteValue() != null) {
                pos = cliCompleterInvocation.getGivenCompleteValue().length();
            }
            String buffer = cliCompleterInvocation.getGivenCompleteValue() == null
                    ? "" : cliCompleterInvocation.getGivenCompleteValue();
            int equals = buffer.indexOf('=');
            if (equals < 1 || equals + 1 == buffer.length()) {
                return;
            }
            // the problem is splitting values with whitespaces, e.g. for command substitution
            final String value = buffer.substring(equals + 1);
            // XXX JFDENISE, WRONG, WHAT ABOUT NEW COMMANDS?
            CommandContext ctx = cliCompleterInvocation.getCommandContext().getLegacyCommandContext();
            final int valueIndex = ctx.getDefaultCommandCompleter().complete(ctx, value, value.length(), candidates);
            if (valueIndex < 0) {
                return;
            }
            cliCompleterInvocation.addAllCompleterValues(candidates);
            cliCompleterInvocation.setAppendSpace(false);
            cliCompleterInvocation.setOffset(value.length() - valueIndex);

        }

    }

    @Deprecated
    @Option(hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    @Arguments(valueSeparator = ',', completer = VariablesCompleter.class)
    private List<String> vars;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("set"));
            return CommandResult.SUCCESS;
        }
        CommandContext ctx = commandInvocation.getCommandContext().getLegacyCommandContext();
        if (vars == null || vars.isEmpty()) {
            final Collection<String> defined = ctx.getVariables();
            if (defined.isEmpty()) {
                return CommandResult.SUCCESS;
            }
            final List<String> pairs = new ArrayList<String>(defined.size());
            for (String var : defined) {
                pairs.add(var + '=' + ctx.getVariable(var));
            }
            Collections.sort(pairs);
            for (String pair : pairs) {
                commandInvocation.println(pair);
            }
            return CommandResult.SUCCESS;
        }
        for (String arg : vars) {
            try {
                arg = ArgumentWithValue.resolveValue(arg);
            } catch (CommandFormatException ex) {
                throw new CommandException(ex);
            }
            if (arg.charAt(0) == '$') {
                arg = arg.substring(1);
                if (arg.isEmpty()) {
                    throw new CommandException("Variable name is missing after '$'");
                }
            }
            final int equals = arg.indexOf('=');
            if (equals < 1) {
                throw new CommandException("'=' is missing for variable '" + arg + "'");
            }
            final String name = arg.substring(0, equals);
            if (name.isEmpty()) {
                throw new CommandException("The name is missing in '" + arg + "'");
            }
            if (equals == arg.length() - 1) {
                try {
                    ctx.setVariable(name, null);
                } catch (CommandLineException ex) {
                    throw new CommandException(ex);
                }
            } else {
                String value = arg.substring(equals + 1);
                if (value.length() > 2 && value.charAt(0) == '`' && value.charAt(value.length() - 1) == '`') {
                    try {
                        value = Util.getResult(ctx, value.substring(1, value.length() - 1));
                    } catch (CommandSubstitutionException ex) {
                        throw new CommandException(ex);
                    }
                }
                try {
                    ctx.setVariable(name, value);
                } catch (CommandLineException ex) {
                    throw new CommandException(ex);
                }
            }
        }
        return CommandResult.SUCCESS;
    }
}
