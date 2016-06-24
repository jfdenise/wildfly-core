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
import org.jboss.aesh.cl.CommandLine;
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
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.container.CommandContainerResult;
import org.jboss.aesh.console.command.container.DefaultCommandContainer;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.parser.AeshLine;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.wildfly.core.cli.command.DMRCommand;
import org.wildfly.core.cli.command.BatchCompliantCommand;
import org.jboss.as.cli.console.AeshCliConsole.CliResultHandler;
import org.jboss.as.cli.impl.CliCommandContextImpl;

/**
 *
 * @author jfdenise
 */
class CliCommandContainer extends DefaultCommandContainer<Command> {

    private class CliCommandParser implements CommandLineParser<Command> {
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
            return container.getParser().printHelp();
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
        // XXX JFDENISE retrieve file and enrich help with text...
        return super.printHelp(childCommandName);
    }

    @Override
    public CommandContainerResult executeCommand(AeshLine line,
            InvocationProviders invocationProviders,
            AeshContext aeshContext,
            CommandInvocation commandInvocation)
            throws CommandLineParserException, OptionValidatorException,
            CommandValidatorException, CommandException, InterruptedException {
        boolean error = false;
        try {
            if (context.isBatchMode()) {
                Command c = container.getParser().getCommand();
                if (c instanceof BatchCompliantCommand) { // Batch compliance implies DMR
                    commandContext.addBatchOperation(((DMRCommand) c).
                            buildRequest(line.getOriginalInput(), commandContext),
                            line.getOriginalInput());
                    return null;
                }
            }
            CommandContainerResult res = container.executeCommand(line,
                    invocationProviders, aeshContext, commandInvocation);
            return res;
        } catch (CommandFormatException ex) {
            error = true;
            throw new CommandException(ex);
        } catch (CommandLineParserException | OptionValidatorException |
                CommandValidatorException | CommandException ex) {
            error = true;
            throw ex;
        } finally {
            if (!error) {
                postExecution(context, commandInvocation);
            }
        }
    }

    private void postExecution(CommandContext context, CommandInvocation commandInvocation) {
        console.setPrompt(context.getPrompt());
    }
}
