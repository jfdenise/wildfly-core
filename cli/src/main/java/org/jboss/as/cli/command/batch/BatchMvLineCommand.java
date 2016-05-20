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

import java.io.IOException;
import java.util.List;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.GroupCommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.command.CliCommandInvocation;
import org.jboss.as.cli.aesh.activator.HiddenActivator;

/**
 *
 * @author jdenise@redhat.com
 */
@GroupCommandDefinition(name = "mv-line", description = "")
public class BatchMvLineCommand implements Command<CliCommandInvocation> {

    @Option(name = "help", hasValue = false)
    private boolean help;

    @Deprecated
    @Arguments(activator = HiddenActivator.class)
    // Can contain current foloowed by new.
    List<Integer> altValues;

    @Option()
    private Integer currentLine;

    @Option()
    private Integer newLine;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws IOException, InterruptedException {
        if (help) {
            commandInvocation.getCommandContext().printLine("Aesh should have hooks for help!");
            return null;
        }
        CommandContext ctx = commandInvocation.getCommandContext();
        BatchManager batchManager = ctx.getBatchManager();

        if (!batchManager.isBatchActive()) {
            throw new RuntimeException("No active batch.");
        }

        Batch batch = batchManager.getActiveBatch();
        final int batchSize = batch.size();
        if (batchSize == 0) {
            throw new RuntimeException("The batch is empty.");
        }
        Integer curr = currentLine == null ? null : currentLine;
        if (altValues != null && !altValues.isEmpty()) {
            curr = altValues.get(0);
        }

        Integer neww = newLine == null ? null : newLine;
        if (altValues != null && altValues.size() > 1) {
            neww = altValues.get(1);
        }

        if (neww == null || curr == null) {
            throw new RuntimeException("Expected two options.");
        }

        if (curr < 1 || curr > batchSize) {
            throw new RuntimeException(curr + " isn't in range [1.." + batchSize + "].");
        }

        if (neww < 1 || neww > batchSize) {
            throw new RuntimeException(neww + " isn't in range [1.." + batchSize + "].");
        }

        batch.move(curr - 1, neww - 1);

        return null;
    }

}
