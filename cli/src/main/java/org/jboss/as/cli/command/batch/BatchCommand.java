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

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.GroupCommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.command.CliCommandInvocation;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.aesh.completer.FileCompleter;
import org.jboss.as.cli.aesh.converter.FileConverter;

/**
 *
 * @author jdenise@redhat.com
 */
@GroupCommandDefinition(name = "batch", description = "", groupCommands
        = {BatchClearCommand.class,
            BatchDiscardCommand.class,
            BatchEditLineCommand.class,
            BatchHoldbackCommand.class,
            BatchListCommand.class,
            BatchLoadFileCommand.class,
            BatchMvLineCommand.class,
            BatchNewCommand.class,
            BatchReactivateCommand.class,
            BatchRmLineCommand.class,
            BatchRunCommand.class,
            BatchRunFileCommand.class
        })
public class BatchCommand implements Command<CliCommandInvocation> {

    @Option(name = "help", hasValue = false)
    private boolean help;

    @Deprecated
    @Option(name = "file", converter = FileConverter.class,
            completer = FileCompleter.class, activator = HiddenActivator.class)
    private File file;

    @Deprecated
    @Option(name = "l", hasValue = false)
    private boolean list;

    @Deprecated
    @Arguments(activator = HiddenActivator.class)
    List<String> name;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws IOException, InterruptedException {
        if (help) {
            commandInvocation.getShell().out().println(commandInvocation.getHelpInfo("batch"));
            return null;
        }
        if (commandInvocation.getCommandContext().getBatchManager().isBatchActive()) {
            throw new RuntimeException("Can't start a new batch while in batch mode.");
        }
        if (file != null) {
            return BatchLoadFileCommand.handle(commandInvocation, file);
        }
        if (list) {
            return BatchListCommand.handle(commandInvocation);
        }
        if (name != null && !name.isEmpty()) {
            return BatchReactivateCommand.handle(commandInvocation, name);
        }

        return BatchNewCommand.handle(commandInvocation);
    }
}
