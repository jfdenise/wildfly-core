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

import java.util.List;
import org.jboss.aesh.cl.GroupCommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.CommandHistory;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.wildfly.core.cli.command.CliCommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
@GroupCommandDefinition(name = "history", description = "",
        groupCommands = {ClearHistoryCommand.class, DisableHistoryCommand.class, EnableHistoryCommand.class})
public class HistoryCommand implements Command<CliCommandInvocation> {

    @Option(name = "help", hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    @Deprecated
    @Option(activator = HiddenActivator.class, hasValue = false)
    boolean disable;

    @Deprecated
    @Option(activator = HiddenActivator.class, hasValue = false)
    boolean enable;

    @Deprecated
    @Option(activator = HiddenActivator.class, hasValue = false)
    boolean clear;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("reload"));
            return CommandResult.SUCCESS;
        }
        if (!enable && !disable && !clear) {
            printHistory(commandInvocation);
        }
        if (disable) {
            new DisableHistoryCommand().execute(commandInvocation);
        }
        if (enable) {
            new EnableHistoryCommand().execute(commandInvocation);
        }
        if (clear) {
            new ClearHistoryCommand().execute(commandInvocation);
        }
        return CommandResult.SUCCESS;
    }

    private static void printHistory(CliCommandInvocation ctx) {
        CommandHistory history = ctx.getCommandContext().getLegacyCommandContext().getHistory();
        List<String> list = history.asList();
        for (String cmd : list) {
            ctx.println(cmd);
        }
        ctx.println("(The history is currently " + (history.isUseHistory() ? "enabled)" : "disabled)"));
    }
}
