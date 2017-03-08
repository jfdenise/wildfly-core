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
package org.jboss.as.cli.impl.aesh;

import java.util.List;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.parser.CommandLineCompletionParser;
import org.aesh.command.impl.parser.CommandLineParser;
import org.aesh.command.populator.CommandPopulator;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.console.AeshContext;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.container.CommandContainerResult;
import org.aesh.command.container.DefaultCommandContainer;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.parser.OptionParserException;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.parser.ParsedLineIterator;
import org.aesh.parser.ParsedLine;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.impl.aesh.commands.deprecated.CompatCommandActivator;

/**
 * Wrapping of container to plug CLI Help support.
 *
 * @author jfdenise
 */
public class CLICommandContainer extends DefaultCommandContainer<Command> {

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
        public CommandLineParser<Command> getChildParser(String name) {
            return parser.getChildParser(name);
        }

        @Override
        public void addChildParser(CommandLineParser<Command> childParser) {
            parser.addChildParser(childParser);
        }

        @Override
        public List<CommandLineParser<Command>> getAllChildParsers() {
            return parser.getAllChildParsers();
        }

        @Override
        public CommandPopulator getCommandPopulator() {
            return parser.getCommandPopulator();
        }

        @Override
        public String printHelp() {
            return HelpSupport.getSubCommandHelp(CLICommandContainer.this.parser.getProcessedCommand().name(),
                    parser);
        }

        @Override
        public void parse(String line) {
            parser.parse(line);
        }

        @Override
        public void parse(String line, boolean ignoreRequirements) {
            parser.parse(line, ignoreRequirements);
        }

        @Override
        public void parse(ParsedLineIterator iterator, boolean ignoreRequirements) {
            parser.parse(iterator, ignoreRequirements);
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

        @Override
        public void populateObject(String line, InvocationProviders invocationProviders,
                AeshContext aeshContext, boolean validate) throws CommandLineParserException, OptionValidatorException {
            parser.populateObject(line, invocationProviders, aeshContext, validate);
        }

        @Override
        public ProcessedOption lastParsedOption() {
            return parser.lastParsedOption();
        }

        @Override
        public CommandLineParser<Command> parsedCommand() {
            return parser.parsedCommand();
        }
    }

    public class CLICommandParser implements CommandLineParser<Command> {

        public CommandLineParser<Command> getWrappedParser() {
            return container.getParser();
        }

        @Override
        public ProcessedCommand<Command> getProcessedCommand() {
            return container.getParser().getProcessedCommand();
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
        public CommandLineParser<Command> getChildParser(String name) {
            return container.getParser().getChildParser(name);
        }

        @Override
        public void addChildParser(CommandLineParser<Command> childParser) {
            container.getParser().addChildParser(childParser);
        }

        @Override
        public List<CommandLineParser<Command>> getAllChildParsers() {
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
        public void parse(String line) {
            container.getParser().parse(line);
        }

        @Override
        public void parse(String line, boolean ignoreRequirements) {
            container.getParser().parse(line, ignoreRequirements);
        }

        @Override
        public void parse(ParsedLineIterator iterator, boolean ignoreRequirements) {
            container.getParser().parse(iterator, ignoreRequirements);
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

        @Override
        public void populateObject(String line, InvocationProviders invocationProviders, AeshContext aeshContext, boolean validate) throws CommandLineParserException, OptionValidatorException {
            container.getParser().populateObject(line, invocationProviders, aeshContext, validate);
        }

        @Override
        public ProcessedOption lastParsedOption() {
            return container.getParser().lastParsedOption();
        }

        @Override
        public CommandLineParser<Command> parsedCommand() {
            return container.getParser().parsedCommand();
        }
    }

    private final CommandContainer<Command> container;
    private final CommandLineParser<Command> parser;
    private final CommandContext ctx;

    CLICommandContainer(CommandContainer<Command> container, CommandContext ctx) throws OptionParserException {
        this.container = container;
        this.ctx = ctx;
        this.parser = new CLICommandParser();
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

    @Override
    public CommandContainerResult executeCommand(ParsedLine line,
            InvocationProviders invocationProviders,
            AeshContext aeshContext,
            CommandInvocation commandInvocation)
            throws CommandLineParserException, OptionValidatorException,
            CommandValidatorException, CommandException, InterruptedException {
        return container.executeCommand(line, invocationProviders, aeshContext, commandInvocation);
    }

    public CommandContainer getWrappedContainer() {
        return container;
    }

    public CommandLineParser<Command> wrapParser(CommandLineParser<Command> p) {
        return new WrappedParser(p);
    }

    private String doPrintHelp() {
        if (parser.getProcessedCommand().getActivator() instanceof CompatCommandActivator) {
            return HelpSupport.printHelp(ctx, parser.getProcessedCommand().name());
        }

        return HelpSupport.getCommandHelp(parser);
    }
}
