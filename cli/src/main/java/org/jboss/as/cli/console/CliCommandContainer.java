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
package org.jboss.as.cli.console;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jboss.aesh.cl.CommandLine;
import org.jboss.aesh.cl.activation.CommandActivator;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.parser.CommandLineCompletionParser;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.parser.OptionParserException;
import org.jboss.aesh.cl.populator.CommandPopulator;
import org.jboss.aesh.cl.validator.CommandValidatorException;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.InvocationProviders;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.container.CommandContainerResult;
import org.jboss.aesh.console.command.container.DefaultCommandContainer;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.parser.AeshLine;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.cli.command.compat.CompatActivator;
import org.jboss.as.cli.command.legacy.InternalBatchCompliantCommand;
import org.jboss.as.cli.command.legacy.InternalDMRCommand;
import org.wildfly.core.cli.command.DMRCommand;
import org.wildfly.core.cli.command.BatchCompliantCommand;
import org.jboss.as.cli.console.AeshCliConsole.CliResultHandler;
import org.jboss.as.cli.console.CliSpecialCommand.CliSpecialCommandContainer;
import org.jboss.as.cli.impl.CliCommandContextImpl;
import org.jboss.as.cli.impl.HelpSupport;
import org.wildfly.core.cli.command.BatchCompliantCommand.BatchResponseHandler;
import org.wildfly.core.cli.command.CommandRedirection;

/**
 *
 * @author jfdenise
 */
public class CliCommandContainer extends DefaultCommandContainer<Command> {

    private class WrappedParser implements CommandLineParser<Command> {

        private final CommandLineParser<Command> parser;

        private WrappedParser(CommandLineParser<Command> parser) {
            this.parser = parser;
        }

        @Override
        public ProcessedCommand<Command> getProcessedCommand() {
            return parser.getProcessedCommand();
        }

        @Override
        public Command getCommand() {
            return parser.getCommand();
        }

        @Override
        public CommandLineCompletionParser getCompletionParser() {
            return parser.getCompletionParser();
        }

        @Override
        public List<String> getAllNames() {
            return parser.getAllNames();
        }

        @Override
        public CommandLineParser<? extends Command> getChildParser(String name) {
            return parser.getChildParser(name);
        }

        @Override
        public void addChildParser(CommandLineParser<? extends Command> childParser) {
            parser.addChildParser(childParser);
        }

        @Override
        public List<CommandLineParser<? extends Command>> getAllChildParsers() {
            return parser.getAllChildParsers();
        }

        @Override
        public CommandPopulator getCommandPopulator() {
            return parser.getCommandPopulator();
        }

        @Override
        public String printHelp() {
            return HelpSupport.getSubCommandHelp(
                    CliCommandContainer.this.parser.getProcessedCommand().getName(),
                    parser);
        }

        @Override
        public CommandLine<? extends Command> parse(String line) {
            return parser.parse(line);
        }

        @Override
        public CommandLine<? extends Command> parse(String line, boolean ignoreRequirements) {
            return parser.parse(line, ignoreRequirements);
        }

        @Override
        public CommandLine<? extends Command> parse(AeshLine line, boolean ignoreRequirements) {
            return parser.parse(line, ignoreRequirements);
        }

        @Override
        public CommandLine<? extends Command> parse(List<String> lines, boolean ignoreRequirements) {
            return parser.parse(lines, ignoreRequirements);
        }

        @Override
        public void clear() {
            parser.clear();
        }

        @Override
        public boolean isGroupCommand() {
            return parser.isGroupCommand();
        }

        @Override
        public void setChild(boolean b) {
            parser.setChild(b);
        }
    }

    public class CliCommandParser implements CommandLineParser<Command> {

        private final ProcessedCommand<Command> cmd;

        CliCommandParser(CliResultHandler handler) throws OptionParserException {
            ProcessedCommand<Command> p = container.getParser().getProcessedCommand();
            handler.setResultHandler(p.getResultHandler());
            cmd = new ProcessedCommand<>(p.getName(), p.getAliases(),
                    p.getCommand(), p.getDescription(),
                    p.getValidator(),
                    handler,
                    p.getArgument(), p.getOptions(), p.getCommandPopulator(),
                    p.getActivator());
        }

