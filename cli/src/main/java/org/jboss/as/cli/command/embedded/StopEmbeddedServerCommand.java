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
package org.jboss.as.cli.command.embedded;

import java.util.concurrent.atomic.AtomicReference;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.embedded.EmbeddedProcessLaunch;
import org.wildfly.core.cli.command.CliCommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "stop", description = "", activator = StopEmbeddedServerActivator.class)
public class StopEmbeddedServerCommand implements Command<CliCommandInvocation> {

    @Option(name = "help", hasValue = false)
    private boolean help;

    private final AtomicReference<EmbeddedProcessLaunch> serverReference;

    public StopEmbeddedServerCommand(AtomicReference<EmbeddedProcessLaunch> serverReference) {
        this.serverReference = serverReference;
    }

    AtomicReference<EmbeddedProcessLaunch> getServerReference() {
        return serverReference;
    }

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        CommandContext ctx = commandInvocation.getCommandContext().getLegacyCommandContext();
        if (help) {
            commandInvocation.getShell().out().println(commandInvocation.getHelpInfo("embed stop"));
            return CommandResult.SUCCESS;
        }
        if (serverReference.get() != null) {
            ctx.disconnectController();
        }
        return CommandResult.SUCCESS;
    }

    static void cleanup(final AtomicReference<EmbeddedProcessLaunch> serverReference) {
        EmbeddedProcessLaunch serverLaunch = serverReference.get();
        if (serverLaunch != null) {
            try {
                serverLaunch.stop();
            } finally {
                try {
                    serverLaunch.getEnvironmentRestorer().restoreEnvironment();
                } finally {
                    serverReference.compareAndSet(serverLaunch, null);
                }
            }
        }
    }

}
