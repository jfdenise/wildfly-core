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
package org.jboss.as.cli.command.legacy;

import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.as.cli.Attachments;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.OperationCommand;
import org.jboss.as.cli.OperationCommand.HandledRequest;
import org.jboss.as.cli.handlers.ResponseHandler;
import org.jboss.as.cli.impl.Console;
import org.jboss.as.controller.client.OperationResponse;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.BatchCompliantCommand.BatchResponseHandler;
import org.wildfly.core.cli.command.CliCommandContext;

/**
 *
 * @author jfdenise
 */
public class CliLegacyBatchCompliantCommandBridge extends
        CliLegacyDMRCommandBridge implements InternalBatchCompliantCommand {

    public CliLegacyBatchCompliantCommandBridge(String name,
            CommandContext ctx, CliCommandContext commandContext,
            OperationCommand handler, Console console) throws CommandLineParserException {
        super(name, ctx, commandContext, handler, console);
    }

    @Override
    public BatchResponseHandler buildBatchResponseHandler(String input, CliCommandContext ctx,
            Attachments attachments) throws CommandException {
        try {
            parse(input);
            HandledRequest req = getHandler().buildHandledRequest(ctx.getLegacyCommandContext(), attachments);
            return (ModelNode step, OperationResponse response) -> {
                ResponseHandler h = req.getResponseHandler();
                if (h != null) {
                    try {
                        h.handleResponse(step, response);
                    } catch (CommandLineException ex) {
                        throw new CommandException(ex);
                    }
                }
            };

        } catch (CommandFormatException ex) {
            throw new CommandException(ex);
        }
    }

}
