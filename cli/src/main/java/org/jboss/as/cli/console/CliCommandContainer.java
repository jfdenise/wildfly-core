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

import java.io.IOException;
import java.util.List;
import org.jboss.aesh.cl.CommandLine;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.parser.CommandLineCompletionParser;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.parser.OptionParserException;
import org.jboss.aesh.cl.populator.CommandPopulator;
import org.jboss.aesh.cl.result.ResultHandler;
import org.jboss.aesh.cl.validator.CommandValidatorException;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.InvocationProviders;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.container.CommandContainerResult;
import org.jboss.aesh.console.command.container.DefaultCommandContainer;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.parser.AeshLine;
import org.jboss.as.cli.CliCommandContext;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.command.DMRCommand;
import org.jboss.as.cli.command.batch.BatchCompliantCommand;

/**
 *
 * @author jfdenise
 */
public class CliCommandContainer extends DefaultCommandContainer<Command> {

    private class ExceptionResultHandler implements ResultHandler {
        private final ResultHandler rh;

        ExceptionResultHandler(ResultHandler rh) {
            this.rh = rh;
        }

        @Override
        public void onSuccess() {
            if (rh != null) {
                rh.onSuccess();
            }
        }

        @Override
        public void onFailure(CommandResult result) {
            if (rh != null) {
                rh.onFailure(result);
            }
            throw new RuntimeException("Command "
                    + container.getParser().getProcessedCommand().getName()
                    + " failed.");
        }

        @Override
        public void onValidationFailure(CommandResult result, Exception exception) {
            if (rh != null) {
                rh.onValidationFailure(result, exception);
            }
            throw new RuntimeException(exception);
        }

    }

    private class CliCommandParser implements CommandLineParser<Command> {
        private final ProcessedCommand<Command> cmd;

        CliCommandParser() throws OptionParserException {
            ProcessedCommand<Command> p = container.getParser().getProcessedCommand();
            cmd = new ProcessedCommand<>(p.getName(), p.getCommand(), p.getDescription(),
                    p.getValidator(),
                    new ExceptionResultHandler(p.getResultHandler()),
                    p.getArgument(), p.getOptions(), p.getCommandPopulator());
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
    private final CliCommandContext commandContext;
    private final CommandContainer<Command> container;
    private final CommandLineParser<Command> parser;
    public CliCommandContainer(CommandContext context,
            CliCommandContext commandContext, CommandContainer<Command> container,
            boolean interactive) throws OptionParserException {
        this.context = context;
        this.commandContext = commandContext;
        this.container = container;
        this.parser = interactive ? container.getParser() : new CliCommandParser();
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
            CommandValidatorException, IOException, InterruptedException {
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
            throw new RuntimeException(ex);
        } finally {
            postExecution(context, commandInvocation);
        }
    }

    static void postExecution(CommandContext context, CommandInvocation commandInvocation) {
        commandInvocation.setPrompt(new Prompt(context.getPrompt()));
    }
}
