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
package org.jboss.as.cli.command.batch;

import java.util.List;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.GroupCommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.aesh.activator.BatchActivator;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocation;
import org.jboss.as.cli.batch.BatchedCommand;
import org.wildfly.core.cli.command.CliCommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
@GroupCommandDefinition(name = "edit-line", description = "", activator = BatchActivator.class)
public class BatchEditLineCommand implements Command<CliCommandInvocation> {

    @Deprecated
    @Option(name = "help", hasValue = false, activator = HiddenActivator.class)
    protected boolean help;

    // XXX JFDENISE AESH-401
    @Arguments(completer = CommandCompleter.class) // required = true
    List<String> cmd;

    @Option(name = "line-number", required = true)
    private Integer line_number;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("batch edit-line"));
            return CommandResult.SUCCESS;
        }
        CommandContext ctx = commandInvocation.
                getCommandContext().getLegacyCommandContext();
        BatchManager batchManager = ctx.getBatchManager();

        if (!batchManager.isBatchActive()) {
            throw new CommandException("No active batch.");
        }

        Batch batch = batchManager.getActiveBatch();
        final int batchSize = batch.size();
        if (batchSize == 0) {
            throw new CommandException("The batch is empty.");
        }

        Integer lineNumber = line_number;

        if (cmd != null && !cmd.isEmpty()) {
            try {
                lineNumber = Integer.parseInt(cmd.get(0));
            } catch (Exception ex) {
                // XXX OK will fail later.
            }
        }

        if (lineNumber == null) {
            throw new CommandException("Missing line number.");
        }

        if (cmd == null || cmd.isEmpty()) {
            throw new CommandException("Missing the new command line.");
        }

        if (lineNumber < 1 || lineNumber > batchSize) {
            throw new RuntimeException(lineNumber + " isn't in range [1.."
                    + batchSize + "].");
        }

        String editedLine = cmd.get(cmd.size() - 1);

        if (editedLine.charAt(0) == '"') {
            if (editedLine.length() > 1
                    && editedLine.charAt(editedLine.length() - 1) == '"') {
                editedLine = editedLine.substring(1, editedLine.length() - 1);
            }
        }

        try {
            BatchedCommand newCmd = ctx.toBatchedCommand(editedLine);
            batch.set(lineNumber - 1, newCmd);
            commandInvocation.getShell().out().println("#" + lineNumber
                    + " " + newCmd.getCommand());
        } catch (CommandFormatException ex) {
            throw new CommandException(ex);
        }
        return CommandResult.SUCCESS;
    }

    private static class CommandCompleter implements OptionCompleter<CliCompleterInvocation> {

        @Override
        public void complete(CliCompleterInvocation completerInvocation) {
            // XXX TODO, We need entry point in Aesh to complete a command.
        }

    }

}
