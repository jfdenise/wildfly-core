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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.jboss.aesh.cl.GroupCommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.command.CliCommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
@GroupCommandDefinition(name = "list", description = "")
public class BatchListCommand implements Command<CliCommandInvocation> {

    @Option(name = "help", hasValue = false)
    private boolean help;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws IOException, InterruptedException {
        if (help) {
            commandInvocation.getCommandContext().printLine("Aesh should have hooks for help!");
            return null;
        }

        return handle(commandInvocation);

    }

    public static CommandResult handle(CliCommandInvocation commandInvocation) {
        final Set<String> heldbackNames = commandInvocation.
                getCommandContext().getBatchManager().getHeldbackNames();
        if (!heldbackNames.isEmpty()) {
            List<String> names = new ArrayList<>(heldbackNames.size());
            for (String heldbackName : heldbackNames) {
                names.add(heldbackName == null ? "<unnamed>" : heldbackName);
            }
            Collections.sort(names);
            for (String heldbackName : names) {
                commandInvocation.getShell().out().println(heldbackName);
            }
        }
        return null;
    }

}
