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
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocation;
import org.jboss.as.cli.impl.DefaultCompleter;
import org.wildfly.core.cli.command.CliCommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "unset", description = "")
public class UnsetVariableCommand implements Command<CliCommandInvocation> {

    public static class VariablesCompleter implements OptionCompleter<CliCompleterInvocation> {

        @Override
        public void complete(CliCompleterInvocation cliCompleterInvocation) {
            List<String> candidates = new ArrayList<>();
            int pos = 0;
            if (cliCompleterInvocation.getGivenCompleteValue() != null) {
                pos = cliCompleterInvocation.getGivenCompleteValue().length();
            }
            DefaultCompleter dc = new DefaultCompleter(new DefaultCompleter.CandidatesProvider() {
                @Override
                public Collection<String> getAllCandidates(CommandContext ctx) {
                    UnsetVariableCommand command = (UnsetVariableCommand) cliCompleterInvocation.getCommand();

                    if (command.vars == null || command.vars.isEmpty()) {
                        return ctx.getVariables();
                    }
                    if (ctx.getVariables().isEmpty()) {
                        return Collections.emptyList();
                    }
                    final ArrayList<String> all = new ArrayList<String>(ctx.getVariables());
                    all.removeAll(command.vars);
                    return all;
                }
            });
            int cursor = dc.complete(cliCompleterInvocation.getCommandContext().getLegacyCommandContext(),
                    cliCompleterInvocation.getGivenCompleteValue(),
                    pos, candidates);

            cliCompleterInvocation.addAllCompleterValues(candidates);
            cliCompleterInvocation.setOffset(pos - cursor);

        }

    }
    @Option(name = "help", hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    @Arguments(valueSeparator = ',', completer = VariablesCompleter.class)
    private List<String> vars;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("unset"));
            return CommandResult.SUCCESS;
        }
        if (vars == null || vars.isEmpty()) {
            throw new CommandException("Variable name is missing");
        }

        for (String name : vars) {
            if (name.charAt(0) == '$') {
                name = name.substring(1);
                if (name.isEmpty()) {
                    throw new CommandException("Variable name is missing after '$'");
                }
            }
            try {
                commandInvocation.getCommandContext().getLegacyCommandContext().setVariable(name, null);
            } catch (CommandLineException ex) {
                throw new CommandException(ex);
            }
        }
        return CommandResult.SUCCESS;
    }
}
