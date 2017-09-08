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
package org.jboss.as.cli.impl.aesh.commands;

import java.io.File;
import java.io.IOException;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.option.Option;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;
import org.wildfly.core.cli.command.aesh.FileCompleter;
import org.wildfly.core.cli.command.aesh.activator.AbstractRejectOptionActivator;

/**
 *
 * @author jdenise@redhat.com
 */
@CommandDefinition(name = "stop-record", description = "", activator = StopRecordActivator.class)
public class StopRecordCommand implements Command<CLICommandInvocation> {

    public static class DiscardOptionActivator extends AbstractRejectOptionActivator {

        public DiscardOptionActivator() {
            super("store", "export", "transient");
        }
    }

    public static class NoTrashOptionActivator extends AbstractRejectOptionActivator {

        public NoTrashOptionActivator() {
            super("discard");
        }
    }

    @Option(required = false, activator = NoTrashOptionActivator.class)
    private String store;

    @Option(required = false, name = "transient", activator = NoTrashOptionActivator.class, hasValue = false)
    private boolean trans;

    @Option(required = false, completer = FileCompleter.class, activator = NoTrashOptionActivator.class)
    private String export;

    @Option(required = false, hasValue = false, activator = DiscardOptionActivator.class)
    private boolean discard;

    @Override
    public CommandResult execute(CLICommandInvocation commandInvocation) throws CommandException, InterruptedException {
        CommandContext ctx = commandInvocation.getCommandContext();
        final CommandsRecorderControlFlow crCF = CommandsRecorderControlFlow.get(ctx);
        if (crCF == null) {
            throw new CommandException("stop-record is not available outside of a recording session.");
        }
        if (discard && (export != null || store != null)) {
            throw new CommandException("--discard can't be used with --store or --export option.");
        }
        if (trans && store == null) {
            throw new CommandException("--transient option can only be used when storing a command.");
        }
        try {
            if (discard) {
                crCF.trash(commandInvocation);
            } else {
                crCF.run(commandInvocation, store, trans, export == null ? null : new File(export));
            }
        } catch (CommandLineException | IOException ex) {
            throw new CommandException(ex);
        }
        return CommandResult.SUCCESS;
    }

}
