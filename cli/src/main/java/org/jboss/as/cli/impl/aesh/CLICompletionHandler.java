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
package org.jboss.as.cli.impl.aesh;

import java.util.List;
import java.util.logging.Level;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.readline.completion.CompleteOperation;
import org.aesh.readline.completion.Completion;
import org.aesh.readline.completion.CompletionHandler;
import org.aesh.terminal.formatting.TerminalString;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.logmanager.Logger;

/**
 *
 * @author jdenise@redhat.com
 */
class CLICompletionHandler extends CompletionHandler<AeshCompleteOperation> implements Completion<AeshCompleteOperation>, CommandLineCompleter {

    private final Completion<CompleteOperation> delegate;
    private final AeshCommands aeshCommands;
    private static final Logger LOG = Logger.getLogger(CLICompletionHandler.class.getName());

    CLICompletionHandler(AeshCommands aeshCommands, Completion<CompleteOperation> delegate) {
        this.aeshCommands = aeshCommands;
        this.delegate = delegate == null ? new Completion<CompleteOperation>() {
            @Override
            public void complete(CompleteOperation completeOperation) {
            }
        } : delegate;
    }

    @Override
    public void complete(AeshCompleteOperation co) {

        if (LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "Completing {0}", co.getBuffer());
        }
        String buff = co.getBuffer().trim();
        String[] parts = buff.split(" ");
        // all command names from both repositories
        if (buff.isEmpty() || parts.length == 0) {
            // Aesh
            aeshCommands.complete(co);
            // Delegate
            delegate.complete(co);
        } else {
            // One of the two repositories.
            // Aesh first
            aeshCommands.complete(co);
            if (co.getCompletionCandidates().isEmpty()) {
                delegate.complete(co);
            }
        }

        if (LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "Completion candidates {0}",
                    co.getCompletionCandidates());
        }
    }

    @Override
    public AeshCompleteOperation createCompleteOperation(String buffer, int cursor) {
        return new AeshCompleteOperation(aeshCommands.getAeshContext(), buffer, cursor);
    }

    @Override
    public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {
        AeshCompleteOperation co = new AeshCompleteOperation(aeshCommands.getAeshContext(), buffer, cursor);
        complete(co);
        for (TerminalString ts : co.getCompletionCandidates()) {
            candidates.add(ts.getCharacters());
        }
        return co.getOffset();
    }

}
