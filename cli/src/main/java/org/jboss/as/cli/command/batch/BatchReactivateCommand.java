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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.GroupCommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.aesh.provider.CliCompleterInvocation;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.batch.BatchedCommand;
import org.wildfly.core.cli.command.CliCommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
@GroupCommandDefinition(name = "re-activate", description = "")
public class BatchReactivateCommand implements Command<CliCommandInvocation> {

    @Option(name = "help", hasValue = false)
    private boolean help;

    @Arguments(completer = BatchNameCompleter.class)
    private List<String> name;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println("Aesh should have hooks for help!");
            return null;
        }
        return handle(commandInvocation, name);
    }

    static CommandResult handle(CliCommandInvocation commandInvocation, List<String> name) throws CommandException {
        CommandContext ctx = commandInvocation.getCommandContext().getLegacyCommandContext();
        BatchManager batchManager = ctx.getBatchManager();

        if (name == null || name.isEmpty()) {
            throw new CommandException("No batch name to re-activate");
        }
        String batchName = name.get(0);
        if (batchManager.isHeldback(batchName)) {
            boolean activated = batchManager.activateHeldbackBatch(batchName);
            if (activated) {
                final String msg = batchName == null ? "Re-activated batch"
                        : "Re-activated batch '" + batchName + "'";
                commandInvocation.getShell().out().println(msg);
                List<BatchedCommand> batch = batchManager.getActiveBatch().getCommands();
                if (!batch.isEmpty()) {
                    for (int i = 0; i < batch.size(); ++i) {
                        BatchedCommand cmd = batch.get(i);
                        commandInvocation.getShell().out().println("#"
                                + (i + 1) + ' ' + cmd.getCommand());
                    }
                }
            } else {
                // that's more like illegal state
                throw new CommandException("Failed to activate batch.");
            }
        } else {
            throw new CommandException("'" + name
                    + "' not found among the held back batches.");
        }
        return null;
    }

    private class BatchNameCompleter implements OptionCompleter<CliCompleterInvocation> {

        @Override
        public void complete(CliCompleterInvocation completerInvocation) {
            List<String> candidates = new ArrayList<>();
            int cursor = complete(completerInvocation.getCommandContext().getLegacyCommandContext(),
                    completerInvocation.getGivenCompleteValue(), candidates);
            completerInvocation.addAllCompleterValues(candidates);
            completerInvocation.setOffset(completerInvocation.getGivenCompleteValue().length() - cursor);
            completerInvocation.setAppendSpace(false);
        }

        private int complete(CommandContext ctx, String buffer, List<String> candidates) {
            BatchManager batchManager = ctx.getBatchManager();
            Set<String> names = batchManager.getHeldbackNames();
            if (names.isEmpty()) {
                return -1;
            }

            int nextCharIndex = 0;
            while (nextCharIndex < buffer.length()) {
                if (!Character.isWhitespace(buffer.charAt(nextCharIndex))) {
                    break;
                }
                ++nextCharIndex;
            }

            String chunk = buffer.substring(nextCharIndex).trim();
            for (String name : names) {
                if (name != null && name.startsWith(chunk)) {
                    candidates.add(name);
                }
            }
            Collections.sort(candidates);
            return nextCharIndex;
        }

    }

}
