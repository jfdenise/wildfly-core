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
package org.jboss.as.cli.command.ifelse;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.CommandLineException;
import org.wildfly.core.cli.command.CliCommandInvocation;
import org.wildfly.core.cli.command.CommandRedirection;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "end-if", description = "", activator = EndIfActivator.class)
public class EndIfCommand implements Command<CliCommandInvocation> {

    @Option(hasValue = false)
    private boolean help;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("end-if"));
            return CommandResult.SUCCESS;
        }
        CommandRedirection redirection = commandInvocation.getCommandContext().getCommandRedirection();
        IfElseRedirection flow = null;
        if (redirection instanceof IfElseRedirection) {
            flow = (IfElseRedirection) redirection;
        } else {
            throw new CommandException("end-if is not available outside if-else control flow");
        }

        try {
            flow.run(commandInvocation.getCommandContext());
        } catch (CommandLineException ex) {
            throw new CommandException(ex);
        }
        return CommandResult.SUCCESS;
    }
}
