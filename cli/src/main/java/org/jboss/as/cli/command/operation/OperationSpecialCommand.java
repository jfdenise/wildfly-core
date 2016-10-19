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

import java.io.IOException;
import java.util.List;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.result.NullResultHandler;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.container.CommandContainerResult;
import org.jboss.as.cli.Attachments;
import org.wildfly.core.cli.command.CliCommandContext;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.command.legacy.CommandContextWrapper;
import org.jboss.as.cli.command.legacy.InternalBatchCompliantCommand;
import org.jboss.as.cli.console.CliSpecialCommand.CliSpecialExecutor;
import org.jboss.as.cli.impl.CliCommandContextImpl;
import org.jboss.as.cli.impl.Console;
import org.jboss.as.cli.impl.HelpSupport;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationCandidatesProvider;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.cli.command.BatchCompliantCommand.BatchResponseHandler;

/**
 *
 * @author jdenise@redhat.com
 */
public class OperationSpecialCommand implements CliSpecialExecutor,
        InternalBatchCompliantCommand {

    private final CommandContext ctx;
    private final CliCommandContextImpl commandContext;
    private final DefaultOperationCandidatesProvider operationCandidatesProvider
            = new DefaultOperationCandidatesProvider();
    private final DefaultCallbackHandler parser = new DefaultCallbackHandler(false);
    private final DefaultCallbackHandler operationParser
            = new DefaultCallbackHandler(true);
    private final Console console;
    private final DefaultCallbackHandler line = new DefaultCallbackHandler(false);

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
        // We must use this completer instead of the OperationRequestCompleter.INSTANCE
        // due to $<var> completion that only occurs in it.
        return ctx.getDefaultCommandCompleter().complete(new CommandContextWrapper(ctx, parser),
                buffer, buffer.length(), candidates);
    }

    @Override
    public boolean accept(String line) {
        return line.startsWith(":") || line.startsWith(".") || line.startsWith("/");
    }

    public OperationSpecialCommand(CommandContext ctx, CliCommandContextImpl commandContext, Console console)
            throws CommandLineParserException {
        this.ctx = ctx;
        this.commandContext = commandContext;
        this.console = console;
    }

    @Override
    public ModelNode buildRequest(String command, CliCommandContext context) throws CommandFormatException {
        return buildRequest(command, context, null);
    }

    @Override
    public ModelNode buildRequest(String command, CliCommandContext context, Attachments attachments) throws CommandFormatException {
        operationParser.reset();
        operationParser.parse(ctx.getCurrentNodePath(),
                command, ctx);
        if (attachments == null) {
            return Util.toOperationRequest(ctx, operationParser);
        } else {
            return Util.toOperationRequest(ctx, operationParser, attachments);
        }
    }

    @Override
    public String printHelp(String op) {
        if (op == null) {
            return HelpSupport.printHelp(console, "wildfly_raw_op");
        }
        // Check if the op exists.
        line.reset();
        try {
            line.parse(ctx.getCurrentNodePath(),
                    op, ctx);
        } catch (CommandFormatException ex) {
            return HelpSupport.printHelp(console, "wildfly_raw_op");
        }

        String opName = line.getOperationName();
        if (opName == null) {
            return HelpSupport.printHelp(console, "wildfly_raw_op");
        }

        OperationRequestAddress address = line.getAddress();
        ModelNode request = new ModelNode();
        if (address == null || address.isEmpty()) {
            request.get(Util.ADDRESS).setEmptyList();
        } else {
            if (address.endsOnType()) {
                return HelpSupport.printHelp(console, "wildfly_raw_op");
            }
            final ModelNode addrNode = request.get(Util.ADDRESS);
            for (OperationRequestAddress.Node node : address) {
                addrNode.add(node.getType(), node.getName());
            }
        }
        request.get(org.jboss.as.cli.Util.OPERATION).set(org.jboss.as.cli.Util.READ_OPERATION_DESCRIPTION);
        request.get(org.jboss.as.cli.Util.NAME).set(opName);
        ModelNode result;
        try {
            result = ctx.getModelControllerClient().execute(request);
        } catch (IOException e) {
            return HelpSupport.printHelp(console, "wildfly_raw_op");
        }
        if (!result.hasDefined(org.jboss.as.cli.Util.RESULT)) {
            return HelpSupport.printHelp(console, "wildfly_raw_op");
        }
        String content = HelpSupport.printHelp(console, result.get(org.jboss.as.cli.Util.RESULT));
        if (content == null) {
            return HelpSupport.printHelp(console, "wildfly_raw_op");
        }
        return content;
    }

    @Override
    public BatchResponseHandler buildBatchResponseHandler(String line, CliCommandContext commandContext,
            Attachments attachments) throws CommandException {
        return null;
    }

}