        public CommandLineParser<Command> getWrappedParser() {
            return container.getParser();
        }

        @Override
        public ProcessedCommand<Command> getProcessedCommand() {
            return cmd;
        }

        @Override
        public Command getCommand() {
            return container.getParser().getCommand();
        }

        @Override
        public CommandLineCompletionParser getCompletionParser() {
            return container.getParser().getCompletionParser();
        }

        @Override
        public List<String> getAllNames() {
            return container.getParser().getAllNames();
        }

        @Override
        public CommandLineParser<? extends Command> getChildParser(String name) {
            return container.getParser().getChildParser(name);
        }

        @Override
        public void addChildParser(CommandLineParser<? extends Command> childParser) {
            container.getParser().addChildParser(childParser);
        }

        @Override
        public List<CommandLineParser<? extends Command>> getAllChildParsers() {
            return container.getParser().getAllChildParsers();
        }

        @Override
        public CommandPopulator getCommandPopulator() {
            return container.getParser().getCommandPopulator();
        }

        @Override
        public String printHelp() {
            return doPrintHelp();
        }

        @Override
        public CommandLine<? extends Command> parse(String line) {
            return container.getParser().parse(line);
        }

        @Override
        public CommandLine<? extends Command> parse(String line, boolean ignoreRequirements) {
            return container.getParser().parse(line, ignoreRequirements);
        }

        @Override
        public CommandLine<? extends Command> parse(AeshLine line, boolean ignoreRequirements) {
            return container.getParser().parse(line, ignoreRequirements);
        }

        @Override
        public CommandLine<? extends Command> parse(List<String> lines, boolean ignoreRequirements) {
            return container.getParser().parse(lines, ignoreRequirements);
        }

        @Override
        public void clear() {
            container.getParser().clear();
        }

        @Override
        public boolean isGroupCommand() {
            return container.getParser().isGroupCommand();
        }

        @Override
        public void setChild(boolean b) {
            container.getParser().setChild(b);
        }
    }

    private final CommandContext context;
    private final CliCommandContextImpl commandContext;
    private final CommandContainer<Command> container;
    private final CommandLineParser<Command> parser;
    private final AeshCliConsole console;

    CliCommandContainer(AeshCliConsole console,
            CommandContext context,
            CliCommandContextImpl commandContext,
            CommandContainer<Command> container,
            CliResultHandler handler) throws OptionParserException {
        this.context = context;
        this.commandContext = commandContext;
        this.container = container;
        this.parser = new CliCommandParser(handler);
        this.console = console;
    }

    @Override
    public CommandLineParser<Command> getParser() {
        return parser;
    }

    @Override
    public boolean haveBuildError() {
        return container.haveBuildError();
    }

    @Override
    public String getBuildErrorMessage() {
        return container.getBuildErrorMessage();
    }

    @Override
    public void close() throws Exception {
        container.close();
    }

    @Override
    public String printHelp(String childCommandName) {
        return doPrintHelp();
    }

    private int redirectionDepth;

