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
import org.jboss.aesh.cl.parser.CommandLineParser;
import org.jboss.aesh.cl.parser.CommandLineParserException;
import org.jboss.aesh.cl.validator.CommandValidatorException;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.InvocationProviders;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.container.CommandContainer;
import org.jboss.aesh.console.command.container.CommandContainerResult;
import org.jboss.aesh.console.command.container.DefaultCommandContainer;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.parser.AeshLine;
import org.jboss.as.cli.CommandContext;

/**
 *
 * @author jfdenise
 */
public class CliCommandContainer extends DefaultCommandContainer<Command> {

    private final CommandContext context;
    private final CommandContainer<Command> container;

    public CliCommandContainer(CommandContext context, CommandContainer<Command> container) {
        this.context = context;
        this.container = container;
    }

    @Override
    public CommandLineParser<Command> getParser() {
        return container.getParser();
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
    public CommandContainerResult executeCommand(AeshLine line, InvocationProviders invocationProviders,
            AeshContext aeshContext,
            CommandInvocation commandInvocation)
            throws CommandLineParserException, OptionValidatorException, CommandValidatorException, IOException, InterruptedException {
        CommandContainerResult res = container.executeCommand(line, invocationProviders, aeshContext, commandInvocation);
        postExecution(context, commandInvocation);
        return res;
    }

    static void postExecution(CommandContext context, CommandInvocation commandInvocation) {
        commandInvocation.setPrompt(new Prompt(context.getPrompt()));
    }
}
