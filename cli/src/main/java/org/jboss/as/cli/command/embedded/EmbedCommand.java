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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.jboss.aesh.cl.GroupCommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.GroupCommand;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.embedded.EmbeddedProcessLaunch;
import org.wildfly.core.cli.command.CliCommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
@GroupCommandDefinition(name = "embed", description = "", activator = EmbedCommandActivator.class)
public class EmbedCommand implements GroupCommand<CliCommandInvocation> {

    @Deprecated
    @Option(name = "help", hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    private final AtomicReference<EmbeddedProcessLaunch> serverReference;
    private final boolean modular;

    public EmbedCommand(AtomicReference<EmbeddedProcessLaunch> serverReference, boolean modular) {
        this.serverReference = serverReference;
        this.modular = modular;
    }

    AtomicReference<EmbeddedProcessLaunch> getServerReference() {
        return serverReference;
    }

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("embed"));
            return CommandResult.SUCCESS;
        }
        throw new CommandException("Missing target to embed");
    }

    @Override
    public List<Command> getCommands() {
        List<Command> commands = new ArrayList<>();
        commands.add(new EmbedServerCommand(serverReference, modular));
        commands.add(new StopEmbeddedServerCommand(serverReference));
        commands.add(new EmbedHostControllerCommand(serverReference, modular));
        return commands;
    }
}
