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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jboss.aesh.cl.CommandLine;
import org.jboss.aesh.cl.activation.CommandActivator;
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
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.container.CommandContainerResult;
import org.jboss.aesh.console.command.container.DefaultCommandContainer;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.parser.AeshLine;
import org.wildfly.core.cli.command.CliCommandContext;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.command.legacy.InternalBatchCompliantCommand;
import org.jboss.as.cli.command.legacy.InternalDMRCommand;
import org.jboss.as.cli.console.AeshCliConsole.CliResultHandler;
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
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            throw new UnsupportedOperationException("Bridge Command can't be called directly.");
        }
    }

    private static class DMR extends CommandImpl implements InternalDMRCommand {

        private final InternalDMRCommand cmd;

        DMR(InternalDMRCommand cmd) {
            this.cmd = cmd;
        }

        @Override
        public ModelNode buildRequest(String input, CliCommandContext context) throws CommandFormatException {
            return cmd.buildRequest(input, context);
        }
    }

    private static class Batch extends DMR implements InternalBatchCompliantCommand {

        public Batch(InternalDMRCommand cmd) {
            super(cmd);
        }

    }

    private class CliSpecialParser implements CommandLineParser<Command> {

        private final ProcessedCommand cmd;
        private final Command command;

        private CliSpecialParser(String name) throws CommandLineParserException {
            if (executor instanceof InternalBatchCompliantCommand) {
                command = new Batch((InternalDMRCommand) executor);
            } else if (executor instanceof InternalDMRCommand) {
                command = new DMR((InternalDMRCommand) executor);
            } else {
                command = new CommandImpl();
            }
            cmd = new ProcessedCommandBuilder().command(command).name(name).
                    activator(activator).resultHandler(handler).create();
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
                CommandValidatorException, CommandException, InterruptedException {

            try {
                CommandContainerResult res = executor.execute(commandContext, line.getOriginalInput());
                return res;
            } catch (CommandLineException ex) {
                throw new CommandException(ex);
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
    private final CommandContainer container;
    private final CliSpecialExecutor executor;
    private final CliResultHandler handler;
    private final CommandActivator activator;

    CliSpecialCommand(String name,
            CommandActivator activator,
            CliSpecialExecutor executor,
            CommandContext commandContext,
            CliResultHandler handler,
            CliCommandRegistry registry)
            throws CommandLineParserException {
        this.commandContext = commandContext;
        this.executor = executor;
        this.handler = handler;
        this.activator = activator;
        container = registry.wrapContainer(new CliSpecialCommandContainer(name));
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

    CommandContainer<Command> getCommandContainer() {
        return container;
    }
}
