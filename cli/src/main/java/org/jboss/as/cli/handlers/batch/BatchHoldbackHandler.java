/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cli.handlers.batch;

import java.util.ArrayList;
import org.aesh.command.CommandException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.handlers.CommandHandlerWithHelp;
import org.jboss.as.cli.impl.ArgumentWithValue;
import org.jboss.as.cli.impl.aesh.commands.batch.BatchHoldbackCommand;
import org.jboss.as.cli.operation.ParsedCommandLine;

/**
 *
 * @author Alexey Loubyansky
 */
@Deprecated
public class BatchHoldbackHandler extends CommandHandlerWithHelp {

    public BatchHoldbackHandler() {
        super("batch-holdback");
        // purely for validation, so the arguments are recognized
        new ArgumentWithValue(this, 0, "--name");
    }

    @Override
    public boolean isAvailable(CommandContext ctx) {
        if (!super.isAvailable(ctx)) {
            return false;
        }
        return ctx.isBatchMode();
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.CommandHandlerWithHelp#doHandle(org.jboss.as.cli.CommandContext)
     */
    @Override
    protected void doHandle(CommandContext ctx) throws CommandFormatException {
        String name = null;
        ParsedCommandLine args = ctx.getParsedCommandLine();
        if (args.hasProperties()) {
            name = args.getOtherProperties().get(0);
        }
        try {
            BatchHoldbackCommand cmd = new BatchHoldbackCommand();
            cmd.name = new ArrayList<>();
            cmd.name.add(name);
            cmd.execute(ctx);
        } catch (CommandException ex) {
            throw new CommandFormatException(ex.getLocalizedMessage());
        }
    }
}
