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
import org.wildfly.core.cli.command.CliCommandContext;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.OperationCommand;
import org.jboss.as.cli.impl.CliCommandContextImpl;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author jfdenise
 */
public class CliLegacyDMRCommandBridge extends CliLegacyCommandBridge implements InternalDMRCommand {

    private final OperationCommand handler;
    private final DefaultCallbackHandler line
            = new DefaultCallbackHandler(true);
    private final CliCommandContextImpl commandContext;
    private final CommandContext ctx;
    public CliLegacyDMRCommandBridge(String name,
            CommandContext ctx, CliCommandContextImpl commandContext, OperationCommand handler) throws CommandLineParserException {
        super(name, commandContext);
        this.handler = handler;
        this.commandContext = commandContext;
        this.ctx = ctx;
    }

    @Override
    public ModelNode buildRequest(String input, CliCommandContext context) throws CommandFormatException {
        line.reset();
        line.parse(ctx.getCurrentNodePath(), input, ctx);
        commandContext.setParsedCommandLine(line);
        return handler.buildRequest(ctx);
    }

}
