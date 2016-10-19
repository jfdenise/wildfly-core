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

import java.util.List;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.GroupCommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.aesh.activator.BatchActivator;
import org.jboss.as.cli.aesh.activator.HiddenActivator;
import org.jboss.as.cli.batch.BatchManager;
import org.wildfly.core.cli.command.CliCommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
@GroupCommandDefinition(name = "holdback", description = "", activator = BatchActivator.class)
public class BatchHoldbackCommand implements Command<CliCommandInvocation> {

    @Deprecated
    @Option(name = "help", hasValue = false, activator = HiddenActivator.class)
    protected boolean help;

    // XXX JFDENISE AESH-401
    @Arguments() // required = true
    private List<String> name;

    @Override
    public CommandResult execute(CliCommandInvocation commandInvocation)
            throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("batch holdback"));
            return CommandResult.SUCCESS;
        }
        CommandContext ctx = commandInvocation.getCommandContext().getLegacyCommandContext();
        BatchManager batchManager = ctx.getBatchManager();
        if (!batchManager.isBatchActive()) {
            throw new CommandException("No active batch to holdback.");
        }

        if (name == null || name.isEmpty()) {
            throw new CommandException("No batch name to holdback");
        }
        String batchName = name.get(0);
        if (batchManager.isHeldback(batchName)) {
            throw new CommandException("There already is "
                    + (batchName == null ? "unnamed" : "'" + batchName
                            + "'") + " batch held back.");
        }

        if (!batchManager.holdbackActiveBatch(batchName)) {
            throw new CommandException("Failed to holdback the batch.");
        }
        return CommandResult.SUCCESS;
    }

}
