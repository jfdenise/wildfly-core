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
import java.util.ArrayList;
import java.util.List;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocation;
import org.jboss.as.cli.console.CliCommandRegistry;
import org.wildfly.core.cli.command.CliCommandInvocation;

/**
 * Remove added command
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "remove", description = "")
public class CommandRemove implements Command<CliCommandInvocation> {

    @Deprecated
    @Option(name = "command-name", activator = HiddenActivator.class,
            completer = DynamicCommandCompleter.class, description = "Deprecated, use the unamed argument")
    private String legacyCommandName;

    @Arguments(completer = DynamicCommandCompleter.class)
    private List<String> commandName;

    @Option(name = "help", hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation) throws CommandException, InterruptedException {

        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("command remove"));
            return null;
        }

        CliCommandRegistry reg = (CliCommandRegistry) commandInvocation.getCommandRegistry();
        // Should be handled at the aesh level.
        if ((commandName == null || commandName.isEmpty()) && legacyCommandName == null) {
            return null;
        }
        String cmd = (commandName == null || commandName.isEmpty()) ? legacyCommandName : commandName.get(0);
        try {
            reg.removeGenericCommand(cmd);
        } catch (Exception ex) {
            throw new CommandException(ex.getMessage(), ex);
        }
        return null;
    }

    public class DynamicCommandCompleter implements OptionCompleter<CliCompleterInvocation> {

        @Override
        public void complete(CliCompleterInvocation cliCompleterInvocation) {
            CliCommandRegistry reg
                    = (CliCommandRegistry) cliCompleterInvocation.getCommandRegistry();
            List<String> candidates = new ArrayList<>();
            int pos = 0;
            if (cliCompleterInvocation.getGivenCompleteValue() != null) {
                pos = cliCompleterInvocation.getGivenCompleteValue().length();
            }
            candidates.addAll(reg.getRemovableGenericCommandNames());
            cliCompleterInvocation.addAllCompleterValues(candidates);
            cliCompleterInvocation.setAppendSpace(false);
        }
    }

}
