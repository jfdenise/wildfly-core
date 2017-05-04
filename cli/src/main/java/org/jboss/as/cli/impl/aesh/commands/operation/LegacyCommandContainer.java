/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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
 *
 */
package org.jboss.as.cli.impl.aesh.commands.operation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.container.DefaultCommandContainer;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedCommandBuilder;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.impl.parser.CommandLineCompletionParser;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.populator.CommandPopulator;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.complete.AeshCompleteOperation;
import org.aesh.console.AeshContext;
import org.aesh.parser.ParsedLineIterator;
import org.jboss.as.cli.CommandHandler;
import org.jboss.as.cli.impl.CommandContextImpl;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
public class LegacyCommandContainer extends DefaultCommandContainer<Command> {

    public class LegacyCommand implements Command<CLICommandInvocation>, SpecialCommand {

        public CommandHandler getCommandHandler() {
            return handler;
        }

        @Override
        public CommandResult execute(CLICommandInvocation commandInvocation) throws CommandException, InterruptedException {
            throw new CommandException("Should never be called directly.");
        }

        @Override
        public String getLine() {
            return line;
        }
    }

    public class OperationParser implements CommandLineParser<Command> {

        @Override
        public ProcessedCommand<Command> getProcessedCommand() {
            try {
                return new ProcessedCommandBuilder().command(command).name(name).
                        aliases(aliases).create();
            } catch (CommandLineParserException ex) {
                throw new RuntimeException(ex);
            }
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
            return Collections.emptyList();
        }

        @Override
        public CommandLineParser<Command> getChildParser(String name) {
            return null;
        }

        @Override
        public void addChildParser(CommandLineParser<Command> childParser) {

        }

        @Override
        public List<CommandLineParser<Command>> getAllChildParsers() {
            return Collections.emptyList();
        }

        @Override
        public CommandPopulator<Object, Command> getCommandPopulator() {
            return new CommandPopulator<Object, Command>() {
                @Override
                public void populateObject(ProcessedCommand<Command> processedCommand,
                        InvocationProviders invocationProviders, AeshContext aeshContext, Mode mode) throws CommandLineParserException, OptionValidatorException {
                }

                @Override
                public Object getObject() {
                    return command;
                }
            };
        }

        @Override
        public void populateObject(String line, InvocationProviders invocationProviders,
                AeshContext aeshContext, Mode mode) throws CommandLineParserException, OptionValidatorException {

        }

        @Override
        public String printHelp() {
            return null;
        }

        @Override
        public void parse(String line) {

        }

        @Override
        public ProcessedOption lastParsedOption() {
            return null;
        }

        @Override
        public void parse(String line, Mode mode) {
            LegacyCommandContainer.this.line = line;
        }

        @Override
        public void parse(ParsedLineIterator iterator, Mode mode) {
            line = iterator.originalLine();
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

        @Override
        public CommandLineParser<Command> parsedCommand() {
            return this;
        }

        @Override
        public void complete(AeshCompleteOperation co, InvocationProviders invocationProviders) {
            ctx.completeOperationAndLegacy(co);
        }
    }

    private final Command<CLICommandInvocation> command = new LegacyCommand();

    private String line;

    private final CommandLineParser<Command> parser = new OperationParser();
    private final CommandContextImpl ctx;
    private final String name;
    private final CommandHandler handler;
    private final List<String> aliases;

    public LegacyCommandContainer(CommandContextImpl ctx, String[] names, CommandHandler handler) {
        this.ctx = ctx;
        this.name = names[0];
        this.aliases = names.length > 1 ? Arrays.asList(Arrays.copyOfRange(names, 1, names.length)) : Collections.emptyList();
        this.handler = handler;
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
