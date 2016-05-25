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
import org.jboss.aesh.cl.Option;
import org.jboss.as.cli.aesh.completer.PathOptionCompleter;
import org.jboss.as.cli.aesh.converter.OperationRequestAddressConverter;
import org.jboss.as.cli.operation.OperationRequestAddress;

/**
 * A Command to change the current node path.
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "cd", description = "")
public class CdCommand implements Command<CliCommandInvocation> {

    @Arguments(completer = PathOptionCompleter.class,
            converter = OperationRequestAddressConverter.class)
    // XXX jfdenise when we have ON/OFF for validation
    //validator = ChangeNodeValidator.class)
    private List<OperationRequestAddress> arguments;

    @Option(name = "no-validation")
    private boolean noValidation;

    @Option(hasValue = false)
    private boolean help;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws IOException, InterruptedException {
        if (help) {
            commandInvocation.getShell().out().println(commandInvocation.getHelpInfo("cd"));
            return null;
        }
        if (arguments != null && arguments.size() > 0) {
            commandInvocation.getCommandContext().setCurrentNodePath(arguments.get(0));
        }
        return null;
    }
}
