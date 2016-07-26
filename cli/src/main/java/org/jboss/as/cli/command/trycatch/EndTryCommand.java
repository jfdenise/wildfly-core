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
package org.jboss.as.cli.command.trycatch;

import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.wildfly.core.cli.command.CliCommandInvocation;

/**
 *
 * @author jfdenise
 */
@CommandDefinition(name = "end-try", description = "", activator = EndTryActivator.class)
public class EndTryCommand implements Command<CliCommandInvocation> {

    @Option(hasValue = false)
    private boolean help;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("end-try"));
            return CommandResult.SUCCESS;
        }
        final TryCatchFinallyRedirection flow
                = TryCatchFinallyRedirection.get(commandInvocation.getCommandContext().getLegacyCommandContext());
        if (flow == null) {
            throw new CommandException("end-if may appear only at the end of try-catch-finally control flow");
        }
        if (flow.isInTry()) {
            throw new CommandException("end-if may appear only after catch or finally");
        }
        flow.run(commandInvocation.getCommandContext());
        return CommandResult.SUCCESS;
    }
}
