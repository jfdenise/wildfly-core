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
import java.io.IOException;
import java.util.List;
import org.jboss.aesh.cl.Arguments;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;

/**
 * A Command to echo variables
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "echo-dmr", description = "")
public class EchoDMRCommand implements Command<CliCommandInvocation> {

    // XXX JFDENISE, NEED A COMPLETER FOR COMMAND.
    @Arguments()
    private List<String> cmd;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws IOException, InterruptedException {
        if (cmd != null && cmd.size() > 0) {
            try {
                echoDMR(commandInvocation);
            } catch (CommandFormatException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            commandInvocation.getShell().out().println("Missing the command or operation to translate to DMR.");
        }
        return null;
    }

    private void echoDMR(CliCommandInvocation commandInvocation)
            throws CommandFormatException {
        StringBuilder builder = new StringBuilder();
        for (String s : cmd) {
            builder.append(s).append(" ");
        }
        CommandContext ctx = commandInvocation.getCommandContext();

        commandInvocation.getShell().out().println(ctx.buildRequest(builder.toString()).toString());
    }
}
