/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cli.impl.aesh.commands.batch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;
import org.wildfly.core.cli.command.aesh.FileCompleter;
import org.wildfly.core.cli.command.aesh.FileConverter;
import org.wildfly.core.cli.command.aesh.activator.AbstractOptionActivator;

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
public class BatchCommand implements Command<CLICommandInvocation> {

    static final String CLEAR = "clear";
    static final String DISCARD = "discard";
    static final String EDIT_LINE = "edit-line";
    static final String HOLDBACK = "holdback";
    static final String LIST = "list";
    static final String LOAD_FILE = "load-file";
    static final String MOVE_LINE = "move-line";
    static final String NEW = "new";
    static final String REACTIVATE = "re-activate";
    static final String REMOVE_LINE = "remove-line";
    static final String RUN = "run";
    static final String RUN_FILE = "run-file";

    public class HideLegacyOptionActivator extends AbstractOptionActivator {

        @Override
        public boolean isActivated(ProcessedCommand processedCommand) {
            return getCommandContext().isLegacyMode();
        }
    }

    @Deprecated
    @Option(name = "help", hasValue = false, activator = HideLegacyOptionActivator.class)
    private boolean help;

    @Deprecated
    @Option(name = "file", converter = FileConverter.class,
            completer = FileCompleter.class, activator = HideLegacyOptionActivator.class)
    private File file;

    @Deprecated
    @Option(name = "l", hasValue = false, activator = HideLegacyOptionActivator.class)
    private boolean list;

    @Deprecated
    @Arguments(activator = HideLegacyOptionActivator.class,
            completer = BatchReactivateCommand.BatchNameCompleter.class)
    private List<String> name;

    @Override
    public CommandResult execute(CLICommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("batch"));
            return CommandResult.SUCCESS;
        }
        if (commandInvocation.getCommandContext().getBatchManager().isBatchActive()) {
            throw new CommandException("Can't start a new batch while in batch mode.");
        }
        if (file != null) {
            return BatchLoadFileCommand.handle(commandInvocation, file);
        }
        if (list) {
            return new BatchListCommand().execute(commandInvocation.getCommandContext());
        }
        if (name != null && !name.isEmpty()) {
            return BatchReactivateCommand.handle(commandInvocation, name);
        }

        return BatchNewCommand.handle(commandInvocation);
    }

    /**
     * We have a legacy behavior batch {batch-name} to re-activate a batch. In
     * case we have a clasj between action and batch name, the legacy behavior
     * wins and the action name is considered a batch name.
     *
     * @param ctx
     * @param cmd
     * @return
     */
    static CommandResult handle(CLICommandInvocation invocation, String cmd) throws CommandException {
        if (invocation.getCommandContext().getBatchManager().getHeldbackNames().contains(cmd)) {
            List<String> lst = new ArrayList<>();
            lst.add(cmd);
            return BatchReactivateCommand.handle(invocation, lst);
        }
        return null;
    }
}
