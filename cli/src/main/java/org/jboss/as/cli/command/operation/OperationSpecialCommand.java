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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import org.jboss.aesh.cl.CommandLine;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.ProcessedCommandBuilder;
import org.jboss.aesh.cl.parser.CommandLineCompletionParser;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.populator.CommandPopulator;
import org.jboss.aesh.cl.result.NullResultHandler;
import org.jboss.aesh.cl.validator.CommandValidatorException;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.InvocationProviders;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.container.CommandContainerResult;
import org.jboss.aesh.console.command.container.DefaultCommandContainer;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.parser.AeshLine;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.console.CliCommandRegistry.CliSpecialCommand;
import org.jboss.as.cli.console.Console;
import org.jboss.as.cli.operation.OperationRequestCompleter;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationCandidatesProvider;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author jdenise@redhat.com
 */
public class OperationSpecialCommand implements CliSpecialCommand {

    private class CliOperationParser implements CommandLineParser<Command> {
        private final ProcessedCommand cmd;

        CliOperationParser() throws CommandLineParserException {
            cmd = new ProcessedCommandBuilder().name(":").create();
        }

        @Override
        public ProcessedCommand<Command> getProcessedCommand() {
            return cmd;
        }

        @Override
        public Command getCommand() {
            return null;
        }

        @Override
        public CommandLineCompletionParser getCompletionParser() {
            return null;
        }

        @Override
        public List<String> getAllNames() {
            return null;
        }

        @Override
        public CommandLineParser<? extends Command> getChildParser(String name) {
            return null;
        }

        @Override
        public void addChildParser(CommandLineParser<? extends Command> childParser) {
        }

        @Override
        public List<CommandLineParser<? extends Command>> getAllChildParsers() {
            return Collections.emptyList();
        }

        @Override
        public CommandPopulator getCommandPopulator() {
            return null;
        }

        @Override
        public String printHelp() {
            return null;
        }

        @Override
        public CommandLine<? extends Command> parse(String line) {
            return null;
        }

        @Override
        public CommandLine<? extends Command> parse(String line,
                boolean ignoreRequirements) {
            return null;
        }

        @Override
        public CommandLine<? extends Command> parse(AeshLine line,
                boolean ignoreRequirements) {
            return null;
        }

        @Override
        public CommandLine<? extends Command> parse(List<String> lines,
                boolean ignoreRequirements) {
            return null;
        }

        @Override
        public void clear() {

        }

        @Override
        public boolean isGroupCommand() {
            return false;
        }

        @Override
        public void setChild(boolean b) {

        }

    }

    private class OperationCommandContainer extends DefaultCommandContainer<Command> {

        private final CliOperationParser parser;
        private final DefaultCallbackHandler operationParser
                = new DefaultCallbackHandler(true);

        public OperationCommandContainer() throws CommandLineParserException {
            parser = new CliOperationParser();
        }

        @Override
        public CommandContainerResult executeCommand(AeshLine line,
                InvocationProviders invocationProviders,
                AeshContext aeshContext,
                CommandInvocation commandInvocation)
                throws CommandLineParserException, OptionValidatorException,
                CommandValidatorException, IOException, InterruptedException {

            try {
                operationParser.reset();
                operationParser.parse(ctx.getCurrentNodePath(),
                        line.getOriginalInput(), ctx);
                final ModelNode request = Util.toOperationRequest(ctx,
                        operationParser);
                handle(ctx, request);
            } catch (CommandLineException ex) {
                throw new RuntimeException(ex);
            }

            return new CommandContainerResult(new NullResultHandler(),
                    CommandResult.SUCCESS);
        }

        @Override
        public CommandLineParser<Command> getParser() {
            return parser;
        }

        @Override
        public boolean haveBuildError() {
            return false;
        }

        @Override
        public String getBuildErrorMessage() {
            return null;
        }

        @Override
        public void close() throws Exception {
        }

        public void handle(CommandContext ctx, ModelNode request)
                throws CommandLineException {

            ModelControllerClient client = ctx.getModelControllerClient();
            if (client == null) {
                throw new CommandFormatException("You are disconnected at the moment."
                        + " Type 'connect' to connect to the server"
                        + " or 'help' for the list of supported commands.");
            }

            if (ctx.getConfig().isValidateOperationRequests()) {
                ModelNode opDescOutcome = Util.validateRequest(ctx, request);
                if (opDescOutcome != null) { // operation has params that might need to be replaced
                    Util.replaceFilePathsWithBytes(request, opDescOutcome);
                }
            }

            try {
                final ModelNode result = client.execute(request);
                if (Util.isSuccess(result)) {
                    console.print(result.toString());
                } else {
                    throw new CommandLineException(result.toString());
                }
            } catch (NoSuchElementException e) {
                throw new CommandLineException("ModelNode request is incomplete", e);
            } catch (CancellationException e) {
                throw new CommandLineException("The result couldn't be retrieved"
                        + " (perhaps the task was cancelled", e);
            } catch (IOException e) {
                ctx.disconnectController();
                throw new CommandLineException("Communication error", e);
            } catch (RuntimeException e) {
                throw new CommandLineException("Failed to execute operation.", e);
            }
        }
    }
    private final OperationCommandContainer op = new OperationCommandContainer();
    private final CommandContext ctx;
    private final DefaultOperationCandidatesProvider operationCandidatesProvider
            = new DefaultOperationCandidatesProvider();
    private final DefaultCallbackHandler parser = new DefaultCallbackHandler(false);
    private final Console console;

    public OperationSpecialCommand(CommandContext ctx, Console console)
            throws CommandLineParserException {
        this.ctx = ctx;
        this.console = console;
    }

    @Override
    public CommandContainer commandFor(String line) {
        if (accept(line)) {
            return op;
        }
        return null;
    }

    @Override
    public boolean complete(CompleteOperation completeOperation) {
        if (accept(completeOperation.getBuffer())) {
            List<String> candidates = new ArrayList<>();
            try {
                parser.reset();
                parser.parse(ctx.getCurrentNodePath(),
                        completeOperation.getBuffer(), ctx);
            } catch (CommandFormatException ex) {
                return true;
            }
            int cursor = OperationRequestCompleter.INSTANCE.complete(ctx,
                    parser, operationCandidatesProvider,
                    completeOperation.getBuffer(), 0, candidates);
            completeOperation.setOffset(cursor);
            completeOperation.doAppendSeparator(false);
            completeOperation.addCompletionCandidates(candidates);
            return true;
        }
        return false;
    }

    private static boolean accept(String line) {
        return line.startsWith(":") || line.startsWith(".") || line.startsWith("/");
    }

    @Override
    public CommandContainer<Command> getCommand() {
        return op;
    }
}
