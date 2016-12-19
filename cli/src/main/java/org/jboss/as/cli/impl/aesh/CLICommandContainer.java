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
import org.aesh.cl.CommandLine;
import org.aesh.cl.internal.ProcessedCommand;
import org.aesh.cl.parser.CommandLineCompletionParser;
import org.aesh.cl.parser.CommandLineParser;
import org.aesh.cl.parser.CommandLineParserException;
import org.aesh.cl.parser.OptionParserException;
import org.aesh.cl.populator.CommandPopulator;
import org.aesh.cl.validator.CommandValidatorException;
import org.aesh.cl.validator.OptionValidatorException;
import org.aesh.console.AeshContext;
import org.aesh.console.InvocationProviders;
import org.aesh.console.command.Command;
import org.aesh.console.command.CommandException;
import org.aesh.console.command.container.CommandContainer;
import org.aesh.console.command.container.CommandContainerResult;
import org.aesh.console.command.container.DefaultCommandContainer;
import org.aesh.console.command.invocation.CommandInvocation;
import org.aesh.util.ParsedLine;

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
            return HelpSupport.getSubCommandHelp(CLICommandContainer.this.parser.getProcessedCommand().getName(),
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
        public CommandLine<? extends Command> parse(ParsedLine line, boolean ignoreRequirements) {
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
        public CommandLine<? extends Command> parse(ParsedLine line, boolean ignoreRequirements) {
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

    private final CommandContainer<Command> container;
    private final CommandLineParser<Command> parser;

    CLICommandContainer(CommandContainer<Command> container) throws OptionParserException {
        this.container = container;
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
        return HelpSupport.getCommandHelp(parser);
    }
}
