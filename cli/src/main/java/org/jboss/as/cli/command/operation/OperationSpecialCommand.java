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
package org.jboss.as.cli.command.operation;

import java.util.List;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.result.NullResultHandler;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.container.CommandContainerResult;
import org.jboss.as.cli.CliCommandContext;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.command.DMRCommand;
import org.jboss.as.cli.command.batch.BatchCompliantCommand;
import org.jboss.as.cli.console.CliSpecialCommand.CliSpecialExecutor;
import org.jboss.as.cli.impl.CliCommandContextImpl;
import org.jboss.as.cli.operation.OperationRequestCompleter;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationCandidatesProvider;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author jdenise@redhat.com
 */
public class OperationSpecialCommand implements CliSpecialExecutor,
        BatchCompliantCommand, DMRCommand {

    private final CommandContext ctx;
    private final CliCommandContextImpl commandContext;
    private final DefaultOperationCandidatesProvider operationCandidatesProvider
            = new DefaultOperationCandidatesProvider();
    private final DefaultCallbackHandler parser = new DefaultCallbackHandler(false);
    private final DefaultCallbackHandler operationParser
            = new DefaultCallbackHandler(true);

    @Override
    public CommandContainerResult execute(CommandContext commandContext,
            String originalInput) throws CommandLineException {
        operationParser.reset();
        operationParser.parse(ctx.getCurrentNodePath(),
                originalInput, ctx);
        this.commandContext.handleOperation(operationParser);

        return new CommandContainerResult(new NullResultHandler(),
                CommandResult.SUCCESS);
    }

    @Override
    public int complete(CommandContext commandContext, String buffer,
            int i, List<String> candidates) {
        try {
            parser.reset();
            parser.parse(ctx.getCurrentNodePath(),
                    buffer, ctx);
        } catch (CommandFormatException ex) {
            throw new RuntimeException(ex);
        }
        return OperationRequestCompleter.INSTANCE.complete(ctx,
                parser, operationCandidatesProvider,
                buffer, 0, candidates);
    }

    @Override
    public boolean accept(String line) {
        return line.startsWith(":") || line.startsWith(".") || line.startsWith("/");
    }

    public OperationSpecialCommand(CommandContext ctx, CliCommandContextImpl commandContext)
            throws CommandLineParserException {
        this.ctx = ctx;
        this.commandContext = commandContext;
    }

    @Override
    public ModelNode buildRequest(String command, CliCommandContext context) throws CommandFormatException {
        operationParser.reset();
        operationParser.parse(ctx.getCurrentNodePath(),
                command, ctx);
        return Util.toOperationRequest(ctx, operationParser);
    }

}
