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
package org.jboss.as.cli.impl;

import java.util.ArrayList;
import java.util.List;
import org.aesh.readline.completion.CompleteOperation;
import org.aesh.readline.completion.Completion;
import org.jboss.as.cli.CommandCompleter;
import org.jboss.as.cli.CommandContext;

/**
 * The completion for operation and handlers. Can't be added to aesh-readline
 * directly because this one and aesh one can return some content (eg: list of
 * commands).
 *
 * @author jdenise@redhat.com
 */
public class CLICompletion implements Completion<CompleteOperation> {

    private final CommandCompleter completer;
    private final CommandContext context;

    CLICompletion(CommandContext context, CommandCompleter completer) {
        this.context = context;
        this.completer = completer;
    }

    public static boolean isOperation(String mainCommand) {
        mainCommand = mainCommand.trim();
        return mainCommand.startsWith(":") || mainCommand.startsWith(".") || mainCommand.startsWith("/");
    }

    @Override
    public void complete(CompleteOperation co) {
        if (isOperation(co.getBuffer())) {
            List<String> candidates = new ArrayList<>();
            int offset = completer.complete(context, co.getBuffer(), co.getCursor(), candidates);
            co.doAppendSeparator(false);
            co.addCompletionCandidates(candidates);
            co.setOffset(offset);
            return;
        }

        List<String> candidates = new ArrayList<>();
        int offset = completer.complete(context, co.getBuffer(), co.getCursor(), candidates);
        co.setOffset(offset);
        if (!candidates.isEmpty()) {
            co.addCompletionCandidates(candidates);
            String buffer = context.getArgumentsString() == null
                    ? co.getBuffer() : context.getArgumentsString() + co.getBuffer();
            if (co.getCompletionCandidates().size() == 1
                    && co.getCompletionCandidates().get(0).
                    getCharacters().startsWith(buffer)) {
                co.doAppendSeparator(true);
            } else {
                co.doAppendSeparator(false);
            }
        }
    }
}
