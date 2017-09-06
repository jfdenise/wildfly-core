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
package org.jboss.as.cli.handlers.recorder;


import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.batch.BatchManager;
import org.jboss.as.cli.handlers.CommandHandlerWithHelp;

/**
 *
 * @author jfdenise
 */
public class CommandsRecorderHandler extends CommandHandlerWithHelp {

    public CommandsRecorderHandler() {
        super("commands-recorder", true);
    }

    @Override
    public boolean isAvailable(CommandContext ctx) {
        return CommandsRecorderControlFlow.get(ctx) == null && !ctx.getBatchManager().isBatchActive();
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.CommandHandlerWithHelp#doHandle(org.jboss.as.cli.CommandContext)
     */
    @Override
    protected void doHandle(CommandContext ctx) throws CommandLineException {
        final BatchManager batchManager = ctx.getBatchManager();
        if (batchManager.isBatchActive()) {
            throw new CommandFormatException("recording is not allowed while in batch mode.");
        }
        ctx.registerRedirection(new CommandsRecorderControlFlow(ctx));
    }

    /**
     * It has to accept everything since we don't know what kind of command will
     * be edited.
     */
    @Override
    public boolean hasArgument(CommandContext ctx, int index) {
        return true;
    }

    @Override
    public boolean hasArgument(CommandContext ctx, String name) {
        return true;
    }
}