    @Override
    public CommandContainerResult executeCommand(AeshLine line,
            InvocationProviders invocationProviders,
            AeshContext aeshContext,
            CommandInvocation commandInvocation)
            throws CommandLineParserException, OptionValidatorException,
            CommandValidatorException, CommandException, InterruptedException {
        try {
            // Compatibility with legacy redirection
            if (commandContext.getLegacyCommandContext().isWorkflowMode()) {
                commandContext.getLegacyCommandContext().handle(line.getOriginalInput());
                return new CommandContainerResult(null, CommandResult.SUCCESS);
            }

            // New redirection API.
            CommandRedirection redirection = commandContext.getCommandRedirection();
            if (redirection != null && redirectionDepth == 0) {
                // RedirectionDepth allows redirection to executeCommands directly
                // and not to loop in itself.
                // For example, try redirection executing catch command
                redirectionDepth += 1;
                try {
                    redirection.handle(commandContext, line);
                } finally {
                    redirectionDepth--;
                }
                return new CommandContainerResult(null, CommandResult.SUCCESS);
            }

            // This is required to see sub commands.
            // A null commandLine means that it is not parsed by Aesh (legacy bridge
            // and operation.
            CommandLine commandLine = container.getParser().parse(line, false);
            CommandLineParser<Command> cmdParser = commandLine == null ? getParser()
                    : commandLine.getParser();
            if (context.isBatchMode()) {
                // Legacy bridge and Operation
                Batch batch = context.getBatchManager().getActiveBatch();
                if (commandLine == null) {
                    Command c = cmdParser.getCommand();
                    if (c instanceof InternalBatchCompliantCommand) { // Batch compliance implies DMR
                        BatchResponseHandler req = ((InternalBatchCompliantCommand) c).
                                buildBatchResponseHandler(line.getOriginalInput(),
                                        commandContext, batch.getAttachments());
                        commandContext.addBatchOperation(((InternalDMRCommand) c).
                                buildRequest(line.getOriginalInput(), commandContext),
                                line.getOriginalInput(), req);
                        return new CommandContainerResult(null, CommandResult.SUCCESS);
                    }
                } else {
                    // Must populate in order to inject options in proper command
                    cmdParser.getCommandPopulator().populateObject(commandLine,
                            invocationProviders, aeshContext, true);
                    Command c = cmdParser.getCommand();
                    if (c instanceof BatchCompliantCommand) { // Batch compliance implies DMR
                        BatchResponseHandler req = ((BatchCompliantCommand) c).
                                buildBatchResponseHandler(commandContext, batch.getAttachments());
                        commandContext.addBatchOperation(((DMRCommand) c).
                                buildRequest(commandContext),
                                line.getOriginalInput(), req);
                        return new CommandContainerResult(null, CommandResult.SUCCESS);
                    }
                }

            }

            // Inactive commands are hidden. This is required for legacy to not show up
            // in completion. The problem is that we don't want to execute inactive
            // commands that are inactive for good reasons (not connected...)
            CommandActivator activator = cmdParser.getProcessedCommand().getActivator();
            if (!activator.isActivated(cmdParser.getProcessedCommand())) {
                boolean shouldThrow;
                if (activator instanceof CompatActivator) {
                    CompatActivator compat = (CompatActivator) activator;
                    shouldThrow = !compat.isActuallyActivated(cmdParser.getProcessedCommand());
                } else {
                    shouldThrow = true;
                }
                if (shouldThrow) {
                    throw new CommandException("The command is not available in the "
                            + "current context (e.g. required subsystems or connection to the controller might be unavailable).");
                }
            }

            // The timeout is handled for the special commands by the CommandContext, do not timeout it.
            CommandContainerResult res;
            if (container instanceof CliSpecialCommandContainer) {
                res = container.executeCommand(line,
                        invocationProviders, aeshContext, commandInvocation);
            } else {
                res = commandContext.getCommandExecutor().execute(this, commandContext,
                        commandInvocation, commandLine, invocationProviders,
                        aeshContext, console, commandContext.getCommandTimeout(), TimeUnit.SECONDS);
            }

            return res;
        } catch (CommandLineException ex) {
            throw new CommandException(ex);
        } catch (CommandLineParserException | OptionValidatorException |
                CommandValidatorException | CommandException ex) {
            throw ex;
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            if (cause instanceof CommandException) {
                throw (CommandException) cause;
            }
            throw new CommandException("Execution exception for " + cause.getMessage(), cause);
        } catch (TimeoutException ex) {
            throw new CommandException("Execution timeout.");
        } finally {
            postExecution(context, commandInvocation);
        }
    }

    public CommandContainer getWrappedContainer() {
        return container;
    }

    public CommandLineParser<Command> wrapParser(CommandLineParser<Command> p) {
        return new WrappedParser(p);
    }

    private void postExecution(CommandContext context, CommandInvocation commandInvocation) {
        console.setPrompt(context.getPrompt());
    }

    private String doPrintHelp() {
        if (container instanceof CliSpecialCommand.CliSpecialCommandContainer) {
            return container.printHelp(null);
        }
        return HelpSupport.getCommandHelp(parser);
    }
}
