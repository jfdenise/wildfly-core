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
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.completer.CompleterInvocation;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.wildfly.core.cli.command.CliCommandContext.TIMEOUT_RESET_VALUE;
import org.wildfly.core.cli.command.CliCommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "reset", description = "")
public class CommandTimeoutReset implements Command<CliCommandInvocation> {

    @Deprecated
    @Option(hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    @Arguments(completer = TimeoutCompleter.class)
    private List<String> value;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("command-timeout set"));
            return CommandResult.SUCCESS;
        }
        if (value == null || value.isEmpty()) {
            throw new CommandException("No value to reset");
        }
        TIMEOUT_RESET_VALUE resetValue = Enum.valueOf(TIMEOUT_RESET_VALUE.class, value.get(0).toUpperCase());
        commandInvocation.getCommandContext().resetCommandTimeout(resetValue);
        return CommandResult.SUCCESS;
    }

    public static class TimeoutCompleter implements OptionCompleter {

        @Override
        public void complete(CompleterInvocation t) {
            String buffer = t.getGivenCompleteValue();
            List<String> candidates = new ArrayList<>();
            List<String> values = new ArrayList<>();
            for (TIMEOUT_RESET_VALUE tr : TIMEOUT_RESET_VALUE.values()) {
                values.add(tr.name().toLowerCase());
            }
            if (buffer == null || buffer.isEmpty()) {
                candidates.addAll(values);
            } else {
                for (String v : values) {
                    if (v.equals(buffer)) {
                        candidates.add(v + " ");
                        break;
                    } else if (v.startsWith(buffer)) {
                        candidates.add(v);
                    }
                }
            }
            Collections.sort(candidates);
            t.addAllCompleterValues(candidates);
        }
    }
}
