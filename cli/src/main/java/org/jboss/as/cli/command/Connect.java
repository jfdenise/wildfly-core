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

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import java.io.IOException;
import java.util.List;
import org.jboss.as.cli.impl.ArgumentWithValue;

@CommandDefinition(name = "connect", description = "Connect to a JBoss Server")
public class Connect implements Command<CliCommandInvocation> {

    @Arguments()
    private List<String> controller;

    private Exception ex;
    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation) throws IOException, InterruptedException {
        Thread thr = new Thread(() -> {
            try {
                String url = controller == null || controller.isEmpty() ? null : controller.get(0);
                if (url != null) {
                    url = ArgumentWithValue.resolveValue(url);
                }
                commandInvocation.getCommandContext().connectController(url);
            } catch (Exception ex1) {
                commandInvocation.print(ex1.getMessage());
                Connect.this.ex = new RuntimeException(ex1);
                Thread.currentThread().interrupt();
            }
        });
        thr.start();
        try {
            // We will be possibly interrupted by console if Ctrl-C typed.
            thr.join();
        } catch (InterruptedException ex) {
            commandInvocation.getCommandContext().interruptConnect();
            thr.interrupt();
            commandInvocation.getCommandContext().exit();
        }
        if (ex != null) {
            throw new RuntimeException(ex);
        }
        return CommandResult.SUCCESS;
    }
}
