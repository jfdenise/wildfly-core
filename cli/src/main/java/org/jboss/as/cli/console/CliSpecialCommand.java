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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jboss.aesh.cl.CommandLine;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.ProcessedCommandBuilder;
import org.jboss.aesh.cl.parser.CommandLineCompletionParser;
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.populator.CommandPopulator;
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
import org.jboss.as.cli.command.DMRCommand;
import org.jboss.as.cli.command.batch.BatchCompliantCommand;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author jdenise@redhat.com
 */
public class CliSpecialCommand {

    public interface CliSpecialExecutor {

        CommandContainerResult execute(CommandContext commandContext,
                String originalInput) throws CommandLineException;

        boolean accept(String line);

        int complete(CommandContext commandContext, String buffer, int i, List<String> candidates);

    }

    private static class CommandImpl implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws IOException, InterruptedException {
            throw new UnsupportedOperationException("Bridge Command can't be called directly.");
        }
    }

    private static class Batch extends CommandImpl implements BatchCompliantCommand {
    }

    private static class DMR extends CommandImpl implements DMRCommand {

        private final DMRCommand cmd;

        DMR(DMRCommand cmd) {
            this.cmd = cmd;
        }

        @Override
        public ModelNode buildRequest(String input, CommandContext context) throws CommandFormatException {
            return cmd.buildRequest(input, context);
        }
    }

    private static class BatchDMR extends DMR implements BatchCompliantCommand {

        public BatchDMR(DMRCommand cmd) {
            super(cmd);
        }

    }

    private class CliSpecialParser implements CommandLineParser<Command> {

        private final ProcessedCommand cmd;
        private final Command command;

        private CliSpecialParser(String name) throws CommandLineParserException {
            if (executor instanceof BatchCompliantCommand) {
                if (executor instanceof DMRCommand) {
                    command = new BatchDMR((DMRCommand) executor);
                } else {
                    command = new Batch();
                }
            } else if (executor instanceof DMRCommand) {
                command = new DMR((DMRCommand) executor);
            } else {
                command = new CommandImpl();
            }
            cmd = new ProcessedCommandBuilder().command(command).name(name).create();
        }

        @Override
        public ProcessedCommand<Command> getProcessedCommand() {
            return cmd;
        }

        @Override
        public Command getCommand() {
            return command;
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

    private class CliSpecialCommandContainer extends DefaultCommandContainer<Command> {

        private final CliSpecialParser parser;

        private CliSpecialCommandContainer(String name) throws CommandLineParserException {
            parser = new CliSpecialParser(name);
        }

        @Override
        public CommandContainerResult executeCommand(AeshLine line,
                InvocationProviders invocationProviders,
                AeshContext aeshContext,
                CommandInvocation commandInvocation)
                throws CommandLineParserException, OptionValidatorException,
                CommandValidatorException, IOException, InterruptedException {

            try {
                CommandContainerResult res = executor.execute(commandContext, line.getOriginalInput());
                return res;
            } catch (CommandLineException ex) {
                throw new RuntimeException(ex);
            } finally {
                CliCommandContainer.postExecution(commandContext, commandInvocation);
            }
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
    }

    private final CommandContext commandContext;
    private final CliSpecialCommandContainer container;
    private final CliSpecialExecutor executor;

    CliSpecialCommand(String name,
            CliSpecialExecutor executor,
            CommandContext commandContext)
            throws CommandLineParserException {
        this.commandContext = commandContext;
        this.executor = executor;
        container = new CliSpecialCommandContainer(name);
    }

    public CommandContainer commandFor(String line) {
        if (executor.accept(line)) {
            return container;
        }
        return null;
    }

    boolean complete(CompleteOperation completeOperation) {
        if (executor.accept(completeOperation.getBuffer())) {
            List<String> candidates = new ArrayList<>();
            int cursor = executor.complete(commandContext,
                    completeOperation.getBuffer(), 0, candidates);
            completeOperation.setOffset(cursor);
            completeOperation.doAppendSeparator(false);
            completeOperation.addCompletionCandidates(candidates);
            return true;
        }
        return false;
    }

    CommandContainer<Command> getCommand() {
        return container;
    }
}
