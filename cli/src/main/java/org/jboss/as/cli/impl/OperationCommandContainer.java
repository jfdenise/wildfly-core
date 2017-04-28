/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.cli.impl;

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
import org.aesh.console.AeshContext;
import org.aesh.parser.ParsedLineIterator;
import org.jboss.as.cli.CommandLineException;
import org.wildfly.core.cli.command.aesh.CLICommandInvocation;

/**
 *
 * @author jdenise@redhat.com
 */
public class OperationCommandContainer extends DefaultCommandContainer<Command> {

    public class OperationParser implements CommandLineParser<Command> {

        @Override
        public ProcessedCommand<Command> getProcessedCommand() {
            try {
                return new ProcessedCommandBuilder().command(command).name("/").create();
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
            OperationCommandContainer.this.line = line;
        }

        @Override
        public void parse(ParsedLineIterator iterator, Mode mode) {
            line = iterator.getOriginalLine();
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

    }

    private final Command<CLICommandInvocation> command = new Command<CLICommandInvocation>() {
        @Override
        public CommandResult execute(CLICommandInvocation commandInvocation) throws CommandException, InterruptedException {
            try {
                // we pass down the contextual commandContext, could have been wrapped by the timeout handling
                ctx.handleOperation(line, commandInvocation.getCommandContext());
            } catch (CommandLineException ex) {
                throw new CommandException(ex.getLocalizedMessage());
            } finally {
                line = null;
            }
            return CommandResult.SUCCESS;
        }
    };

    private String line;

    private final CommandLineParser<Command> parser = new OperationParser();
    private final CommandContextImpl ctx;

    OperationCommandContainer(CommandContextImpl ctx) {
        this.ctx = ctx;
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
