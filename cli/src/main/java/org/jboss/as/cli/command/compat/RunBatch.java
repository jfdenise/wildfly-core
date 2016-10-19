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
package org.jboss.as.cli.command.compat;

import java.io.File;
import java.util.ArrayList;
import org.jboss.aesh.cl.GroupCommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.aesh.converter.FileConverter;
import org.jboss.as.cli.aesh.converter.HeadersConverter;
import org.jboss.as.cli.command.batch.BatchRunCommand;
import org.jboss.as.cli.command.batch.BatchRunFileCommand;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.CliCommandContext;
import org.wildfly.core.cli.command.CliCommandInvocation;
import org.wildfly.core.cli.command.DMRCommand;

@Deprecated
@GroupCommandDefinition(name = "run-batch", description = "", activator = RunBatchActivator.class)
public class RunBatch implements Command<CliCommandInvocation>, DMRCommand {

    @Deprecated
    @Option(name = "help", hasValue = false, activator = HiddenActivator.class)
    private boolean help;

    @Deprecated
    @Option(name = "verbose", hasValue = false, required = false, shortName = 'v', activator = HiddenActivator.class)
    private boolean verbose;

    @Deprecated
    @Option(name = "headers", activator = HiddenActivator.class,
            converter = HeadersConverter.class, required = false)
    private ModelNode headers;

    @Deprecated
    @Option(name = "file", converter = FileConverter.class, activator = HiddenActivator.class)
    private File file;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (help) {
            try {
                Util.printLegacyHelp(commandInvocation.getCommandContext().getLegacyCommandContext(), "run-batch");
            } catch (CommandLineException ex) {
                throw new CommandException(ex);
            }
            return CommandResult.SUCCESS;
        }
        if (file != null) {
            BatchRunFileCommand command = new BatchRunFileCommand();
            command.headers = headers;
            command.verbose = verbose;
            command.arg = new ArrayList<>();
            command.arg.add(file);
            return command.execute(commandInvocation);
        }
        BatchRunCommand command = new BatchRunCommand();
        command.headers = headers;
        command.verbose = verbose;
        return command.execute(commandInvocation);
    }

    @Override
    public ModelNode buildRequest(CliCommandContext context) throws CommandFormatException {
        if (file != null) {
            BatchRunFileCommand command = new BatchRunFileCommand();
            command.headers = headers;
            command.verbose = verbose;
            command.arg = new ArrayList<>();
            command.arg.add(file);
            return command.buildRequest(context);
        }
        if (!context.getLegacyCommandContext().isBatchMode()) {
            throw new CommandFormatException("Without arguments the command "
                    + "can be executed only in the batch mode.");
        }
        BatchRunCommand command = new BatchRunCommand();
        command.headers = headers;
        command.verbose = verbose;
        return command.buildRequest(context);
    }

}
